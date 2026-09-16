package se.sundsvall.supportcenter.service.scheduler;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toErrorMessage;

/**
 * What the two end of lease runs do when something goes wrong.
 *
 * The runs disagree about what counts as a failure of the computer and what counts as the other end being unwell, and
 * that judgement stays with each of them. What happens once the judgement is made is the same either way, and lives
 * here: an attempt spent or not, when a computer is given up on, and what reaches the health endpoint. Carried in one
 * place because it is the policy of the feature, and two copies of it drift the first time one of them is changed.
 */
@Component
class EndOfLeaseFailureRecorder {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseFailureRecorder.class);

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final Dept44HealthUtility dept44HealthUtility;
	private final int maximumAttempts;
	private final Duration dependencyFailureBackoff;

	EndOfLeaseFailureRecorder(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		final Dept44HealthUtility dept44HealthUtility,
		@Value("${scheduler.end-of-lease.maximum-attempts}") final int maximumAttempts,
		@Value("${scheduler.end-of-lease.dependency-failure-backoff}") final Duration dependencyFailureBackoff) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.dept44HealthUtility = dept44HealthUtility;
		this.maximumAttempts = maximumAttempts;
		this.dependencyFailureBackoff = dependencyFailureBackoff;
	}

	/**
	 * Spends one of a computer's attempts, and gives up on it once they are gone.
	 *
	 * @param computer     the computer that failed
	 * @param jobName      the run reporting it, which is the name its health indicator is registered under
	 * @param subject      what to call the computer in what a person reads, such as "serial number J123ABC"
	 * @param errorMessage what went wrong
	 */
	void recordFailedAttempt(final EndOfLeaseComputerEntity computer, final String jobName, final String subject, final String errorMessage) {
		final var attempts = computer.getAttempts() + 1;

		computer.setAttempts(attempts);
		computer.setErrorMessage(toErrorMessage(errorMessage));

		if (attempts >= maximumAttempts) {
			computer.setStatus(FAILED);

			// Giving up on a computer is the definition of FAILED: nobody but a person can take it further. The run
			// itself swallows this, so without saying so here the only place it shows is the log.
			final var gaveUp = "Gave up on %s after %d attempts".formatted(subject, attempts);
			LOG.warn("{}: {}", gaveUp, errorMessage);
			dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, gaveUp);
		}

		endOfLeaseComputerRepository.save(computer);
	}

	/**
	 * Records that the other end could not be reached, which is no fact about any of these computers and costs them
	 * nothing. The reason is written on every row so that a queue standing still can be read off it.
	 *
	 * @param computers the computers the call was for
	 * @param jobName   the run reporting it, which is the name its health indicator is registered under
	 * @param reason    what could not be reached and why, in the words of the run that found out
	 */
	void recordDependencyFailure(final List<EndOfLeaseComputerEntity> computers, final String jobName, final String reason) {
		// The run carries on and swallows this, so the scheduler aspect never sees it. Said here instead, or an outage
		// is invisible until the queue has aged enough for the queue indicator to notice.
		LOG.warn("{}. The attempts of {} computer(s) are left untouched, and they are held back for {}.", reason, computers.size(), dependencyFailureBackoff);
		dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, reason);

		// Held back rather than left free to be picked up again on the very next pass. Costing no attempt is what keeps
		// an outage from draining the queue into FAILED, but it is also what lets such a row keep its place at the front
		// of every page for good, and a page worth of them stops everyone behind them.
		final var retryAfter = now(systemDefault()).plus(dependencyFailureBackoff);

		computers.forEach(computer -> {
			computer.setErrorMessage(toErrorMessage(reason));
			computer.setRetryAfter(retryAfter);
			endOfLeaseComputerRepository.save(computer);
		});
	}
}
