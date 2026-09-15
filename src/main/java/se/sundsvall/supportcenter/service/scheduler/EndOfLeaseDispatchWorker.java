package se.sundsvall.supportcenter.service.scheduler;

import feign.RetryableException;
import generated.client.sysman.TargetReference;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.sysman.SysManIntegration;

import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toSaveMessagesToTargetsCommand;

/**
 * The work the dispatch run does. Separate from the scheduler so that the scheduler stays the part that says when, and
 * this stays the part that says what.
 *
 * A page is grouped by municipality and each group is one call, since the municipality decides the installation and an
 * installation only knows its own computers. What comes back is the targets the installation recognized, which is not
 * always everything that was sent, so a group is reconciled name by name rather than marked sent as a whole.
 */
@Component
public class EndOfLeaseDispatchWorker {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseDispatchWorker.class);

	private static final String NOT_RECOGNIZED = "SysMan did not recognize the computer name and answered without it";

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final SysManIntegration sysManIntegration;
	private final Dept44HealthUtility dept44HealthUtility;
	private final int pageSize;
	private final int maximumAttempts;
	private final long messageId;
	private final String jobName;

	public EndOfLeaseDispatchWorker(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		final SysManIntegration sysManIntegration,
		final Dept44HealthUtility dept44HealthUtility,
		@Value("${scheduler.end-of-lease.dispatch.page-size}") final int pageSize,
		@Value("${scheduler.end-of-lease.maximum-attempts}") final int maximumAttempts,
		@Value("${scheduler.end-of-lease.dispatch.message-id}") final long messageId,
		@Value("${scheduler.end-of-lease.dispatch.name}") final String jobName) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.sysManIntegration = sysManIntegration;
		this.dept44HealthUtility = dept44HealthUtility;
		this.pageSize = pageSize;
		this.maximumAttempts = maximumAttempts;
		this.messageId = messageId;
		this.jobName = jobName;
	}

	public void processComputersReadyToSend() {
		final var computers = endOfLeaseComputerRepository
			.findByStatusAndAssetMunicipalityIdIsNotNullOrderByCreated(PENDING, PageRequest.ofSize(pageSize));

		if (!computers.isEmpty()) {
			LOG.info("Sending the message via SysMan to {} computer(s).", computers.size());

			computers.stream()
				.collect(groupingBy(EndOfLeaseComputerEntity::getAssetMunicipalityId))
				.forEach(this::send);
		} else {
			LOG.info("No computer is waiting for its message, nothing is sent via SysMan.");
		}
	}

	private void send(final String municipalityId, final List<EndOfLeaseComputerEntity> computers) {
		final List<TargetReference> targetReferences;

		try {
			targetReferences = sysManIntegration.sendMessagesToTargets(
				municipalityId, toSaveMessagesToTargetsCommand(computers, messageId));
		} catch (final ServerProblem | RetryableException | CallNotPermittedException e) {
			// The installation is unwell or out of reach. Nobody in the group was reached, but that is a fact about the
			// installation and not about any of them, so it is not counted against anyone. Listed by name rather than
			// caught as everything else, so that a defect of our own falls through to the branch that does count and
			// cannot keep its group in every page for good.
			recordDependencyFailure(municipalityId, computers, e.getMessage());
			return;
		} catch (final Exception e) {
			// The installation turned our call down, or we built a call it could not read. One computer name it will
			// not accept is enough to lose the whole group, so this has to count, or that group never gets past the row
			// that broke it. A municipality with no installation configured lands here too, which is right: nobody but
			// a human can put that one straight.
			LOG.warn("The SysMan installation of municipality {} turned down the call for all {} computer(s) in it: {}", municipalityId, computers.size(), e.getMessage());
			computers.forEach(computer -> recordFailedAttempt(computer, e.getMessage()));
			return;
		}

		// Outside the try on purpose. The message is delivered by the time we get here, so a failure while writing that
		// down is not a failed send and must not be recorded as one. Left to travel up instead, where the scheduler
		// aspect reports it, rather than be mistaken for an installation that never answered.
		reconcile(computers, targetReferences);
	}

	/**
	 * A failure that belongs to the installation rather than to the computers. The reason is written on every row so
	 * that a queue standing still can be read off it, but the attempts and the statuses are left as they were.
	 */
	private void recordDependencyFailure(final String municipalityId, final List<EndOfLeaseComputerEntity> computers, final String errorMessage) {
		LOG.warn("The SysMan installation of municipality {} could not be reached for {} computer(s), leaving their attempts untouched: {}", municipalityId, computers.size(), errorMessage);

		// The run carries on to the next municipality and swallows this, so the scheduler aspect never sees it. Said
		// here instead, or an outage is invisible until the queue has aged enough for the queue indicator to notice.
		dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, "The SysMan installation of municipality %s could not be reached: %s".formatted(municipalityId, errorMessage));

		computers.forEach(computer -> {
			computer.setErrorMessage(errorMessage);
			endOfLeaseComputerRepository.save(computer);
		});
	}

	/**
	 * A target the installation does not know about is left out of the answer rather than reported as an error, so what
	 * came back is the only thing that says who was reached.
	 */
	private void reconcile(final List<EndOfLeaseComputerEntity> computers, final List<TargetReference> targetReferences) {
		final var reachedNames = ofTargetNames(targetReferences);

		computers.forEach(computer -> {
			if (reachedNames.contains(upperCase(computer.getAssetTag()))) {
				computer.setStatus(SENT);
				computer.setSentAt(now(ZoneId.systemDefault()));
				computer.setErrorMessage(null);
				endOfLeaseComputerRepository.save(computer);
			} else {
				recordFailedAttempt(computer, NOT_RECOGNIZED);
			}
		});
	}

	private static Set<String> ofTargetNames(final List<TargetReference> targetReferences) {
		return ofNullable(targetReferences).orElse(emptyList()).stream()
			.map(TargetReference::getName)
			.filter(Objects::nonNull)
			.map(EndOfLeaseDispatchWorker::upperCase)
			.collect(toSet());
	}

	private static String upperCase(final String value) {
		return value.toUpperCase(Locale.ROOT);
	}

	private void recordFailedAttempt(final EndOfLeaseComputerEntity computer, final String errorMessage) {
		final var attempts = computer.getAttempts() + 1;

		computer.setAttempts(attempts);
		computer.setErrorMessage(errorMessage);

		if (attempts >= maximumAttempts) {
			computer.setStatus(FAILED);
			LOG.warn("Giving up the send of computer name {} after {} attempts: {}", computer.getAssetTag(), attempts, errorMessage);

			// Giving up on a computer is the definition of FAILED: nobody but a person can take it further. The run
			// itself swallows this, so without saying so here the only place it shows is the log.
			dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, "Gave up the send of computer name %s after %d attempts".formatted(computer.getAssetTag(), attempts));
		}

		endOfLeaseComputerRepository.save(computer);
	}
}
