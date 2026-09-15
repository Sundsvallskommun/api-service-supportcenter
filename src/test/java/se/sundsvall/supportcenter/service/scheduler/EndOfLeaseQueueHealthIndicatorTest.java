package se.sundsvall.supportcenter.service.scheduler;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseQueueHealthIndicatorTest {

	private static final Duration MAXIMUM_QUEUE_AGE = Duration.ofHours(24);

	@Mock
	private EndOfLeaseComputerRepository endOfLeaseComputerRepositoryMock;

	private EndOfLeaseQueueHealthIndicator endOfLeaseQueueHealthIndicator;

	@BeforeEach
	void setUp() {
		endOfLeaseQueueHealthIndicator = new EndOfLeaseQueueHealthIndicator(endOfLeaseComputerRepositoryMock, MAXIMUM_QUEUE_AGE);
	}

	@Test
	void anEmptyQueueIsUp() {
		when(endOfLeaseComputerRepositoryMock.findFirstByStatusOrderByCreated(PENDING)).thenReturn(Optional.empty());

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("UP");
		assertThat(health.getDetails()).containsEntry("Reason", "No computer is waiting");
	}

	@Test
	void aQueueThatIsMovingIsUp() {
		whenOldestWaitedFor(2);

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("UP");
		assertThat(health.getDetails()).containsEntry("Oldest waiting serial number", "J123ABC");
	}

	/**
	 * Neither run leaves a computer FAILED over an outage, so a queue that stops moving is the only sign of one. It has
	 * to reach the health endpoint or the outage is silent.
	 */
	@Test
	void aQueueStandingStillIsRestricted() {
		whenOldestWaitedFor(25);

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(health.getDetails().get("Reason").toString()).contains("has been waiting");
		assertThat(health.getDetails()).containsEntry("Oldest waiting serial number", "J123ABC");
	}

	/**
	 * DOWN would carry through to the aggregated health and take the instance out of rotation over a queue we could not
	 * read. The datasource has an indicator of its own for a database that is actually the problem.
	 */
	@Test
	void aQueueThatCannotBeReadIsRestrictedRatherThanDown() {
		when(endOfLeaseComputerRepositoryMock.findFirstByStatusOrderByCreated(PENDING)).thenThrow(new IllegalStateException("The database is unwell"));

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(health.getDetails().get("Reason").toString()).contains("The database is unwell");
	}

	private void whenOldestWaitedFor(final int hours) {
		when(endOfLeaseComputerRepositoryMock.findFirstByStatusOrderByCreated(PENDING)).thenReturn(Optional.of(
			EndOfLeaseComputerEntity.create()
				.withSerialNumber("J123ABC")
				.withAssetTag("AB12345")
				.withStatus(PENDING)
				.withCreated(OffsetDateTime.now().minusHours(hours))));
	}
}
