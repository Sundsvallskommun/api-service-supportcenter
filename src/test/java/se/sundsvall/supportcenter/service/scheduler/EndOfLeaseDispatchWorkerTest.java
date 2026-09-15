package se.sundsvall.supportcenter.service.scheduler;

import generated.client.sysman.SaveMessagesToTargetsCommand;
import generated.client.sysman.TargetReference;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.sysman.SysManIntegration;

import static generated.client.sysman.SaveMessagesToTargetsCommand.TargetTypeEnum.COMPUTER;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseDispatchWorkerTest {

	private static final String SUNDSVALL = "2281";
	private static final String ANGE = "2260";
	private static final int PAGE_SIZE = 100;
	private static final int MAXIMUM_ATTEMPTS = 5;
	private static final long MESSAGE_ID = 1;
	private static final String JOB_NAME = "end-of-lease-dispatch";

	@Mock
	private EndOfLeaseComputerRepository endOfLeaseComputerRepositoryMock;

	@Mock
	private SysManIntegration sysManIntegrationMock;

	@Mock
	private Dept44HealthUtility dept44HealthUtilityMock;

	@Captor
	private ArgumentCaptor<EndOfLeaseComputerEntity> computerCaptor;

	@Captor
	private ArgumentCaptor<SaveMessagesToTargetsCommand> commandCaptor;

	private EndOfLeaseDispatchWorker endOfLeaseDispatchWorker;

	@BeforeEach
	void setUp() {
		endOfLeaseDispatchWorker = new EndOfLeaseDispatchWorker(
			endOfLeaseComputerRepositoryMock, sysManIntegrationMock, dept44HealthUtilityMock, PAGE_SIZE, MAXIMUM_ATTEMPTS, MESSAGE_ID, JOB_NAME);
	}

	@Test
	void aComputerSysManAnsweredWithIsSent() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(List.of(targetReference("AB12345")));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(SENT);
		assertThat(computerCaptor.getValue().getSentAt()).isNotNull();
		assertThat(computerCaptor.getValue().getAttempts()).isZero();
		assertThat(computerCaptor.getValue().getErrorMessage()).isNull();
	}

	/**
	 * The asset tag is the computer name in SysMan, and the message is the one the property names.
	 */
	@Test
	void theCallNamesEveryComputerInTheGroup() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0), computer("CD67890", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(emptyList());

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(sysManIntegrationMock).sendMessagesToTargets(eq(SUNDSVALL), commandCaptor.capture());
		assertThat(commandCaptor.getValue().getTargets()).containsExactly("AB12345", "CD67890");
		assertThat(commandCaptor.getValue().getMessagesToSend()).containsExactly(MESSAGE_ID);
		assertThat(commandCaptor.getValue().getTargetType()).isEqualTo(COMPUTER);
		assertThat(commandCaptor.getValue().getTargetAll()).isFalse();
	}

	/**
	 * One call per installation. A computer belongs to the SysMan its municipality is run by, and an installation knows
	 * nothing of the other one's computers.
	 */
	@Test
	void everyMunicipalityIsItsOwnCall() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0), computer("CD67890", ANGE, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(List.of(targetReference("AB12345")));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(ANGE), any())).thenReturn(List.of(targetReference("CD67890")));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(sysManIntegrationMock).sendMessagesToTargets(eq(SUNDSVALL), any());
		verify(sysManIntegrationMock).sendMessagesToTargets(eq(ANGE), any());
		verifyNoMoreInteractions(sysManIntegrationMock);
	}

	/**
	 * A target SysMan does not know about is left out of the answer rather than reported, so a short answer is how a
	 * computer that was not reached shows up. It keeps its turn instead of being counted as sent.
	 */
	@Test
	void aComputerLeftOutOfTheAnswerIsRetried() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0), computer("CD67890", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(List.of(targetReference("AB12345")));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getAssetTag, EndOfLeaseComputerEntity::getStatus, EndOfLeaseComputerEntity::getAttempts)
			.containsExactly(
				tuple("AB12345", SENT, 0),
				tuple("CD67890", PENDING, 1));
	}

	@Test
	void anAnswerThatNamesTheComputerInAnotherCaseStillCounts() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(List.of(targetReference("ab12345")));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(SENT);
	}

	@Test
	void anAnswerWithoutTargetsRetriesEveryone() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(null);

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isOne();
	}

	/**
	 * One computer name the installation will not accept is enough to lose the whole group, so a turned down call has to
	 * count. Without that, the group never gets past the row that broke it.
	 */
	@Test
	void aCallTheInstallationTurnsDownCountsForTheWholeGroup() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0), computer("CD67890", SUNDSVALL, 2));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenThrow(new ClientProblem(BAD_REQUEST, "Invalid target name"));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getStatus, EndOfLeaseComputerEntity::getAttempts)
			.containsExactly(
				tuple(PENDING, 1),
				tuple(PENDING, 3));
	}

	/**
	 * An unwell or unreachable installation is no fact about any of the computers in the group, so spending their budget
	 * on it would give up on a queue that has nothing wrong with it. The reason is still written on every row, so a
	 * queue standing still can be read off it.
	 */
	@Test
	void anUnreachableInstallationDoesNotCostAnAttempt() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0), computer("CD67890", SUNDSVALL, 2));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenThrow(new ServerProblem(BAD_GATEWAY, "SysMan is unwell"));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getStatus, EndOfLeaseComputerEntity::getAttempts)
			.containsExactly(
				tuple(PENDING, 0),
				tuple(PENDING, 2));
	}

	/**
	 * An outage never leaves a computer FAILED, however long it lasts.
	 */
	@Test
	void anUnreachableInstallationNeverLeavesTheComputerFailed() {
		whenPageContains(computer("AB12345", SUNDSVALL, MAXIMUM_ATTEMPTS - 1));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenThrow(new ServerProblem(BAD_GATEWAY, "SysMan is unwell"));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS - 1);
	}

	@Test
	void theLastAttemptLeavesTheComputerFailed() {
		whenPageContains(computer("AB12345", SUNDSVALL, MAXIMUM_ATTEMPTS - 1));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(emptyList());

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(FAILED);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS);
	}

	/**
	 * A municipality with no installation configured is not an outage. Nothing but a human can put it straight, so it
	 * has to run the attempts down and reach FAILED rather than sit in the queue forever.
	 */
	@Test
	void aMunicipalityWithNoInstallationCountsAsAnAttempt() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any()))
			.thenThrow(Problem.valueOf(INTERNAL_SERVER_ERROR, "No SysMan installation is configured for municipality 2281"));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAttempts()).isOne();
	}

	/**
	 * The message is delivered by the time the outcome is written down, so a failure while writing it is not a failed
	 * send. Swallowed here it would stamp an error on rows already marked SENT and leave the rest to be sent a second
	 * time, which is the one thing the batch is deduplicated to avoid.
	 */
	@Test
	void aFailureWhileRecordingTheOutcomeIsNotMistakenForAFailedSend() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(List.of(targetReference("AB12345")));
		when(endOfLeaseComputerRepositoryMock.save(any())).thenThrow(new IllegalStateException("The database is unwell"));

		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> endOfLeaseDispatchWorker.processComputersReadyToSend());
	}

	/**
	 * The run swallows an outage so that the computers keep their attempts, which leaves the scheduler aspect with
	 * nothing to report. Saying so here is what puts it on the health endpoint on the run that hits it, rather than a
	 * day later when the queue has aged enough for the queue indicator to notice.
	 */
	@Test
	void anUnreachableInstallationIsReportedOnTheHealthEndpoint() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenThrow(new ServerProblem(BAD_GATEWAY, "SysMan is unwell"));

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("could not be reached"));
	}

	/**
	 * Giving up on a computer means nobody but a person can take it further, which is worth the same visibility as an
	 * outage. An ordinary attempt that still has budget left is not, or the indicator would never be green.
	 */
	@Test
	void givingUpOnAComputerIsReportedButAnOrdinaryAttemptIsNot() {
		whenPageContains(computer("AB12345", SUNDSVALL, 0), computer("CD67890", SUNDSVALL, MAXIMUM_ATTEMPTS - 1));
		when(sysManIntegrationMock.sendMessagesToTargets(eq(SUNDSVALL), any())).thenReturn(emptyList());

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("Gave up the send of computer name CD67890"));
		verifyNoMoreInteractions(dept44HealthUtilityMock);
	}

	@Test
	void anEmptyPageCallsNobody() {
		when(endOfLeaseComputerRepositoryMock.findByStatusAndAssetMunicipalityIdIsNotNullOrderByCreated(PENDING, PageRequest.ofSize(PAGE_SIZE)))
			.thenReturn(emptyList());

		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verify(endOfLeaseComputerRepositoryMock, never()).save(any());
		verifyNoInteractions(sysManIntegrationMock);
	}

	private void whenPageContains(final EndOfLeaseComputerEntity... computers) {
		when(endOfLeaseComputerRepositoryMock.findByStatusAndAssetMunicipalityIdIsNotNullOrderByCreated(PENDING, PageRequest.ofSize(PAGE_SIZE)))
			.thenReturn(List.of(computers));
	}

	private static EndOfLeaseComputerEntity computer(final String assetTag, final String assetMunicipalityId, final int attempts) {
		return EndOfLeaseComputerEntity.create()
			.withSerialNumber("SN" + assetTag)
			.withAssetTag(assetTag)
			.withAssetMunicipalityId(assetMunicipalityId)
			.withStatus(PENDING)
			.withAttempts(attempts);
	}

	private static TargetReference targetReference(final String name) {
		return new TargetReference().name(name);
	}
}
