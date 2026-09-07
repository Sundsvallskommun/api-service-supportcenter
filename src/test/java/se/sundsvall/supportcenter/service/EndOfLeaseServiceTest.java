package se.sundsvall.supportcenter.service;

import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
	private static final String BATCH_ID = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c";

	@Mock
	private EndOfLeaseBatchRepository endOfLeaseBatchRepositoryMock;

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
	void registerBatchReturnsExistingIdWhenExternalBatchIdIsAlreadyRegistered() {
		final var createEndOfLeaseBatchRequest = createRequest(2);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID)).thenReturn(of(EndOfLeaseBatchEntity.create().withId(BATCH_ID)));
		when(endOfLeaseBatchRepositoryMock.countComputers(BATCH_ID)).thenReturn(2L);

		final var batchId = endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(batchId).isEqualTo(BATCH_ID);

		verify(endOfLeaseBatchRepositoryMock).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).countComputers(BATCH_ID);
		verifyNoMoreInteractions(endOfLeaseBatchRepositoryMock);
	}

	@Test
	void registerBatchReturnsExistingIdWhenResendCarriesADifferentNumberOfComputers() {
		final var createEndOfLeaseBatchRequest = createRequest(3);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID)).thenReturn(of(EndOfLeaseBatchEntity.create().withId(BATCH_ID)));
		when(endOfLeaseBatchRepositoryMock.countComputers(BATCH_ID)).thenReturn(1L);

		final var batchId = endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(batchId).isEqualTo(BATCH_ID);

		verify(endOfLeaseBatchRepositoryMock).findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID);
		verify(endOfLeaseBatchRepositoryMock).countComputers(BATCH_ID);
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
	void registerBatchReturnsExistingIdWhenTwoResendsRaceForTheSameExternalBatchId() {
		final var createEndOfLeaseBatchRequest = createRequest(1);

		when(endOfLeaseBatchRepositoryMock.findByMunicipalityIdAndExternalBatchId(MUNICIPALITY_ID, EXTERNAL_BATCH_ID))
			.thenReturn(empty())
			.thenReturn(of(EndOfLeaseBatchEntity.create().withId(BATCH_ID)));
		when(endOfLeaseBatchRepositoryMock.save(any(EndOfLeaseBatchEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate entry"));

		final var batchId = endOfLeaseService.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(batchId).isEqualTo(BATCH_ID);

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
}
