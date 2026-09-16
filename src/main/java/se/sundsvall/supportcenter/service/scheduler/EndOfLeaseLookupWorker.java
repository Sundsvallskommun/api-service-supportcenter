package se.sundsvall.supportcenter.service.scheduler;

import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
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
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.integration.pob.configuration.POBProperties;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
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

	private static final String NO_KEY = "integration.pob.key is not configured, so the job has no POB identity to look computers up with";

	private static final String UNKNOWN_TO_POB = "POB answered the serial number with no configuration item holding a municipality we can route on";

	/**
	 * How many computers in a row may fail on POB itself before the run gives up on the rest of the page.
	 *
	 * POB is the same POB for all 250 in a page, so working on once it has stopped answering buys nothing. Three rather
	 * than one, or a POB that answers half the calls would barely move the queue. Reset by any computer that goes
	 * through, so this counts a run of failures and not a total.
	 */
	private static final int MAXIMUM_CONSECUTIVE_DEPENDENCY_FAILURES = 3;

	private static final String GAVE_UP_ON_THE_PAGE = "POB failed %d calls in a row, so the rest of this page is left for the next run rather than tried against a POB that is plainly not answering";

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final POBIntegration pobIntegration;
	private final POBProperties pobProperties;
	private final EndOfLeaseFailureRecorder endOfLeaseFailureRecorder;
	private final Dept44HealthUtility dept44HealthUtility;
	private final int pageSize;
	private final String jobName;

	public EndOfLeaseLookupWorker(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		final POBIntegration pobIntegration,
		final POBProperties pobProperties,
		final EndOfLeaseFailureRecorder endOfLeaseFailureRecorder,
		final Dept44HealthUtility dept44HealthUtility,
		@Value("${scheduler.end-of-lease.lookup.page-size}") final int pageSize,
		@Value("${scheduler.end-of-lease.lookup.name}") final String jobName) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.pobIntegration = pobIntegration;
		this.pobProperties = pobProperties;
		this.endOfLeaseFailureRecorder = endOfLeaseFailureRecorder;
		this.dept44HealthUtility = dept44HealthUtility;
		this.pageSize = pageSize;
		this.jobName = jobName;
	}

	public void processComputersAwaitingLookup() {
		// The key is the job's own, and only this job needs one, so a missing key is not allowed to stop the service
		// from starting. Caught here instead, where it is one unhealthy job rather than an API that will not come up.
		if (isBlank(pobProperties.key())) {
			LOG.error(NO_KEY);
			dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, NO_KEY);
			return;
		}

		final var computers = endOfLeaseComputerRepository
			.findAwaitingLookup(PENDING, now(systemDefault()), PageRequest.ofSize(pageSize));

		final var numberOfComputers = computers.size();

		if (numberOfComputers == 0) {
			LOG.info("Found no computers to look up the municipality for.");
		} else {
			LOG.info("Found {} computers to look up the municipality for", numberOfComputers);
			lookUpPage(computers);
		}
	}

	/**
	 * Works through the page, and stops once POB has turned enough calls down in a row to have stopped answering.
	 *
	 * Everything not reached is left exactly as it was found, so the next run starts where this one stopped. A failure
	 * of the computer itself never stops the run, since the next computer has nothing to do with it.
	 */
	private void lookUpPage(final List<EndOfLeaseComputerEntity> computers) {
		var consecutiveDependencyFailures = 0;

		for (final var computer : computers) {
			if (lookUp(computer)) {
				consecutiveDependencyFailures++;

				if (consecutiveDependencyFailures >= MAXIMUM_CONSECUTIVE_DEPENDENCY_FAILURES) {
					LOG.warn(GAVE_UP_ON_THE_PAGE.formatted(consecutiveDependencyFailures));
					return;
				}
			} else {
				consecutiveDependencyFailures = 0;
			}
		}
	}

	private static String subject(final EndOfLeaseComputerEntity computer) {
		return "serial number " + computer.getSerialNumber();
	}

	/**
	 * @return whether POB was the thing that failed, rather than this computer
	 */
	private boolean lookUp(final EndOfLeaseComputerEntity computer) {
		try {
			final var assetMunicipalityId = toAssetMunicipalityId(
				pobIntegration.getConfigurationItemsBySerialNumberForEndOfLease(pobProperties.key(), computer.getSerialNumber()));

			if (isNull(assetMunicipalityId)) {
				// Retried rather than failed outright, since the computer is missing from POB or carries a municipality
				// nobody has mapped, and both are things someone can put right while the attempts last.
				endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), UNKNOWN_TO_POB);
				return false;
			}

			computer.setAssetMunicipalityId(assetMunicipalityId);
			// Back to nothing spent, so that the dispatch run starts on a full budget. The two runs share the column but
			// fail for unrelated reasons, and POB having been slow to answer says nothing about whether SysMan will. A
			// computer that used most of its attempts getting looked up would otherwise be given up on after a single
			// bad call to SysMan.
			computer.setAttempts(0);
			// Don't carry an old failure into a successful lookup. retryAfter is cleared for the same reason, though
			// only the dispatch run sets one now.
			computer.setErrorMessage(null);
			computer.setRetryAfter(null);
			endOfLeaseComputerRepository.save(computer);
			return false;
		} catch (final ServerProblem | RetryableException | CallNotPermittedException e) {
			// POB is unwell or out of reach, which is no fact about this computer, so no attempt is spent. Listed by
			// name rather than caught as everything else, so anything we did not foresee falls through below.
			endOfLeaseFailureRecorder.noteDependencyFailure(computer, jobName, subject(computer), "POB could not be reached: " + e.getMessage());
			return true;
		} catch (final ClientProblem e) {
			// The key this job authenticates with is its own, and POB turning it down says nothing about the computer
			// the call happened to be about. Counted, a rotated key would drain the whole queue into FAILED before
			// anyone noticed. Every other 4xx is about this row and falls through below.
			if (e.getStatus() == UNAUTHORIZED || e.getStatus() == FORBIDDEN) {
				endOfLeaseFailureRecorder.noteDependencyFailure(computer, jobName, subject(computer), "POB turned the job's key down: " + e.getMessage());
				return true;
			}

			endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), e.getMessage());
			return false;
		} catch (final Exception e) {
			// POB turned our request down, or this row's own data broke us on the way through. Either way it counts:
			// a row nothing can make sense of would otherwise keep its place in every page for good, and a hundred of
			// them stop the queue for everyone behind them.
			endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), e.getMessage());
			return false;
		}
	}

}
