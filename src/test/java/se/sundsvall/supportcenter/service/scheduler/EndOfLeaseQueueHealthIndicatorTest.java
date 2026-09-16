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
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
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
	 * Duration prints itself as PT74H48M33.899099S, which nobody reads at a glance. The threshold beside it stays as
	 * written, since that is the value standing in application.yml.
	 */
	@Test
	void theWaitingTimeIsReadable() {
		whenOldestWaitedFor(74);

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getDetails().get("Reason").toString())
			.contains("has been waiting 3d 2h")
			.contains("longer than PT24H")
			.doesNotContain("PT74H");
	}

	/**
	 * The run that gives up says so on the scheduler's own indicator, but the aspect resets that on the next run that
	 * goes well. Counted from the rows instead, a computer nobody can report stays reported until a person has dealt
	 * with it.
	 */
	@Test
	void aComputerThatWasGivenUpOnKeepsBeingReported() {
		when(endOfLeaseComputerRepositoryMock.countByStatus(FAILED)).thenReturn(2L);
		when(endOfLeaseComputerRepositoryMock.findFirstByStatusOrderByCreated(PENDING)).thenReturn(Optional.empty());

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(health.getDetails().get("Reason").toString()).contains("2 computer(s) have been given up on");
		assertThat(health.getDetails()).containsEntry("Given up on", 2L);
	}

	/**
	 * Both hold often enough: an outage that stops the queue is also what runs computers out of attempts. Reported one
	 * at a time the age won and the computers somebody has to deal with survived only as a detail, which is the more
	 * actionable of the two.
	 */
	@Test
	void anAgeingQueueWithComputersGivenUpOnReportsBoth() {
		when(endOfLeaseComputerRepositoryMock.countByStatus(FAILED)).thenReturn(3L);
		whenOldestWaitedFor(25);

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(health.getDetails().get("Reason").toString())
			.contains("has been waiting")
			.contains("3 computer(s) have been given up on");
		assertThat(health.getDetails()).containsEntry("Given up on", 3L);
	}

	@Test
	void aQueueThatIsMovingWithNobodyGivenUpOnIsUp() {
		when(endOfLeaseComputerRepositoryMock.countByStatus(FAILED)).thenReturn(0L);
		whenOldestWaitedFor(2);

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("UP");
		assertThat(health.getDetails()).containsEntry("Given up on", 0L);
	}

	/**
	 * Saying a queue has stopped moving without saying what stopped it leaves the answer a database query away, in a
	 * place nobody looks first. The run that last failed on this computer wrote the reason on its row.
	 */
	@Test
	void theReasonTheOldestComputerIsStuckIsOnTheEndpoint() {
		whenOldestWaitedFor(25, "POB turned the job's key down: 401 Unauthorized");

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getDetails()).containsEntry("Last failure", "POB turned the job's key down: 401 Unauthorized");
	}

	/**
	 * A computer that has never failed has nothing to say, and an empty line would only read as a missing value.
	 */
	@Test
	void aComputerThatHasNotFailedCarriesNoLastFailure() {
		whenOldestWaitedFor(2);

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getDetails()).doesNotContainKey("Last failure");
	}

	@Test
	void aQueueThatCannotBeReadIsRestrictedRatherThanDown() {
		when(endOfLeaseComputerRepositoryMock.findFirstByStatusOrderByCreated(PENDING)).thenThrow(new IllegalStateException("The database is unwell"));

		final var health = endOfLeaseQueueHealthIndicator.health();

		assertThat(health.getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(health.getDetails().get("Reason").toString()).contains("The database is unwell");
	}

	private void whenOldestWaitedFor(final int hours) {
		whenOldestWaitedFor(hours, null);
	}

	private void whenOldestWaitedFor(final int hours, final String errorMessage) {
		when(endOfLeaseComputerRepositoryMock.findFirstByStatusOrderByCreated(PENDING)).thenReturn(Optional.of(
			EndOfLeaseComputerEntity.create()
				.withSerialNumber("J123ABC")
				.withAssetTag("AB12345")
				.withStatus(PENDING)
				.withErrorMessage(errorMessage)
				.withCreated(OffsetDateTime.now().minusHours(hours))));
	}
}
