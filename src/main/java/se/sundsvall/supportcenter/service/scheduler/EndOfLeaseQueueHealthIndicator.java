package se.sundsvall.supportcenter.service.scheduler;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

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
			return endOfLeaseComputerRepository.findFirstByStatusOrderByCreated(PENDING)
				.map(this::toHealth)
				.orElseGet(() -> Health.up().withDetail(DETAIL_REASON, "No computer is waiting").build());
		} catch (final Exception e) {
			// Answering DOWN would take the instance out of rotation over a queue we could not read, and the datasource
			// has an indicator of its own for the case where the database is the problem.
			return Health.status(RESTRICTED)
				.withDetail(DETAIL_REASON, "The queue could not be read: " + e.getMessage())
				.build();
		}
	}

	private Health toHealth(final EndOfLeaseComputerEntity oldest) {
		final var age = Duration.between(oldest.getCreated(), OffsetDateTime.now(ZoneId.systemDefault()));

		final var builder = age.compareTo(maximumQueueAge) > 0
			? Health.status(RESTRICTED).withDetail(DETAIL_REASON, "The oldest computer has been waiting %s, which is longer than %s".formatted(age, maximumQueueAge))
			: Health.up();

		return builder
			.withDetail(DETAIL_SERIAL_NUMBER, oldest.getSerialNumber())
			.withDetail(DETAIL_WAITING_SINCE, oldest.getCreated())
			.build();
	}
}
