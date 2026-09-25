package se.sundsvall.supportcenter.service.scheduler;

import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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

import static java.lang.Math.min;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toAssetMunicipalityIds;

/**
 * The work the lookup run does. Separate from the scheduler so that the scheduler stays the part that says when, and
 * this stays the part that says what.
 *
 * Each computer is saved as soon as its call answers, so a run cut short by its lock keeps what it got through.
 */
@Component
public class EndOfLeaseLookupWorker {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseLookupWorker.class);

	private static final String NO_KEY = "integration.pob.key is not configured, so the job has no POB identity to look computers up with";

	private static final String UNKNOWN_TO_POB = "POB answered the serial number with no configuration item holding a municipality we can route on";

	/**
	 * How many calls in a row may fail on POB itself before the run gives up on the rest of the page. Three rather than
	 * one, so a POB that answers half the calls still moves the queue. Any call that goes through resets the count.
	 */
	private static final int MAXIMUM_CONSECUTIVE_DEPENDENCY_FAILURES = 3;

	private static final String GAVE_UP_ON_THE_PAGE = "POB failed %d calls in a row, so the rest of this page is left for the next run rather than tried against a POB that is plainly not answering";

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final POBIntegration pobIntegration;
	private final POBProperties pobProperties;
	private final EndOfLeaseFailureRecorder endOfLeaseFailureRecorder;
	private final Dept44HealthUtility dept44HealthUtility;
	private final int pageSize;
	private final int serialNumbersPerCall;
	private final String jobName;

	public EndOfLeaseLookupWorker(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		final POBIntegration pobIntegration,
		final POBProperties pobProperties,
		final EndOfLeaseFailureRecorder endOfLeaseFailureRecorder,
		final Dept44HealthUtility dept44HealthUtility,
		@Value("${scheduler.end-of-lease.lookup.page-size}") final int pageSize,
		@Value("${scheduler.end-of-lease.lookup.serial-numbers-per-call}") final int serialNumbersPerCall,
		@Value("${scheduler.end-of-lease.lookup.name}") final String jobName) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.pobIntegration = pobIntegration;
		this.pobProperties = pobProperties;
		this.endOfLeaseFailureRecorder = endOfLeaseFailureRecorder;
		this.dept44HealthUtility = dept44HealthUtility;
		this.pageSize = pageSize;
		this.serialNumbersPerCall = serialNumbersPerCall;
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
	 * Works through the page a call at a time and stops after {@link #MAXIMUM_CONSECUTIVE_DEPENDENCY_FAILURES} POB
	 * failures in a row. What is not reached is left untouched for the next run.
	 */
	private void lookUpPage(final List<EndOfLeaseComputerEntity> computers) {
		var consecutiveDependencyFailures = 0;

		for (final var call : toCalls(computers)) {
			if (lookUp(call)) {
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

	/**
	 * The page split into calls of at most serial-numbers-per-call computers. See application.yml for the size.
	 */
	private List<List<EndOfLeaseComputerEntity>> toCalls(final List<EndOfLeaseComputerEntity> computers) {
		return IntStream.iterate(0, from -> from < computers.size(), from -> from + serialNumbersPerCall)
			.mapToObj(from -> computers.subList(from, min(from + serialNumbersPerCall, computers.size())))
			.toList();
	}

	private static String subject(final EndOfLeaseComputerEntity computer) {
		return "serial number " + computer.getSerialNumber();
	}

	/**
	 * Asks POB about the computers of one call and writes the answer on each. A POB failure costs no attempts, any
	 * other failure costs every computer of the call one.
	 *
	 * @return whether POB was what failed
	 */
	private boolean lookUp(final List<EndOfLeaseComputerEntity> computers) {
		// A computer can sit in several batches, and one lookup answers all its rows.
		final var serialNumbers = computers.stream()
			.map(EndOfLeaseComputerEntity::getSerialNumber)
			.distinct()
			.toList();

		final Map<String, String> assetMunicipalityIds;

		try {
			assetMunicipalityIds = toAssetMunicipalityIds(serialNumbers,
				pobIntegration.getConfigurationItemsBySerialNumbersForEndOfLease(pobProperties.key(), serialNumbers));
		} catch (final ServerProblem | RetryableException | CallNotPermittedException e) {
			// POB being down says nothing about the computers. Listed by name, so anything unforeseen falls through
			// below.
			endOfLeaseFailureRecorder.noteDependencyFailure(computers, jobName, "POB could not be reached: " + e.getMessage());
			return true;
		} catch (final ClientProblem e) {
			// A rejected key is not the computers' fault, and counting it would drain the queue into FAILED after a key
			// rotation. Any other 4xx costs each computer an attempt.
			if (e.getStatus() == UNAUTHORIZED || e.getStatus() == FORBIDDEN) {
				endOfLeaseFailureRecorder.noteDependencyFailure(computers, jobName, "POB turned the job's key down: " + e.getMessage());
				return true;
			}

			recordFailedAttempts(computers, e.getMessage());
			return false;
		} catch (final Exception e) {
			// Counted, or a call that always breaks keeps its place at the front of every page and blocks the queue.
			recordFailedAttempts(computers, e.getMessage());
			return false;
		}

		computers.forEach(computer -> write(computer, assetMunicipalityIds.get(computer.getSerialNumber())));
		return false;
	}

	private void write(final EndOfLeaseComputerEntity computer, final String assetMunicipalityId) {
		if (isNull(assetMunicipalityId)) {
			// Retried rather than failed, since someone can add the computer to POB or fix its municipality meanwhile.
			endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), UNKNOWN_TO_POB);
			return;
		}

		computer.setAssetMunicipalityId(assetMunicipalityId);
		// Reset, so the dispatch run starts with a full budget. The runs share the column but fail for unrelated
		// reasons.
		computer.setAttempts(0);
		// Don't carry an old failure over. Only the dispatch run sets retryAfter now, but it is cleared all the same.
		computer.setErrorMessage(null);
		computer.setRetryAfter(null);
		endOfLeaseComputerRepository.save(computer);
	}

	private void recordFailedAttempts(final List<EndOfLeaseComputerEntity> computers, final String errorMessage) {
		computers.forEach(computer -> endOfLeaseFailureRecorder.recordFailedAttempt(computer, jobName, subject(computer), errorMessage));
	}

}
