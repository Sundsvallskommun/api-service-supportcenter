package se.sundsvall.supportcenter.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerParameters;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsParameters;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatusCount;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
	private static final String BATCH_ID = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c";

	@Mock
	private EndOfLeaseBatchRepository endOfLeaseBatchRepositoryMock;

	@Mock
	private EndOfLeaseComputerRepository endOfLeaseComputerRepositoryMock;

	@InjectMocks
	private EndOfLeaseService endOfLeaseService;

	@Test
	void registerBatchStoresBatch() {
		final var createEndOfLeaseBatchRequest = createRequest(1);
		final var entityCaptor = ArgumentCaptor.forClass(EndOfLeaseBatchEntity.class);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID)).thenReturn(empty());
		when(endOfLeaseBatchRepositoryMock.save(any(EndOfLeaseBatchEntity.class))).thenReturn(EndOfLeaseBatchEntity.create().withId(BATCH_ID));

		final var batchId = endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(batchId).isEqualTo(BATCH_ID);

		verify(endOfLeaseBatchRepositoryMock).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).save(entityCaptor.capture());
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);

		final var storedEntity = entityCaptor.getValue();
		assertThat(storedEntity.getExternalBatchId()).isEqualTo(EXTERNAL_BATCH_ID);
		assertThat(storedEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(storedEntity.getComputers()).hasSize(1);
		assertThat(storedEntity.getComputers().getFirst().getStatus()).isEqualTo(PENDING);
	}

	@Test
	void registerBatchThrowsConflictWhenExternalBatchIdIsAlreadyRegistered() {
		final var createEndOfLeaseBatchRequest = createRequest(2);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID)).thenReturn(of(EndOfLeaseBatchEntity.create().withId(BATCH_ID)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(CONFLICT);
				assertThat(problem.getDetail()).isEqualTo("A batch with external id '" + EXTERNAL_BATCH_ID + "' is already registered as '" + BATCH_ID + "'");
			});

		verify(endOfLeaseBatchRepositoryMock).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void registerBatchLooksUpTheBatchOfTheSendersOwnMunicipality() {
		final var createEndOfLeaseBatchRequest = createRequest(1);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId("2260", EXTERNAL_BATCH_ID)).thenReturn(empty());
		when(endOfLeaseBatchRepositoryMock.save(any(EndOfLeaseBatchEntity.class))).thenReturn(EndOfLeaseBatchEntity.create().withId(BATCH_ID));

		endOfLeaseService.registerBatch("2260", createEndOfLeaseBatchRequest);

		verify(endOfLeaseBatchRepositoryMock).findByMunicipalityIdAndExternalBatchId("2260", EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).save(any(EndOfLeaseBatchEntity.class));
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void registerBatchThrowsConflictWhenTwoResendsRaceForTheSameExternalBatchId() {
		final var createEndOfLeaseBatchRequest = createRequest(1);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID))
			.thenReturn(empty())
			.thenReturn(of(EndOfLeaseBatchEntity.create().withId(BATCH_ID)));
		when(endOfLeaseBatchRepositoryMock.save(any(EndOfLeaseBatchEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate entry"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(CONFLICT);
				assertThat(problem.getDetail()).isEqualTo("A batch with external id '" + EXTERNAL_BATCH_ID + "' is already registered as '" + BATCH_ID + "'");
			});

		verify(endOfLeaseBatchRepositoryMock, times(2)).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).save(any(EndOfLeaseBatchEntity.class));
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void registerBatchRethrowsViolationThatIsNotADuplicateBatch() {
		final var createEndOfLeaseBatchRequest = createRequest(1);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID)).thenReturn(empty());
		when(endOfLeaseBatchRepositoryMock.save(any(EndOfLeaseBatchEntity.class))).thenThrow(new DataIntegrityViolationException("column too long"));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest))
			.withMessage("column too long");

		verify(endOfLeaseBatchRepositoryMock, times(2)).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).save(any(EndOfLeaseBatchEntity.class));
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void registerBatchKeepsTheViolationWhenTheRecoveringLookupAlsoFails() {
		final var createEndOfLeaseBatchRequest = createRequest(1);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID))
			.thenReturn(empty())
			.thenThrow(new DataAccessResourceFailureException("connection lost"));
		when(endOfLeaseBatchRepositoryMock.save(any(EndOfLeaseBatchEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate entry"));

		assertThatExceptionOfType(DataAccessResourceFailureException.class)
			.isThrownBy(() -> endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest))
			.withMessage("connection lost")
			.satisfies(thrown -> assertThat(thrown.getSuppressed())
				.extracting(Throwable::getMessage)
				.containsExactly("duplicate entry"));

		verify(endOfLeaseBatchRepositoryMock, times(2)).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).save(any(EndOfLeaseBatchEntity.class));
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	/**
	 * The attempts have to go back with the state, or the first run that reaches the computer gives up on it again on
	 * the attempt it had left.
	 */
	@Test
	void retryComputersGivenUpOnResetsTheAttemptsAndTheReason() {
		final var computer = computer("J123ABC", 5, "POB answered the serial number with no configuration item");

		when(endOfLeaseComputerRepositoryMock.findByStatusAndBatchMunicipalityId(FAILED, MUNICIPALITY_ID)).thenReturn(List.of(computer));

		final var reset = endOfLeaseService.retryComputersGivenUpOn(MUNICIPALITY_ID, RetryEndOfLeaseComputersRequest.create());

		assertThat(reset).isOne();
		verify(endOfLeaseComputerRepositoryMock).findByStatusAndBatchMunicipalityId(FAILED, MUNICIPALITY_ID);
		verify(endOfLeaseComputerRepositoryMock).saveAll(List.of(computer));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock, endOfLeaseBatchRepositoryMock);

		assertThat(computer.getStatus()).isEqualTo(PENDING);
		assertThat(computer.getAttempts()).isZero();
		assertThat(computer.getErrorMessage()).isNull();
	}

	/**
	 * A computer that never got as far as a lookup has no POB municipality, and one that did keeps it, so the retry
	 * puts each of them back in the run that was waiting for it.
	 */
	@Test
	void retryComputersGivenUpOnLeavesThePobMunicipalityAlone() {
		final var neverLookedUp = computer("J123ABC", 5, "unknown to POB");
		final var lookedUp = computer("K456DEF", 5, "SysMan did not recognize the computer name").withAssetMunicipalityId("2281");

		when(endOfLeaseComputerRepositoryMock.findByStatusAndBatchMunicipalityId(FAILED, MUNICIPALITY_ID)).thenReturn(List.of(neverLookedUp, lookedUp));

		endOfLeaseService.retryComputersGivenUpOn(MUNICIPALITY_ID, RetryEndOfLeaseComputersRequest.create());

		assertThat(neverLookedUp.getAssetMunicipalityId()).isNull();
		assertThat(lookedUp.getAssetMunicipalityId()).isEqualTo("2281");
	}

	@Test
	void retryComputersGivenUpOnTakesOnlyTheOnesNamed() {
		final var serialNumbers = List.of("J123ABC");
		final var computer = computer("J123ABC", 5, "unknown to POB");

		when(endOfLeaseComputerRepositoryMock.findByStatusAndBatchMunicipalityIdAndSerialNumberIn(FAILED, MUNICIPALITY_ID, serialNumbers))
			.thenReturn(List.of(computer));

		final var reset = endOfLeaseService.retryComputersGivenUpOn(MUNICIPALITY_ID, RetryEndOfLeaseComputersRequest.create().withSerialNumbers(serialNumbers));

		assertThat(reset).isOne();
		verify(endOfLeaseComputerRepositoryMock).findByStatusAndBatchMunicipalityIdAndSerialNumberIn(FAILED, MUNICIPALITY_ID, serialNumbers);
		verify(endOfLeaseComputerRepositoryMock).saveAll(any());
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock, endOfLeaseBatchRepositoryMock);
	}

	/**
	 * An empty list is the same ask as no list at all, and not a request to take nothing.
	 */
	@Test
	void retryComputersGivenUpOnTreatsAnEmptyListAsEveryone() {
		when(endOfLeaseComputerRepositoryMock.findByStatusAndBatchMunicipalityId(FAILED, MUNICIPALITY_ID)).thenReturn(emptyList());

		final var reset = endOfLeaseService.retryComputersGivenUpOn(MUNICIPALITY_ID, RetryEndOfLeaseComputersRequest.create().withSerialNumbers(emptyList()));

		assertThat(reset).isZero();
		verify(endOfLeaseComputerRepositoryMock).findByStatusAndBatchMunicipalityId(FAILED, MUNICIPALITY_ID);
	}

	private static EndOfLeaseComputerEntity computer(final String serialNumber, final int attempts, final String errorMessage) {
		return EndOfLeaseComputerEntity.create()
			.withSerialNumber(serialNumber)
			.withAssetTag("AB12345")
			.withStatus(FAILED)
			.withAttempts(attempts)
			.withErrorMessage(errorMessage);
	}

	private static CreateEndOfLeaseBatchRequest createRequest(final int computers) {
		return CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(IntStream.range(0, computers)
				.mapToObj(index -> EndOfLeaseComputer.create()
					.withSerialNumber("J123AB" + index)
					.withAssetTag("WB1660" + index)
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30)))
				.toList());
	}

	@Test
	void getStatisticsAnswersWithWhatTheBatchesAddUpTo() {
		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");

		final var from = LocalDate.of(2026, 8, 18);
		final var to = LocalDate.of(2026, 9, 18);

		when(endOfLeaseBatchRepositoryMock.findInWindow(eq(MUNICIPALITY_ID), any(), any(), any()))
			.thenReturn(new PageImpl<>(List.of(batchEntity(created)), PageRequest.of(0, 100), 1));
		when(endOfLeaseComputerRepositoryMock.countByStatusGroupedByBatch(List.of(BATCH_ID))).thenReturn(List.of(
			new EndOfLeaseBatchStatusCount(BATCH_ID, EXTERNAL_BATCH_ID, created, SENT, 961),
			new EndOfLeaseBatchStatusCount(BATCH_ID, EXTERNAL_BATCH_ID, created, FAILED, 3),
			new EndOfLeaseBatchStatusCount(BATCH_ID, EXTERNAL_BATCH_ID, created, EXCLUDED, 18)));
		when(endOfLeaseComputerRepositoryMock.countByStatusInWindow(eq(MUNICIPALITY_ID), any(), any())).thenReturn(List.of(
			new EndOfLeaseStatusCount(SENT, 961),
			new EndOfLeaseStatusCount(FAILED, 3),
			new EndOfLeaseStatusCount(EXCLUDED, 18)));

		final var statistics = endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(from, to));

		assertThat(statistics.getMetadata().getTotalRecords()).isEqualTo(1);
		assertThat(statistics.getCounts().getTotal()).isEqualTo(982);
		assertThat(statistics.getCounts().getSent()).isEqualTo(961);
		assertThat(statistics.getCounts().getFailed()).isEqualTo(3);
		assertThat(statistics.getCounts().getExcluded()).isEqualTo(18);
		assertThat(statistics.getBatches()).singleElement().satisfies(batch -> {
			assertThat(batch.getId()).isEqualTo(BATCH_ID);
			assertThat(batch.getExternalBatchId()).isEqualTo(EXTERNAL_BATCH_ID);
			assertThat(batch.getCreated()).isEqualTo(created);
			assertThat(batch.getCounts().getTotal()).isEqualTo(982);
		});

		assertThat(statistics.getFrom()).isEqualTo(from);
		assertThat(statistics.getTo()).isEqualTo(to);

		verify(endOfLeaseBatchRepositoryMock).findInWindow(MUNICIPALITY_ID, startOfDay(from), startOfDay(to.plusDays(1)), PageRequest.of(0, 100));
		verify(endOfLeaseComputerRepositoryMock).countByStatusGroupedByBatch(List.of(BATCH_ID));
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(MUNICIPALITY_ID, startOfDay(from), startOfDay(to.plusDays(1)));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	/**
	 * The page number is one based on the way in and zero based to Spring Data, and a page past the last one is a page
	 * with no batches on it. There is nothing to count those against, so the grouped count is not asked at all.
	 */
	@Test
	void getStatisticsSkipsTheGroupedCountForAPageWithNoBatchesOnIt() {
		when(endOfLeaseBatchRepositoryMock.findInWindow(eq(MUNICIPALITY_ID), any(), any(), any()))
			.thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 25), 2));
		when(endOfLeaseComputerRepositoryMock.countByStatusInWindow(eq(MUNICIPALITY_ID), any(), any())).thenReturn(emptyList());

		final var statistics = endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(null, null).withPage(4).withLimit(25));

		assertThat(statistics.getBatches()).isEmpty();
		assertThat(statistics.getCounts().getTotal()).isZero();

		verify(endOfLeaseBatchRepositoryMock).findInWindow(eq(MUNICIPALITY_ID), any(), any(), eq(PageRequest.of(3, 25)));
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(eq(MUNICIPALITY_ID), any(), any());
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void getStatisticsForAMunicipalityThatHasRegisteredNothing() {
		when(endOfLeaseBatchRepositoryMock.findInWindow(eq(MUNICIPALITY_ID), any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
		when(endOfLeaseComputerRepositoryMock.countByStatusInWindow(eq(MUNICIPALITY_ID), any(), any())).thenReturn(emptyList());

		final var statistics = endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(null, null));

		assertThat(statistics.getMetadata().getTotalRecords()).isZero();
		assertThat(statistics.getBatches()).isEmpty();
		assertThat(statistics.getCounts().getTotal()).isZero();

		verify(endOfLeaseBatchRepositoryMock).findInWindow(eq(MUNICIPALITY_ID), any(), any(), any());
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(eq(MUNICIPALITY_ID), any(), any());
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	/**
	 * Nothing asked for is the month up to today. A wider window no longer decides how large the answer is, but it does
	 * decide how many rows the two counts are taken over, so there is still a default rather than everything ever stored.
	 */
	@Test
	void getStatisticsWithoutAWindowCountsTheMonthUpToToday() {
		final var today = LocalDate.now(ZoneId.systemDefault());

		givenAnEmptyWindow();

		final var statistics = endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(null, null));

		assertThat(statistics.getFrom()).isEqualTo(today.minusMonths(1));
		assertThat(statistics.getTo()).isEqualTo(today);

		verify(endOfLeaseBatchRepositoryMock).findInWindow(MUNICIPALITY_ID, startOfDay(today.minusMonths(1)), startOfDay(today.plusDays(1)), PageRequest.of(0, 100));
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(MUNICIPALITY_ID, startOfDay(today.minusMonths(1)), startOfDay(today.plusDays(1)));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	/**
	 * The last day is settled first and the first day is derived from it, so naming only the first day is counted up to
	 * today rather than a month on from what was named.
	 */
	@Test
	void getStatisticsWithOnlyAFirstDayCountsUpToToday() {
		final var today = LocalDate.now(ZoneId.systemDefault());
		final var from = today.minusYears(1);

		givenAnEmptyWindow();

		final var statistics = endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(from, null));

		assertThat(statistics.getFrom()).isEqualTo(from);
		assertThat(statistics.getTo()).isEqualTo(today);

		verify(endOfLeaseBatchRepositoryMock).findInWindow(MUNICIPALITY_ID, startOfDay(from), startOfDay(today.plusDays(1)), PageRequest.of(0, 100));
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(MUNICIPALITY_ID, startOfDay(from), startOfDay(today.plusDays(1)));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void getStatisticsWithOnlyALastDayCountsTheMonthBeforeIt() {
		final var to = LocalDate.of(2026, 6, 30);

		givenAnEmptyWindow();

		final var statistics = endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(null, to));

		assertThat(statistics.getFrom()).isEqualTo(LocalDate.of(2026, 5, 30));
		assertThat(statistics.getTo()).isEqualTo(to);

		verify(endOfLeaseBatchRepositoryMock).findInWindow(MUNICIPALITY_ID, startOfDay(LocalDate.of(2026, 5, 30)), startOfDay(LocalDate.of(2026, 7, 1)), PageRequest.of(0, 100));
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(MUNICIPALITY_ID, startOfDay(LocalDate.of(2026, 5, 30)), startOfDay(LocalDate.of(2026, 7, 1)));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	/**
	 * The last day is counted in full, so a batch registered late on it is still in the window.
	 */
	@Test
	void getStatisticsCountsTheLastDayInFull() {
		final var day = LocalDate.of(2026, 6, 30);

		givenAnEmptyWindow();

		endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(day, day));

		verify(endOfLeaseBatchRepositoryMock).findInWindow(MUNICIPALITY_ID, startOfDay(day), startOfDay(day.plusDays(1)), PageRequest.of(0, 100));
		verify(endOfLeaseComputerRepositoryMock).countByStatusInWindow(MUNICIPALITY_ID, startOfDay(day), startOfDay(day.plusDays(1)));
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void getStatisticsWithAWindowThatEndsBeforeItStarts() {
		final var from = LocalDate.of(2026, 9, 18);
		final var to = LocalDate.of(2026, 8, 18);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> endOfLeaseService.getStatistics(MUNICIPALITY_ID, parameters(from, to)))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("The window ends before it starts: from '2026-09-18' is later than to '2026-08-18'");
			});

		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	private void givenAnEmptyWindow() {
		when(endOfLeaseBatchRepositoryMock.findInWindow(eq(MUNICIPALITY_ID), any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
		when(endOfLeaseComputerRepositoryMock.countByStatusInWindow(eq(MUNICIPALITY_ID), any(), any())).thenReturn(emptyList());
	}

	private static EndOfLeaseStatisticsParameters parameters(final LocalDate from, final LocalDate to) {
		return EndOfLeaseStatisticsParameters.create().withFrom(from).withTo(to);
	}

	private static EndOfLeaseBatchEntity batchEntity(final OffsetDateTime created) {
		return EndOfLeaseBatchEntity.create()
			.withId(BATCH_ID)
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withCreated(created);
	}

	private static OffsetDateTime startOfDay(final LocalDate day) {
		return day.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
	}

	@Test
	void getBatchStatusAnswersWithTheBatchAndThePageOfComputers() {
		final var batch = batchEntity(null);

		when(endOfLeaseBatchRepositoryMock.findByIdAndMunicipalityId(BATCH_ID, MUNICIPALITY_ID)).thenReturn(of(batch));
		when(endOfLeaseComputerRepositoryMock.findByBatchId(eq(BATCH_ID), any(), any())).thenReturn(new PageImpl<>(List.of(
			EndOfLeaseComputerEntity.create().withSerialNumber("J123ABC").withAssetTag("AB12345").withStatus(FAILED).withAttempts(5).withErrorMessage("POB is unwell"),
			EndOfLeaseComputerEntity.create().withSerialNumber("K456DEF").withAssetTag("AB67890").withStatus(SENT).withAttempts(1)), PageRequest.of(0, 100), 2));
		when(endOfLeaseComputerRepositoryMock.countByStatusGroupedByBatch(List.of(BATCH_ID))).thenReturn(List.of(
			new EndOfLeaseBatchStatusCount(BATCH_ID, EXTERNAL_BATCH_ID, null, FAILED, 1),
			new EndOfLeaseBatchStatusCount(BATCH_ID, EXTERNAL_BATCH_ID, null, SENT, 1)));

		final var batchStatus = endOfLeaseService.getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create());

		assertThat(batchStatus.getId()).isEqualTo(BATCH_ID);
		assertThat(batchStatus.getCounts().getTotal()).isEqualTo(2);
		assertThat(batchStatus.getCounts().getFailed()).isEqualTo(1);
		assertThat(batchStatus.getCounts().getSent()).isEqualTo(1);
		assertThat(batchStatus.getMetadata().getTotalRecords()).isEqualTo(2);
		assertThat(batchStatus.getComputers())
			.extracting("serialNumber", "status", "errorMessage")
			.containsExactly(
				tuple("J123ABC", "FAILED", "POB is unwell"),
				tuple("K456DEF", "SENT", null));

		verify(endOfLeaseBatchRepositoryMock).findByIdAndMunicipalityId(BATCH_ID, MUNICIPALITY_ID);
		verify(endOfLeaseComputerRepositoryMock).findByBatchId(BATCH_ID, EnumSet.allOf(EndOfLeaseStatus.class), PageRequest.of(0, 100));
		verify(endOfLeaseComputerRepositoryMock).countByStatusGroupedByBatch(List.of(BATCH_ID));
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
	}

	/**
	 * The municipality in the path is the sender's, so a batch of another sender is answered the same way as one that
	 * does not exist at all.
	 */
	@Test
	void getBatchStatusForABatchTheMunicipalityDoesNotHave() {
		when(endOfLeaseBatchRepositoryMock.findByIdAndMunicipalityId(BATCH_ID, MUNICIPALITY_ID)).thenReturn(empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> endOfLeaseService.getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create()))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).isEqualTo("No batch with id '%s' is registered for municipality '%s'".formatted(BATCH_ID, MUNICIPALITY_ID));
			});

		verify(endOfLeaseBatchRepositoryMock).findByIdAndMunicipalityId(BATCH_ID, MUNICIPALITY_ID);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
	}

	/**
	 * The states asked for are carried into the query rather than applied to the page after it has been read, or a page
	 * of ten would answer with however few of those ten happened to match.
	 */
	@Test
	void getBatchStatusAsksTheQueryForOnlyTheStatesNamed() {
		givenABatchWithNoComputersOnThePage();

		endOfLeaseService.getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create().withStatus(List.of("FAILED", "PENDING")).withPage(2).withLimit(10));

		verify(endOfLeaseComputerRepositoryMock).findByBatchId(BATCH_ID, EnumSet.of(FAILED, PENDING), PageRequest.of(1, 10));
	}

	/**
	 * Nothing asked for is every state. An empty collection would answer the query with nothing, which is not what a
	 * caller who left the parameter out means by it.
	 */
	@ParameterizedTest
	@NullAndEmptySource
	void getBatchStatusAsksForEveryStateWhenNoneWasNamed(final List<String> statuses) {
		givenABatchWithNoComputersOnThePage();

		endOfLeaseService.getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create().withStatus(statuses));

		verify(endOfLeaseComputerRepositoryMock).findByBatchId(BATCH_ID, EnumSet.allOf(EndOfLeaseStatus.class), PageRequest.of(0, 100));
	}

	private void givenABatchWithNoComputersOnThePage() {
		when(endOfLeaseBatchRepositoryMock.findByIdAndMunicipalityId(BATCH_ID, MUNICIPALITY_ID)).thenReturn(of(batchEntity(null)));
		when(endOfLeaseComputerRepositoryMock.findByBatchId(eq(BATCH_ID), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
		when(endOfLeaseComputerRepositoryMock.countByStatusGroupedByBatch(List.of(BATCH_ID))).thenReturn(emptyList());
	}
}
