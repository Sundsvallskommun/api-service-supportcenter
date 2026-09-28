package se.sundsvall.supportcenter.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatusResponse;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerCounts;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerParameters;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerStatus;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseBatchResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String BATCH_ID = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c";
	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
	private static final String DATASET_REQUEST_HEADER = "adv-dataset-request";

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createEndOfLeaseBatch() {

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(
				EndOfLeaseComputer.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30)),
				EndOfLeaseComputer.create()
					.withSerialNumber("K456DEF")
					.withAssetTag("PUB16604")
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30))));

		when(endOfLeaseServiceMock.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest)).thenReturn(BATCH_ID);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseBatches", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.header(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/2281/endOfLeaseBatches/" + BATCH_ID)
			.expectHeader().valueEquals(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.expectBody()
			.jsonPath("$.id").isEqualTo(BATCH_ID)
			.jsonPath("$.externalBatchId").isEqualTo(EXTERNAL_BATCH_ID);

		verify(endOfLeaseServiceMock).registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);
	}

	@Test
	void createEndOfLeaseBatchWithoutDatasetRequestHeader() {

		final var createEndOfLeaseBatchRequest = createEndOfLeaseBatchRequest();

		when(endOfLeaseServiceMock.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest)).thenReturn(BATCH_ID);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseBatches", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().doesNotExist(DATASET_REQUEST_HEADER);

		verify(endOfLeaseServiceMock).registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);
	}

	@Test
	void createEndOfLeaseBatchWithEmptyDatasetRequestHeader() {

		final var createEndOfLeaseBatchRequest = createEndOfLeaseBatchRequest();

		when(endOfLeaseServiceMock.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest)).thenReturn(BATCH_ID);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseBatches", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.header(DATASET_REQUEST_HEADER, "")
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().doesNotExist(DATASET_REQUEST_HEADER);

		verify(endOfLeaseServiceMock).registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);
	}

	@ParameterizedTest
	@MethodSource("validExternalBatchIdArguments")
	void createEndOfLeaseBatchWithValidExternalBatchId(final String externalBatchId) {

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(externalBatchId)
			.withComputers(List.of(
				EndOfLeaseComputer.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30))));

		when(endOfLeaseServiceMock.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest)).thenReturn(BATCH_ID);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseBatches", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().jsonPath("$.id").isEqualTo(BATCH_ID);

		verify(endOfLeaseServiceMock).registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);
	}

	private static Stream<String> validExternalBatchIdArguments() {
		return Stream.of("D", "DSET0001234", "D".repeat(36));
	}

	@Test
	void getEndOfLeaseBatch() {

		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");

		when(endOfLeaseServiceMock.getBatchStatus(eq(MUNICIPALITY_ID), eq(BATCH_ID), any())).thenReturn(EndOfLeaseBatchStatusResponse.create()
			.withId(BATCH_ID)
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withCreated(created)
			.withCounts(EndOfLeaseComputerCounts.create().withTotal(2).withSent(1).withFailed(1))
			.withComputers(List.of(
				EndOfLeaseComputerStatus.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withStatus("FAILED")
					.withAttempts(5)
					.withErrorMessage("POB is unwell"),
				EndOfLeaseComputerStatus.create()
					.withSerialNumber("K456DEF")
					.withAssetTag("AB67890")
					.withStatus("SENT")
					.withAttempts(1)))
			.withMetadata(PagingMetaData.create().withPage(1).withLimit(100).withCount(2).withTotalRecords(2).withTotalPages(1)));

		webTestClient.get().uri("/{municipalityId}/endOfLeaseBatches/{batchId}", MUNICIPALITY_ID, BATCH_ID)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.id").isEqualTo(BATCH_ID)
			.jsonPath("$.externalBatchId").isEqualTo(EXTERNAL_BATCH_ID)
			.jsonPath("$.counts.total").isEqualTo(2)
			.jsonPath("$.counts.failed").isEqualTo(1)
			.jsonPath("$.computers[0].serialNumber").isEqualTo("J123ABC")
			.jsonPath("$.computers[0].status").isEqualTo("FAILED")
			.jsonPath("$.computers[0].errorMessage").isEqualTo("POB is unwell")
			.jsonPath("$.computers[1].status").isEqualTo("SENT")
			.jsonPath("$._meta.totalRecords").isEqualTo(2);

		verify(endOfLeaseServiceMock).getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create());
	}

	/**
	 * The counts stay over the whole batch while the list holds what was asked for, which is what lets three failures be
	 * read as three out of nine hundred and eighty two.
	 */
	@Test
	void getEndOfLeaseBatchFilteredOnState() {

		when(endOfLeaseServiceMock.getBatchStatus(eq(MUNICIPALITY_ID), eq(BATCH_ID), any())).thenReturn(EndOfLeaseBatchStatusResponse.create()
			.withId(BATCH_ID)
			.withCounts(EndOfLeaseComputerCounts.create().withTotal(982).withFailed(3))
			.withComputers(List.of(EndOfLeaseComputerStatus.create()
				.withSerialNumber("J123ABC")
				.withStatus("FAILED")
				.withAttempts(5)))
			.withMetadata(PagingMetaData.create().withPage(1).withLimit(100).withCount(1).withTotalRecords(3).withTotalPages(1)));

		webTestClient.get().uri(builder -> builder.path("/{municipalityId}/endOfLeaseBatches/{batchId}").queryParam("status", "FAILED").build(MUNICIPALITY_ID, BATCH_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.counts.total").isEqualTo(982)
			.jsonPath("$.computers.length()").isEqualTo(1)
			.jsonPath("$.computers[0].status").isEqualTo("FAILED")
			.jsonPath("$._meta.totalRecords").isEqualTo(3);

		verify(endOfLeaseServiceMock).getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create().withStatus(List.of("FAILED")));
	}

	@Test
	void getEndOfLeaseBatchFilteredOnSeveralStates() {

		when(endOfLeaseServiceMock.getBatchStatus(eq(MUNICIPALITY_ID), eq(BATCH_ID), any())).thenReturn(EndOfLeaseBatchStatusResponse.create().withId(BATCH_ID));

		webTestClient.get()
			.uri(builder -> builder.path("/{municipalityId}/endOfLeaseBatches/{batchId}").queryParam("status", "PENDING").queryParam("status", "FAILED").build(MUNICIPALITY_ID, BATCH_ID))
			.exchange()
			.expectStatus().isOk();

		verify(endOfLeaseServiceMock).getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create().withStatus(List.of("PENDING", "FAILED")));
	}

	/**
	 * The page and limit ride on the same object as the states, and a call that names neither is the first hundred
	 * computers of the batch rather than all of them.
	 */
	@Test
	void getEndOfLeaseBatchForAPage() {

		when(endOfLeaseServiceMock.getBatchStatus(eq(MUNICIPALITY_ID), eq(BATCH_ID), any())).thenReturn(EndOfLeaseBatchStatusResponse.create().withId(BATCH_ID));

		webTestClient.get()
			.uri(builder -> builder.path("/{municipalityId}/endOfLeaseBatches/{batchId}").queryParam("page", "4").queryParam("limit", "50").build(MUNICIPALITY_ID, BATCH_ID))
			.exchange()
			.expectStatus().isOk();

		verify(endOfLeaseServiceMock).getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create().withPage(4).withLimit(50));
	}

	/**
	 * The states the filter accepts are written out as strings on the parameter, so nothing but a test stops the two
	 * lists from drifting apart the day a state is added to the enum.
	 */
	@ParameterizedTest
	@EnumSource(EndOfLeaseStatus.class)
	void getEndOfLeaseBatchAcceptsEveryStateThereIs(final EndOfLeaseStatus status) {

		when(endOfLeaseServiceMock.getBatchStatus(eq(MUNICIPALITY_ID), eq(BATCH_ID), any())).thenReturn(EndOfLeaseBatchStatusResponse.create().withId(BATCH_ID));

		webTestClient.get().uri(builder -> builder.path("/{municipalityId}/endOfLeaseBatches/{batchId}").queryParam("status", status.name()).build(MUNICIPALITY_ID, BATCH_ID))
			.exchange()
			.expectStatus().isOk();

		verify(endOfLeaseServiceMock).getBatchStatus(MUNICIPALITY_ID, BATCH_ID, EndOfLeaseComputerParameters.create().withStatus(List.of(status.name())));
	}

	private static CreateEndOfLeaseBatchRequest createEndOfLeaseBatchRequest() {
		return CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(
				EndOfLeaseComputer.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30))));
	}
}
