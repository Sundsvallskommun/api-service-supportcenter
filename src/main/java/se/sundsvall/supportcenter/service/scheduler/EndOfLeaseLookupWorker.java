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
			computers.forEach(this::lookUp);
		}
	}

	private static String subject(final EndOfLeaseComputerEntity computer) {
		return "serial number " + computer.getSerialNumber();
	}

	private void lookUp(final EndOfLeaseComputerEntity computer) {
		try {
			final var assetMunicipalityId = toAssetMunicipalityId(
				pobIntegration.getConfigurationItemsBySerialNumberForEndOfLease(pobProperties.key(), computer.getSerialNumber()));

			if (isNull(assetMunicipalityId)) {
				// Retried rather than failed outright, since the computer is missing from POB or carries a municipality
				// nobody has mapped, and both are things someone can put right while the attempts last.
				endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), UNKNOWN_TO_POB);
				return;
			}

			computer.setAssetMunicipalityId(assetMunicipalityId);
			// Back to nothing spent, so that the dispatch run starts on a full budget. The two runs share the column but
			// fail for unrelated reasons, and POB having been slow to answer says nothing about whether SysMan will. A
			// computer that used most of its attempts getting looked up would otherwise be given up on after a single
			// bad call to SysMan.
			computer.setAttempts(0);
			// Don't store a potentially old failure for a successful lookup, and let the dispatch run have it at once
			// rather than leave a hold from an outage that is plainly over.
			computer.setErrorMessage(null);
			computer.setRetryAfter(null);
			endOfLeaseComputerRepository.save(computer);
		} catch (final ServerProblem | RetryableException | CallNotPermittedException e) {
			// POB is unwell or out of reach, which is no fact about this computer. Kept off the attempts so that an
			// outage does not spend the budget of every computer waiting behind it. Listed by name rather than caught
			// as everything else, so that anything we did not foresee falls through to the branch below.
			endOfLeaseFailureRecorder.recordDependencyFailure(List.of(computer), jobName, "POB could not be reached: " + e.getMessage());
		} catch (final ClientProblem e) {
			// The key this job authenticates with is its own, and POB turning it down says nothing about the computer
			// the call happened to be about. Counted, it would empty the budget of every computer in the queue over a
			// key somebody rotated, and the first anyone would hear of it is a queue that had already drained into
			// FAILED. Every other 4xx is about this row and falls through to the branch below.
			if (e.getStatus() == UNAUTHORIZED || e.getStatus() == FORBIDDEN) {
				endOfLeaseFailureRecorder.recordDependencyFailure(List.of(computer), jobName, "POB turned the job's key down: " + e.getMessage());
			} else {
				endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), e.getMessage());
			}
		} catch (final Exception e) {
			// POB turned our request down, or this row's own data broke us on the way through. Either way it counts:
			// a row nothing can make sense of would otherwise keep its place in every page for good, and a hundred of
			// them stop the queue for everyone behind them.
			endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), e.getMessage());
		}
	}

}
