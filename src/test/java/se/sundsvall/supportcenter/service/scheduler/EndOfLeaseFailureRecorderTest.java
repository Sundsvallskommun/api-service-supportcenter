package se.sundsvall.supportcenter.service.scheduler;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

/**
 * The retry policy of the end of lease feature, proven once. Both runs decide for themselves what counts as a failure
 * of the computer and what counts as the other end being unwell; what follows from that decision is all here.
 */
@ExtendWith(MockitoExtension.class)
class EndOfLeaseFailureRecorderTest {

	private static final int MAXIMUM_ATTEMPTS = 5;
	private static final Duration BACKOFF = Duration.ofHours(6);
	private static final String JOB_NAME = "end-of-lease-lookup";
	private static final String SUBJECT = "serial number J123ABC";

	@Mock
	private EndOfLeaseComputerRepository endOfLeaseComputerRepositoryMock;

	@Mock
	private Dept44HealthUtility dept44HealthUtilityMock;

	@Captor
	private ArgumentCaptor<EndOfLeaseComputerEntity> computerCaptor;

	private EndOfLeaseFailureRecorder endOfLeaseFailureRecorder;

	@BeforeEach
	void setUp() {
		endOfLeaseFailureRecorder = new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS, BACKOFF);
	}

	@Test
	void anAttemptIsSpentAndTheReasonKept() {
		endOfLeaseFailureRecorder.recordFailedAttempt(computer(1), JOB_NAME, SUBJECT, "POB is unwell");

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(2);
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getErrorMessage()).isEqualTo("POB is unwell");
		verifyNoInteractions(dept44HealthUtilityMock);
	}

	/**
	 * Giving up means nobody but a person can take the computer further, which is the one per-computer outcome worth
	 * the health endpoint. An attempt with budget left is not, or the indicator would never be green.
	 */
	@Test
	void theLastAttemptGivesUpAndSaysSo() {
		endOfLeaseFailureRecorder.recordFailedAttempt(computer(MAXIMUM_ATTEMPTS - 1), JOB_NAME, SUBJECT, "POB is unwell");

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(FAILED);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS);
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("Gave up on serial number J123ABC after 5 attempts"));
	}

	/**
	 * The other end being unwell is no fact about any of these computers, so it costs them nothing. The reason is still
	 * written on every row, since a queue standing still has to be readable from the rows.
	 */
	@Test
	void aDependencyFailureCostsNobodyAnAttempt() {
		endOfLeaseFailureRecorder.recordDependencyFailure(List.of(computer(3), computer(0)), JOB_NAME, "POB could not be reached: no route to host");

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getAttempts, EndOfLeaseComputerEntity::getStatus, EndOfLeaseComputerEntity::getErrorMessage)
			.containsExactly(
				tuple(3, PENDING, "POB could not be reached: no route to host"),
				tuple(0, PENDING, "POB could not be reached: no route to host"));

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(JOB_NAME, "POB could not be reached: no route to host");
	}

	/**
	 * Costing no attempt is what keeps an outage from draining the queue into FAILED, and it is also what would let the
	 * same rows fill the front of every page for good. The hold is the other half of that bargain.
	 */
	@Test
	void aDependencyFailureHoldsTheComputerBackForTheBackoffWindow() {
		final var before = OffsetDateTime.now();

		endOfLeaseFailureRecorder.recordDependencyFailure(List.of(computer(0)), JOB_NAME, "SysMan could not be reached");

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getRetryAfter())
			.isNotNull()
			.isBetween(before.plus(BACKOFF), OffsetDateTime.now().plus(BACKOFF));
	}

	/**
	 * A spent attempt takes the computer to FAILED after a handful of tries, so it leaves the queue on its own and
	 * needs no hold. Holding it as well would only delay a computer whose own data is the problem.
	 */
	@Test
	void aSpentAttemptDoesNotHoldTheComputerBack() {
		endOfLeaseFailureRecorder.recordFailedAttempt(computer(0), JOB_NAME, SUBJECT, "SysMan did not recognize the computer name");

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getRetryAfter()).isNull();
	}

	/**
	 * A reason wider than its column would be rejected by the insert from inside the very call that was recording the
	 * failure, which takes the run down with it.
	 */
	@Test
	void aReasonWiderThanItsColumnIsCutToFit() {
		endOfLeaseFailureRecorder.recordFailedAttempt(computer(0), JOB_NAME, SUBJECT, "x".repeat(3000));

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getErrorMessage()).hasSize(2048);
	}

	private static EndOfLeaseComputerEntity computer(final int attempts) {
		return EndOfLeaseComputerEntity.create()
			.withSerialNumber("J123ABC")
			.withAssetTag("AB12345")
			.withStatus(PENDING)
			.withAttempts(attempts);
	}
}
