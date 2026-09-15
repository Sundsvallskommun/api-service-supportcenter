package se.sundsvall.supportcenter.service.scheduler;

import generated.client.pob.PobPayload;
import java.util.HashMap;
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
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.integration.pob.configuration.POBProperties;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseLookupWorkerTest {

	private static final String POB_KEY = "pobKey";
	private static final String SERIAL_NUMBER = "J123ABC";
	private static final int PAGE_SIZE = 100;
	private static final int MAXIMUM_ATTEMPTS = 5;
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
			new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS),
			dept44HealthUtilityMock,
			PAGE_SIZE,
			JOB_NAME);
	}

	@Test
	void municipalityIsWrittenOnTheComputer() {
		final var computer = computer(PENDING, 0);
		whenPageContains(computer);
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(configurationItem("2281"));

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
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(configurationItem("Sundsvall"));

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
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(configurationItem("2281"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isEqualTo("2281");
		assertThat(computerCaptor.getValue().getAttempts()).isZero();
		assertThat(computerCaptor.getValue().getErrorMessage()).isNull();
	}

	@Test
	void aComputerPobKnowsNothingAboutIsRetried() {
		whenPageContains(computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(emptyList());

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
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(configurationItem("Timrå"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getAssetMunicipalityId()).isNull();
		assertThat(computerCaptor.getValue().getAttempts()).isOne();
	}

	/**
	 * POB turning the request down may well be about this computer's data, so it has to run the attempts down rather
	 * than be asked about forever.
	 */
	@Test
	void aRequestPobTurnsDownCountsAsAnAttempt() {
		whenPageContains(computer(PENDING, 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ClientProblem(BAD_REQUEST, "Malformed filter"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(2);
		assertThat(computerCaptor.getValue().getErrorMessage()).isNotBlank();
	}

	/**
	 * An unreachable or unwell POB says nothing about this computer, so spending its budget on the outage would give up
	 * on a queue that has nothing wrong with it. The reason is still written on the row, so a queue standing still can
	 * be read off it.
	 */
	@Test
	void anUnreachablePobDoesNotCostAnAttempt() {
		whenPageContains(computer(PENDING, 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isOne();
		assertThat(computerCaptor.getValue().getErrorMessage()).isNotBlank();
	}

	/**
	 * An outage never leaves a computer FAILED, however long it lasts. Only what the computer itself is the cause of
	 * does.
	 */
	@Test
	void anUnreachablePobNeverLeavesTheComputerFailed() {
		whenPageContains(computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS - 1);
	}

	/**
	 * A row whose own data breaks us on the way through has to run its attempts down like any other. Left uncounted it
	 * would keep its place in every page for good, and a hundred of them would stop the queue for everyone behind them.
	 */
	@Test
	void aDefectOnTheRowItselfCountsAsAnAttempt() {
		whenPageContains(computer(PENDING, 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ClassCastException("Integer cannot be cast to String"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(2);
	}

	/**
	 * The key this job carries is its own, and POB turning it down is no fact about the computer the call happened to
	 * be about. Counted, a rotated key would drain the whole queue into FAILED before anyone noticed.
	 */
	@Test
	void aRejectedPobKeyCostsNoAttempt() {
		whenPageContains(computer(PENDING, 2));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ClientProblem(UNAUTHORIZED, "Unauthorized"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(2);
	}

	/**
	 * The same for a key POB knows but will not let this far, and it never leaves a computer FAILED.
	 */
	@Test
	void aForbiddenPobKeyNeverLeavesTheComputerFailed() {
		whenPageContains(computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ClientProblem(FORBIDDEN, "Forbidden"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).save(computerCaptor.capture());
		assertThat(computerCaptor.getValue().getStatus()).isEqualTo(PENDING);
		assertThat(computerCaptor.getValue().getAttempts()).isEqualTo(MAXIMUM_ATTEMPTS - 1);
	}

	@Test
	void theLastAttemptLeavesTheComputerFailed() {
		whenPageContains(computer(PENDING, MAXIMUM_ATTEMPTS - 1));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(emptyList());

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
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenThrow(new ServerProblem(BAD_GATEWAY, "POB is unwell"));

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
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(emptyList());

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
			new EndOfLeaseFailureRecorder(endOfLeaseComputerRepositoryMock, dept44HealthUtilityMock, MAXIMUM_ATTEMPTS),
			dept44HealthUtilityMock, PAGE_SIZE, JOB_NAME);

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq(JOB_NAME), contains("integration.pob.key"));
		verifyNoInteractions(endOfLeaseComputerRepositoryMock, pobIntegrationMock);
	}

	@Test
	void anEmptyPageCallsNobody() {
		when(endOfLeaseComputerRepositoryMock.findByStatusAndAssetMunicipalityIdIsNullOrderByCreated(PENDING, PageRequest.ofSize(PAGE_SIZE)))
			.thenReturn(emptyList());

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock).findByStatusAndAssetMunicipalityIdIsNullOrderByCreated(PENDING, PageRequest.ofSize(PAGE_SIZE));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoInteractions(pobIntegrationMock);
	}

	/**
	 * Every computer is saved as it goes rather than the page being saved at the end, so that a run cut short by its
	 * lock keeps what it did get through.
	 */
	@Test
	void everyComputerOnThePageIsSavedOnItsOwn() {
		whenPageContains(computer(PENDING, 0), computer(PENDING, 0));
		when(pobIntegrationMock.getConfigurationItemsBySerialNumberForEndOfLease(POB_KEY, SERIAL_NUMBER)).thenReturn(configurationItem("2260"));

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verify(endOfLeaseComputerRepositoryMock, times(2)).save(any(EndOfLeaseComputerEntity.class));
	}

	private void whenPageContains(final EndOfLeaseComputerEntity... computers) {
		when(endOfLeaseComputerRepositoryMock.findByStatusAndAssetMunicipalityIdIsNullOrderByCreated(PENDING, PageRequest.ofSize(PAGE_SIZE)))
			.thenReturn(List.of(computers));
	}

	private static EndOfLeaseComputerEntity computer(final EndOfLeaseStatus status, final int attempts) {
		return EndOfLeaseComputerEntity.create()
			.withSerialNumber(SERIAL_NUMBER)
			.withAssetTag("AB12345")
			.withStatus(status)
			.withAttempts(attempts);
	}

	private static List<PobPayload> configurationItem(final String municipality) {
		final var dataMap = new HashMap<String, Object>();
		dataMap.put("SerialNumber", SERIAL_NUMBER);
		dataMap.put("Virtual.CIKommun", municipality);

		return List.of(new PobPayload().type("ConfigurationItem").data(dataMap));
	}
}
