package se.sundsvall.supportcenter.service.scheduler;

import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.integration.pob.configuration.POBProperties;

import static java.util.Objects.isNull;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toAssetMunicipalityId;

/**
 * The work the lookup run does. Separate from the scheduler so that the scheduler stays the part that says when, and
 * this stays the part that says what.
 *
 * One computer is one POB call and one saved row. Kept that way rather than saving the page at the end, so that a run
 * cut short by its lock leaves behind every computer it did get through instead of losing the lot.
 */
@Component
public class EndOfLeaseLookupWorker {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseLookupWorker.class);

	private static final String UNKNOWN_TO_POB = "POB answered the serial number with no configuration item holding a municipality we can route on";

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final POBIntegration pobIntegration;
	private final POBProperties pobProperties;
	private final Dept44HealthUtility dept44HealthUtility;
	private final int pageSize;
	private final int maximumAttempts;
	private final String jobName;

	public EndOfLeaseLookupWorker(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		final POBIntegration pobIntegration,
		final POBProperties pobProperties,
		final Dept44HealthUtility dept44HealthUtility,
		@Value("${scheduler.end-of-lease.lookup.page-size}") final int pageSize,
		@Value("${scheduler.end-of-lease.maximum-attempts}") final int maximumAttempts,
		@Value("${scheduler.end-of-lease.lookup.name}") final String jobName) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.pobIntegration = pobIntegration;
		this.pobProperties = pobProperties;
		this.dept44HealthUtility = dept44HealthUtility;
		this.pageSize = pageSize;
		this.maximumAttempts = maximumAttempts;
		this.jobName = jobName;
	}

	public void processComputersAwaitingLookup() {
		final var computers = endOfLeaseComputerRepository
			.findByStatusAndAssetMunicipalityIdIsNullOrderByCreated(PENDING, PageRequest.ofSize(pageSize));

		final var numberOfComputers = computers.size();

		if (numberOfComputers == 0) {
			LOG.info("Found no computers to look up the municipality for.");
		} else {
			LOG.info("Found {} computers to look up the municipality for", numberOfComputers);
			computers.forEach(this::lookUp);
		}
	}

	private void lookUp(final EndOfLeaseComputerEntity computer) {
		try {
			final var assetMunicipalityId = toAssetMunicipalityId(
				pobIntegration.getConfigurationItemsBySerialNumberForEndOfLease(pobProperties.key(), computer.getSerialNumber()));

			if (isNull(assetMunicipalityId)) {
				// Retried rather than failed outright, since the computer is missing from POB or carries a municipality
				// nobody has mapped, and both are things someone can put right while the attempts last.
				recordFailedAttempt(computer, UNKNOWN_TO_POB);
				return;
			}

			computer.setAssetMunicipalityId(assetMunicipalityId);
			// Back to nothing spent, so that the dispatch run starts on a full budget. The two runs share the column but
			// fail for unrelated reasons, and POB having been slow to answer says nothing about whether SysMan will. A
			// computer that used most of its attempts getting looked up would otherwise be given up on after a single
			// bad call to SysMan.
			computer.setAttempts(0);
			// Don't store a potentially old failure for a successful lookup.
			computer.setErrorMessage(null);
			endOfLeaseComputerRepository.save(computer);
		} catch (final ServerProblem | RetryableException | CallNotPermittedException e) {
			// POB is unwell or out of reach, which is no fact about this computer. Kept off the attempts so that an
			// outage does not spend the budget of every computer waiting behind it. Listed by name rather than caught
			// as everything else, so that anything we did not foresee falls through to the branch below.
			recordDependencyFailure(computer, e.getMessage());
		} catch (final Exception e) {
			// POB turned our request down, or this row's own data broke us on the way through. Either way it counts:
			// a row nothing can make sense of would otherwise keep its place in every page for good, and a hundred of
			// them stop the queue for everyone behind them.
			recordFailedAttempt(computer, e.getMessage());
		}
	}

	/**
	 * A failure that belongs to POB rather than to the computer. The reason is written on the row so that a queue
	 * standing still can be read off it, but the attempts and the status are left as they were.
	 */
	private void recordDependencyFailure(final EndOfLeaseComputerEntity computer, final String errorMessage) {
		LOG.warn("POB could not be asked about serial number {}, leaving its attempts untouched: {}", computer.getSerialNumber(), errorMessage);

		// The run carries on and swallows this, so the scheduler aspect never sees it. Said here instead, or an outage
		// is invisible until the queue has aged enough for the queue indicator to notice.
		dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, "POB could not be reached: " + errorMessage);

		computer.setErrorMessage(errorMessage);
		endOfLeaseComputerRepository.save(computer);
	}

	private void recordFailedAttempt(final EndOfLeaseComputerEntity computer, final String errorMessage) {
		final var attempts = computer.getAttempts() + 1;

		computer.setAttempts(attempts);
		computer.setErrorMessage(errorMessage);

		if (attempts >= maximumAttempts) {
			computer.setStatus(FAILED);
			LOG.warn("Giving up the lookup of serial number {} after {} attempts: {}", computer.getSerialNumber(), attempts, errorMessage);

			// Giving up on a computer is the definition of FAILED: nobody but a person can take it further. The run
			// itself swallows this, so without saying so here the only place it shows is the log.
			dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, "Gave up the lookup of serial number %s after %d attempts".formatted(computer.getSerialNumber(), attempts));
		}

		endOfLeaseComputerRepository.save(computer);
	}
}
