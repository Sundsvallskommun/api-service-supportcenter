package se.sundsvall.supportcenter.service.scheduler;

import generated.client.pob.PobPayload;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.integration.pob.configuration.POBProperties;

import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseLookupWorkerTest {

	private static final String POB_KEY = "pobKey";
	private static final String SERIAL_NUMBER = "J123ABC";
	private static final int PAGE_SIZE = 100;
	private static final int SERIAL_NUMBERS_PER_CALL = 2;
	private static final int MAXIMUM_ATTEMPTS = 5;
	private static final Duration BACKOFF = Duration.ofHours(6);
	private static final String JOB_NAME = "end-of-lease-lookup";

	@Mock
	private EndOfLeaseComputerRepository endOfLeaseComputerRepositoryMock;

	@Mock
	private POBIntegration pobIntegrationMock;

	@Mock
	private Dept44HealthUtility dept44HealthUtilityMock;

	@Captor
	private ArgumentCaptor<EndOfLeaseComputerEntity> computerCaptor;

	private EndOfLeaseLookupWorker endOfLeaseLookupWorker;

	@BeforeEach
	void setUp() {
		// A real recorder over the same mocks. The policy it carries is proven once in EndOfLeaseFailureRecorderTest,
		// and keeping it real here lets these tests go on saying what a run leaves behind rather than which collaborator
		// it called.
		endOfLeaseLookupWorker = new EndOfLeaseLookupWorker(
			endOfLeaseComputerRepositoryMock,
			pobIntegrationMock,
			new POBProperties(1, 2, POB_KEY),
			new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS, BACKOFF),
			dept44HealthUtilityMock,
			PAGE_SIZE,
			SERIAL_NUMBERS_PER_CALL,
			JOB_NAME);
	}

	@Test
	void municipalityIsWrittenOnTheComputer() {
		final var computer = computer(PENDING, 0);
		whenPageContains(computer);
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(configurationItem("2281"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isEqualTo("2281");
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isZero();
		assertThat(computerCaptor.getValue().getErrorMessage()).isNull();
	}

	/**
	 * POB holds the municipality as either the id or the name it belongs to, and a computer has to be routable whichever
	 * one it was written with.
	 */
	@Test
	void municipalityWrittenAsANameIsWrittenAsAnId() {
		whenPageContains(computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(configurationItem("Sundsvall"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isEqualTo("2281");
	}

	/**
	 * A computer that took a few goes to look up has to enter the dispatch run on a full budget, since what POB did says
	 * nothing about what SysMan will do. The error it carried from those goes is no longer true either.
	 */
	@Test
	void aLookupThatSucceedsOnARetryStartsOver() {
		whenPageContains(computer(PENDING, 3).withErrorMessage("POB was unwell"));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(configurationItem("2281"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isEqualTo("2281");
		assertThat(computerCaptor.getValue().getAttempts()).isZero();
		assertThat(computerCaptor.getValue().getErrorMessage()).isNull();
	}

	@Test
	void aComputerPobKnowsNothingAboutIsRetried() {
		whenPageContains(computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isNull();
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isOne();
		assertThat(computerCaptor.getValue().getErrorMessage()).isNotBlank();
	}

	/**
	 * A municipality outside the mapping is one we cannot pick a SysMan installation from, so it counts as an attempt
	 * rather than being written on the row and failing later.
	 */
	@Test
	void aMunicipalityWeCannotRouteOnIsRetried() {
		whenPageContains(computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(configurationItem("Timrå"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isNull();
		assertThat(computerCaptor.getValue().getAttempts()).isOne();
	}

	/**
	 * A call POB turns down, or that breaks us, may be about one computer's data, and the answer does not say which. Each
	 * serial number is asked about again on its own, so only the computer at fault pays. 404 is what POB's IIS answers a
	 * query string that is too long.
	 */
	@ParameterizedTest
	@MethodSource("failuresOfTheCall")
	void aCallPobTurnsDownIsAskedAgainOneSerialNumberAtATime(final RuntimeException failure) {
		whenPageContains(computer("J000ABC", PENDING, 1), computer("J001ABC", PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC"))).thenThrow(failure);
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC"))).thenReturn(List.of(configurationItem("J000ABC", "2281")));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J001ABC"))).thenThrow(failure);

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getAssetMunicipalityId, EndOfLeaseComputerEntity::getAttempts)
			.containsExactly(
				tuple("J000ABC", "2281", 0),
				tuple("J001ABC", null, 1));
		assertThat(computerCaptor.getAllValues().getLast().getErrorMessage()).isNotBlank();
	}

	private static Stream<RuntimeException> failuresOfTheCall() {
		return Stream.of(
			new ClientProblem(BAD_REQUEST, "Malformed filter"),
			new ClientProblem(NOT_FOUND, "Query string too long"),
			new IllegalStateException("the payload broke us on the way through"));
	}

	/**
	 * Two rows of one serial number are one serial number to POB, so there is nothing left to split them into.
	 */
	@Test
	void aTurnedDownCallOfOneSerialNumberIsChargedRatherThanSplit() {
		whenPageContains(computer(PENDING, 0), computer(PENDING, 2));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new ClientProblem(BAD_REQUEST, "Malformed filter"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(pobIntegrationMock).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER));
		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues()).extracting(EndOfLeaseComputerEntity::getAttempts).containsExactly(1, 3);
		verifyNoMoreInteractions(pobIntegrationMock);
	}

	/**
	 * Split, the rows that share a serial number stay in one call.
	 */
	@Test
	void rowsThatShareASerialNumberStayTogetherWhenACallIsSplit() {
		endOfLeaseLookupWorker = new EndOfLeaseLookupWorker(
			endOfLeaseComputerRepositoryMock, pobIntegrationMock, new POBProperties(1, 2, POB_KEY),
			new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS, BACKOFF),
			dept44HealthUtilityMock, PAGE_SIZE, 3, JOB_NAME);
		whenPageContains(computer("J000ABC", PENDING, 0), computer("J001ABC", PENDING, 0), computer("J000ABC", PENDING, 2));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC"))).thenThrow(new ClientProblem(BAD_REQUEST, "Malformed filter"));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC"))).thenReturn(List.of(configurationItem("J000ABC", "2281")));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J001ABC"))).thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(pobIntegrationMock).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC"));
		verify(endOfLeaseComputerRepositoryMock, times(3)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getAssetMunicipalityId)
			.containsExactly(
				tuple("J000ABC", "2281"),
				tuple("J000ABC", "2281"),
				tuple("J001ABC", null));
	}

	/**
	 * The single calls count towards giving up on the page like any other, or a POB that dies halfway through a split
	 * would be asked about fifty serial numbers, each one waiting out its timeout.
	 */
	@Test
	void theRunStopsWhenPobFailsWhileAskingOneSerialNumberAtATime() {
		whenPageContains(computers(4));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any()))
			.thenThrow(new ClientProblem(BAD_REQUEST, "Malformed filter"))
			.thenThrow(new ClientProblem(BAD_REQUEST, "Malformed filter"))
			.thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		// Two calls turned down, then J000ABC, J001ABC and J002ABC on their own. J003ABC is left for the next run.
		verify(pobIntegrationMock, times(5)).getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any());
		verify(pobIntegrationMock, never()).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J003ABC"));
		verify(endOfLeaseComputerRepositoryMock, times(3)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues()).extracting(EndOfLeaseComputerEntity::getAttempts).containsOnly(0);
	}

	@ParameterizedTest
	@ValueSource(ints = {
		0, -1
	})
	void aCallSizeBelowOneIsRefusedAtStartup(final int serialNumbersPerCall) {
		final var failureRecorder = new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS, BACKOFF);
		final var pobProperties = new POBProperties(1, 2, POB_KEY);

		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> new EndOfLeaseLookupWorker(endOfLeaseComputerRepositoryMock, pobIntegrationMock, pobProperties, failureRecorder,
				dept44HealthUtilityMock, PAGE_SIZE, serialNumbersPerCall, JOB_NAME))
			.withMessageContaining("scheduler.end-of-lease.lookup.serial-numbers-per-call");
	}

	/**
	 * An unreachable POB says nothing about the computers, so it costs no attempt. The reason is still written on every
	 * row of the call.
	 */
	@Test
	void anUnreachablePobDoesNotCostAnAttempt() {
		whenPageContains(computer("J000ABC", PENDING, 1), computer("J001ABC", PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC"))).thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getStatus, EndOfLeaseComputerEntity::getAttempts)
			.containsExactly(
				tuple("J000ABC", PENDING, 1),
				tuple("J001ABC", PENDING, 0));
		assertThat(computerCaptor.getAllValues()).extracting(EndOfLeaseComputerEntity::getErrorMessage).allSatisfy(errorMessage -> assertThat(errorMessage).contains("POB could not be reached"));
	}

	/**
	 * An outage never leaves a computer FAILED, however long it lasts. Only what the computer itself is the cause of
	 * does.
	 */
	@Test
	void anUnreachablePobNeverLeavesTheComputerFailed() {
		whenPageContains(computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS - 1);
	}

	/**
	 * A call that breaks us has to cost attempts too, or it keeps its place at the front of every page.
	 */
	@Test
	void aDefectInTheCallCountsAsAnAttempt() {
		whenPageContains(computer(PENDING, 1));
		// Anything the two named branches above did not foresee. Deliberately not a ClassCastException off the
		// municipality any more: EndOfLeaseMapper converts that value instead of casting it, so the exception this test
		// used to throw can no longer happen, and a test that stubs an impossible failure proves nothing.
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new IllegalStateException("the payload broke us on the way through"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(2);
	}

	/**
	 * The key this job carries is its own, and POB turning it down is no fact about the computers the call happened to
	 * be about. Counted, a rotated key would drain the whole queue into FAILED before anyone noticed.
	 */
	@Test
	void aRejectedPobKeyCostsNoAttempt() {
		whenPageContains(computer(PENDING, 2));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(2);
	}

	/**
	 * The run stops on a POB that is not answering, so there is nothing behind this call left to protect from it and no
	 * reason to hold its computers back. Left free, they are the first tried on the next run, which makes one failing
	 * call an hour the whole price of an outage instead of a page held back for six.
	 */
	@Test
	void aRejectedPobKeyDoesNotHoldTheComputerBack() {
		whenPageContains(computer(PENDING, 2));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getRetryAfter()).isNull();
	}

	/**
	 * POB is the same for every call, so after three failures in a row the rest is left untouched for the next run.
	 */
	@Test
	void theRunStopsAfterThreeFailuresInARow() {
		whenPageContains(computers(10));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any())).thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		// Three calls of two computers each. The last four are never touched.
		verify(pobIntegrationMock, times(3)).getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any());
		verify(endOfLeaseComputerRepositoryMock, times(6)).save(any());
	}

	/**
	 * A POB that answers some calls and not others is a different thing from one that is down. Counted as a total
	 * rather than as a run of failures, a flapping POB would stop the queue on the third bad call of the day.
	 */
	@Test
	void aCallThatGoesThroughResetsTheCount() {
		whenPageContains(computers(12));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any()))
			.thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"))
			.thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"))
			.thenReturn(configurationItem("2281"))
			.thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"))
			.thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"))
			.thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		// The third call resets the count, so all six calls are made.
		verify(pobIntegrationMock, times(6)).getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any());
	}

	/**
	 * A call rejected because of its computers does not stop the run. Only POB failing does.
	 */
	@Test
	void aFailureOfTheComputersThemselvesNeverStopsTheRun() {
		whenPageContains(computers(10));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any())).thenThrow(new ClientProblem(BAD_REQUEST, "Malformed filter"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		// Five calls of two, then each of the ten on its own.
		verify(pobIntegrationMock, times(15)).getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any());
		verify(endOfLeaseComputerRepositoryMock, times(10)).save(any());
	}

	/**
	 * The same for a key POB knows but will not let this far, and it never leaves a computer FAILED.
	 */
	@Test
	void aForbiddenPobKeyNeverLeavesTheComputerFailed() {
		whenPageContains(computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new ClientProblem(FORBIDDEN, "Forbidden"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS - 1);
	}

	@Test
	void theLastAttemptLeavesTheComputerFailed() {
		whenPageContains(computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(FAILED);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS);
	}

	/**
	 * The run swallows an outage so that the computers keep their attempts, which leaves the scheduler aspect with
	 * nothing to report. Saying so here is what puts it on the health endpoint on the run that hits it.
	 */
	@Test
	void anUnreachablePobIsReportedOnTheHealthEndpoint() {
		whenPageContains(computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("POB could not be reached"));
	}

	/**
	 * Giving up on a computer means nobody but a person can take it further, which is worth the same visibility as an
	 * outage. An ordinary attempt that still has budget left is not, or the indicator would never be green.
	 */
	@Test
	void givingUpOnAComputerIsReportedButAnOrdinaryAttemptIsNot() {
		whenPageContains(computer(PENDING, 0), computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("Gave up on serial number"));
		verifyNoMoreInteractions(dept44HealthUtilityMock);
	}

	/**
	 * Only this job needs a POB key of its own, so a missing one must not stop the service from starting. It is caught
	 * here instead, where it is one unhealthy job rather than an API that will not come up.
	 */
	@Test
	void aMissingPobKeyStopsTheRunAndSaysSo() {
		endOfLeaseLookupWorker = new EndOfLeaseLookupWorker(
			endOfLeaseComputerRepositoryMock, pobIntegrationMock, new POBProperties(1, 2, " "),
			new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS, BACKOFF),
			dept44HealthUtilityMock, PAGE_SIZE, SERIAL_NUMBERS_PER_CALL, JOB_NAME);

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("integration.pob.key"));
		verifyNoInteractions(endOfLeaseComputerRepositoryMock, pobIntegrationMock);
	}

	@Test
	void anEmptyPageCallsNobody() {
		when(endOfLeaseComputerRepositoryMock.findAwaitingLookup(eq(PENDING), any(), eq(PageRequest.ofSize(PAGE_SIZE))))
			.thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).findAwaitingLookup(eq(PENDING), any(), eq(PageRequest.ofSize(PAGE_SIZE)));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoInteractions(pobIntegrationMock);
	}

	/**
	 * Every computer is saved as it goes rather than the page being saved at the end, so that a run cut short by its
	 * lock keeps what it did get through.
	 */
	@Test
	void everyComputerOnThePageIsSavedOnItsOwn() {
		whenPageContains(computer("J000ABC", PENDING, 0), computer("J001ABC", PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC")))
			.thenReturn(List.of(configurationItem("J000ABC", "2260"), configurationItem("J001ABC", "2260")));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(any(EndOfLeaseComputerEntity.class));
	}

	/**
	 * No call holds more serial numbers than configured, and the last one takes the rest.
	 */
	@Test
	void thePageIsAskedAboutInCallsOfTheConfiguredSize() {
		whenPageContains(computers(5));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(eq(POB_KEY), any())).thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		final var inOrder = inOrder(pobIntegrationMock);
		inOrder.verify(pobIntegrationMock).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC"));
		inOrder.verify(pobIntegrationMock).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J002ABC", "J003ABC"));
		inOrder.verify(pobIntegrationMock).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J004ABC"));
		verifyNoMoreInteractions(pobIntegrationMock);
	}

	/**
	 * POB answers in its own order. Each computer must get its own municipality, or it goes through the wrong SysMan
	 * installation.
	 */
	@Test
	void eachComputerIsGivenTheMunicipalityOfItsOwnSerialNumber() {
		whenPageContains(computer("J000ABC", PENDING, 0), computer("J001ABC", PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC")))
			.thenReturn(List.of(configurationItem("J001ABC", "Ånge"), configurationItem("J000ABC", "Sundsvall")));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getAssetMunicipalityId)
			.containsExactly(
				tuple("J000ABC", "2281"),
				tuple("J001ABC", "2260"));
	}

	/**
	 * A computer missing from the answer is retried, and the others of the call go through.
	 */
	@Test
	void aComputerMissingFromTheAnswerIsRetriedWhileTheRestGoThrough() {
		whenPageContains(computer("J000ABC", PENDING, 0), computer("J001ABC", PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of("J000ABC", "J001ABC")))
			.thenReturn(List.of(configurationItem("J000ABC", "2281")));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getAssetMunicipalityId, EndOfLeaseComputerEntity::getAttempts)
			.containsExactly(
				tuple("J000ABC", "2281", 0),
				tuple("J001ABC", null, 1));
	}

	/**
	 * A computer can sit in two batches, with both rows in one call. POB is asked once, and both rows get the answer.
	 */
	@Test
	void aSerialNumberOnTwoRowsIsAskedAboutOnce() {
		whenPageContains(computer(PENDING, 0), computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER))).thenReturn(configurationItem("2281"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(pobIntegrationMock).getConfigurationItemsBySerialNumbersForEndOfLease(POB_KEY, List.of(SERIAL_NUMBER));
		verify(endOfLeaseComputerRepositoryMock, times(2)).save(computerCaptor.capture());
		assertThat(computerCaptor.getAllValues()).extracting(EndOfLeaseComputerEntity::getAssetMunicipalityId).containsExactly("2281", "2281");
	}

	private void whenPageContains(final EndOfLeaseComputerEntity... computers) {
		when(endOfLeaseComputerRepositoryMock.findAwaitingLookup(eq(PENDING), any(), eq(PageRequest.ofSize(PAGE_SIZE))))
			.thenReturn(List.of(computers));
	}

	private static EndOfLeaseComputerEntity computer(final EndOfLeaseStatus status, final int attempts) {
		return computer(SERIAL_NUMBER, status, attempts);
	}

	private static EndOfLeaseComputerEntity computer(final String serialNumber, final EndOfLeaseStatus status, final int attempts) {
		return EndOfLeaseComputerEntity.create()
			.withSerialNumber(serialNumber)
			.withAssetTag("AB12345")
			.withStatus(status)
			.withAttempts(attempts);
	}

	private static EndOfLeaseComputerEntity[] computers(final int count) {
		return range(0, count)
			.mapToObj(index -> computer("J%03dABC".formatted(index), PENDING, 0))
			.toArray(EndOfLeaseComputerEntity[]::new);
	}

	private static List<PobPayload> configurationItem(final String municipality) {
		return List.of(configurationItem(SERIAL_NUMBER, municipality));
	}

	private static PobPayload configurationItem(final String serialNumber, final String municipality) {
		final var dataMap = new HashMap<String, Object>();
		dataMap.put("SerialNumber", serialNumber);
		dataMap.put("Virtual.CIKommun", municipality);

		return new PobPayload().type("ConfigurationItem").data(dataMap);
	}
}
