package se.sundsvall.supportcenter.service.scheduler;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

/**
 * Says whether the end of lease queue is draining.
 *
 * Neither run leaves a computer FAILED over an unreachable POB or SysMan, which is deliberate but quiet: an outage
 * shows up as a queue that stops moving rather than as rows anyone is told about. This is what turns that silence into
 * something visible on the health endpoint.
 *
 * Deliberately its own indicator rather than the one the scheduler aspect keeps per job. That one is only written while
 * a job runs, so a job that has stopped running altogether, the case most worth catching, would leave it standing at
 * whatever it last said. This is read on every health scrape and the queue ages whether anything picks it up or not.
 */
@Component
public class EndOfLeaseQueueHealthIndicator implements HealthIndicator {

	/**
	 * The same status the dept44 scheduler indicators use. Not DOWN on purpose: an ageing queue is something to look at,
	 * not a reason to take the instance out of rotation, and DOWN would carry through to the aggregated health.
	 */
	private static final String RESTRICTED = "RESTRICTED";

	private static final String DETAIL_REASON = "Reason";
	private static final String DETAIL_WAITING_SINCE = "Waiting since";
	private static final String DETAIL_SERIAL_NUMBER = "Oldest waiting serial number";
	private static final String DETAIL_GIVEN_UP = "Given up on";
	private static final String DETAIL_LAST_FAILURE = "Last failure";

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;
	private final Duration maximumQueueAge;

	public EndOfLeaseQueueHealthIndicator(
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository,
		@Value("${scheduler.end-of-lease.maximum-queue-age}") final Duration maximumQueueAge) {

		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
		this.maximumQueueAge = maximumQueueAge;
	}

	@Override
	public Health health() {
		try {
			// Counted here rather than left to the run that gave up. That one says so on the scheduler's own indicator,
			// but the aspect resets it on the next run that goes well, so an hour later nothing remembers. A computer
			// nobody can report is only off this list once a person has dealt with it.
			final var givenUp = endOfLeaseComputerRepository.countByStatus(FAILED);

			return endOfLeaseComputerRepository.findFirstByStatusOrderByCreated(PENDING)
				.map(oldest -> toHealth(oldest, givenUp))
				.orElseGet(() -> toHealth(givenUp));
		} catch (final Exception e) {
			// Answering DOWN would take the instance out of rotation over a queue we could not read, and the datasource
			// has an indicator of its own for the case where the database is the problem.
			return Health.status(RESTRICTED)
				.withDetail(DETAIL_REASON, "The queue could not be read: " + e.getMessage())
				.build();
		}
	}

	private Health toHealth(final EndOfLeaseComputerEntity oldest, final long givenUp) {
		final var age = Duration.between(oldest.getCreated(), OffsetDateTime.now(ZoneId.systemDefault()));

		// Both reasons reach Reason when both hold. Reported one at a time the age won, and the computers somebody has
		// to deal with survived only as a detail, which is the more actionable of the two.
		final var reasons = new ArrayList<String>();

		if (age.compareTo(maximumQueueAge) > 0) {
			reasons.add("The oldest computer has been waiting %s, which is longer than %s".formatted(readable(age), maximumQueueAge));
		}
		if (givenUp > 0) {
			reasons.add(givenUpReason(givenUp));
		}

		final var builder = reasons.isEmpty()
			? Health.up()
			: Health.status(RESTRICTED).withDetail(DETAIL_REASON, String.join(". ", reasons));

		builder
			.withDetail(DETAIL_SERIAL_NUMBER, oldest.getSerialNumber())
			.withDetail(DETAIL_WAITING_SINCE, oldest.getCreated())
			.withDetail(DETAIL_GIVEN_UP, givenUp);

		// Why it is still waiting, in the words of whichever run last failed on it. Without this the endpoint says a
		// queue has stopped moving but not what stopped it, and the answer is a database query away in a place nobody
		// looks first. Left out when the row carries no failure, since an empty line only reads as a missing value.
		ofNullable(oldest.getErrorMessage()).ifPresent(errorMessage -> builder.withDetail(DETAIL_LAST_FAILURE, errorMessage));

		return builder.build();
	}

	/**
	 * A waiting time in the two units that matter, for a line a person reads at a glance. Duration prints itself as
	 * PT74H48M33.899099S, which is a machine's answer to a question nobody asked.
	 *
	 * The threshold beside it is left as it is written, since that is the value standing in application.yml and the
	 * reader may well be on their way there to change it.
	 */
	private static String readable(final Duration age) {
		if (age.toDaysPart() > 0) {
			return "%dd %dh".formatted(age.toDaysPart(), age.toHoursPart());
		}
		if (age.toHoursPart() > 0) {
			return "%dh %dm".formatted(age.toHoursPart(), age.toMinutesPart());
		}
		return "%dm".formatted(age.toMinutesPart());
	}

	private Health toHealth(final long givenUp) {
		// An empty queue is not the whole story. Nothing is waiting, but a computer that was given up on is still
		// somebody's to deal with, and saying the queue is empty would talk over it.
		if (givenUp > 0) {
			return restrictedIfGivenUp(givenUp)
				.withDetail(DETAIL_GIVEN_UP, givenUp)
				.build();
		}

		return Health.up()
			.withDetail(DETAIL_REASON, "No computer is waiting")
			.withDetail(DETAIL_GIVEN_UP, givenUp)
			.build();
	}

	/**
	 * A computer that has been given up on needs a person, and says so for as long as it is in that state.
	 */
	private static Health.Builder restrictedIfGivenUp(final long givenUp) {
		if (givenUp > 0) {
			return Health.status(RESTRICTED).withDetail(DETAIL_REASON, givenUpReason(givenUp));
		}
		return Health.up();
	}

	private static String givenUpReason(final long givenUp) {
		return "%d computer(s) have been given up on and need a person to look at them".formatted(givenUp);
	}
}
