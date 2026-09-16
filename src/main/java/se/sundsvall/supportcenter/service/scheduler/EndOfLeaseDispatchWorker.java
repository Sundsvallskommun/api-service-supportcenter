package se.sundsvall.supportcenter.service.scheduler;

import feign.RetryableException;
import generated.client.sysman.TargetReference;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.exception.ClientProblem;
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
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
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

	private static final String CONNECT_TIMED_OUT = "connect timed out";

	private static final String NO_MESSAGE_ID = "scheduler.end-of-lease.dispatch.message-id is not configured, so the job has no message to ask SysMan to send";

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final SysManIntegration sysManIntegration;
	private final EndOfLeaseFailureRecorder endOfLeaseFailureRecorder;
	private final Dept44HealthUtility dept44HealthUtility;
	private final int pageSize;
	private final long messageId;
	private final String jobName;

	public EndOfLeaseDispatchWorker(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		final SysManIntegration sysManIntegration,
		final EndOfLeaseFailureRecorder endOfLeaseFailureRecorder,
		final Dept44HealthUtility dept44HealthUtility,
		@Value("${scheduler.end-of-lease.dispatch.page-size}") final int pageSize,
		@Value("${scheduler.end-of-lease.dispatch.message-id:0}") final long messageId,
		@Value("${scheduler.end-of-lease.dispatch.name}") final String jobName) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.sysManIntegration = sysManIntegration;
		this.endOfLeaseFailureRecorder = endOfLeaseFailureRecorder;
		this.dept44HealthUtility = dept44HealthUtility;
		this.pageSize = pageSize;
		this.messageId = messageId;
		this.jobName = jobName;
	}

	public void processComputersReadyToSend() {
		// Which message this is has to be named per environment, and SENT is terminal, so a run that sent the wrong one
		// could not be taken back. Caught here rather than validated at startup, where it would stop an API that has never
		// needed a message id from coming up at all. The lookup run refuses without its POB key the same way.
		if (messageId <= 0) {
			LOG.error(NO_MESSAGE_ID);
			dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, NO_MESSAGE_ID);
			return;
		}

		final var computers = endOfLeaseComputerRepository
			.findReadyToSend(PENDING, now(ZoneId.systemDefault()), PageRequest.ofSize(pageSize));

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
		} catch (final ServerProblem | CallNotPermittedException e) {
			// The installation is unwell or refusing traffic. Nobody in the group was reached, but that is a fact about
			// the installation and not about any of them, so it is not counted against anyone. Listed by name rather
			// than caught as everything else, so that a defect of our own falls through to the branch that does count
			// and cannot keep its group in every page for good.
			endOfLeaseFailureRecorder.recordDependencyFailure(computers, jobName, unreachable(municipalityId, e));
			return;
		} catch (final RetryableException e) {
			// Feign wraps both a connection that was never made and an answer that never came in the same exception,
			// and the difference decides everything here. Never connected means nothing was delivered and nobody should
			// pay for it. An answer that never came is not evidence of anything: the installation may well have taken
			// the call and sent the messages before it went quiet. Counted, because uncounted it is sent again every
			// hour for good, and the batch is deduplicated precisely so that nobody is told twice.
			if (neverReached(e)) {
				endOfLeaseFailureRecorder.recordDependencyFailure(computers, jobName, unreachable(municipalityId, e));
			} else {
				LOG.warn("The SysMan installation of municipality {} did not answer for {} computer(s), which is no proof the message was not sent: {}", municipalityId, computers.size(), e.getMessage());
				chargeEveryone(computers, e);
			}
			return;
		} catch (final ClientProblem e) {
			// A rejected account is the one 4xx that says nothing about the computers. A rotated password or a locked
			// account answers every call the same way, and counted per computer it empties the budget of a whole
			// municipality in five runs over something none of them is the cause of. Every other 4xx is either the call
			// we built or a row in it, and falls through to the count below.
			if (rejectedOurAccount(e)) {
				endOfLeaseFailureRecorder.recordDependencyFailure(computers, jobName, rejectedIdentity(municipalityId, e));
			} else {
				LOG.warn("The SysMan installation of municipality {} turned down the call for all {} computer(s) in it: {}", municipalityId, computers.size(), e.getMessage());
				chargeEveryone(computers, e);
			}
			return;
		} catch (final Exception e) {
			// The installation turned our call down, or we built a call it could not read. One computer name it will
			// not accept is enough to lose the whole group, so this has to count, or that group never gets past the row
			// that broke it. A municipality with no installation configured lands here too, which is right: nobody but
			// a human can put that one straight.
			LOG.warn("The SysMan installation of municipality {} turned down the call for all {} computer(s) in it: {}", municipalityId, computers.size(), e.getMessage());
			chargeEveryone(computers, e);
			return;
		}

		// Outside the try on purpose. The message is delivered by the time we get here, so a failure while writing that
		// down is not a failed send and must not be recorded as one. Left to travel up instead, where the scheduler
		// aspect reports it, rather than be mistaken for an installation that never answered.
		reconcile(computers, targetReferences);
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
				computer.setRetryAfter(null);
				endOfLeaseComputerRepository.save(computer);
			} else {
				endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), NOT_RECOGNIZED);
			}
		});
	}

	private void chargeEveryone(final List<EndOfLeaseComputerEntity> computers, final Exception e) {
		computers.forEach(computer -> endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), e.getMessage()));
	}

	private static String subject(final EndOfLeaseComputerEntity computer) {
		return "computer name " + computer.getAssetTag();
	}

	private static String unreachable(final String municipalityId, final Exception e) {
		return "The SysMan installation of municipality %s could not be reached: %s".formatted(municipalityId, e.getMessage());
	}

	private static String rejectedIdentity(final String municipalityId, final ClientProblem e) {
		return "The SysMan installation of municipality %s turned our account down, which is nothing any of these computers is the cause of: %s".formatted(municipalityId, e.getMessage());
	}

	/**
	 * Whether the installation refused the account rather than the call. Both answers are 4xx and only the status tells
	 * them apart.
	 */
	private static boolean rejectedOurAccount(final ClientProblem e) {
		return e.getStatus() == UNAUTHORIZED || e.getStatus() == FORBIDDEN;
	}

	/**
	 * Whether the call never got as far as the installation. A refused or unresolvable host says so, and so does a
	 * handshake that failed and a connection that was never established inside its timeout. A call that was made and
	 * then timed out waiting for the answer says nothing at all about what the other end did with it.
	 *
	 * Measured against okhttp 4.12.0: a blackholed address gives SocketTimeoutException("Connect timed out") once the
	 * connect timeout is up, while a host that answers and then goes quiet gives SocketTimeoutException("Read timed
	 * out"). The type is the same for both and the message is the only thing that separates them. Both strings come
	 * from the JDK's own socket implementation rather than from okhttp, so they do not move with the http client. A
	 * message we do not recognize counts the attempt, which is what this method did for every timeout before.
	 */
	private static boolean neverReached(final RetryableException e) {
		final var cause = e.getCause();

		return cause instanceof ConnectException
			|| cause instanceof UnknownHostException
			|| cause instanceof NoRouteToHostException
			|| cause instanceof SSLException
			|| connectTimedOut(cause);
	}

	private static boolean connectTimedOut(final Throwable cause) {
		return cause instanceof SocketTimeoutException
			&& String.valueOf(cause.getMessage()).toLowerCase(Locale.ROOT).startsWith(CONNECT_TIMED_OUT);
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

}
