package se.sundsvall.supportcenter.api;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static java.util.Collections.emptyList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseBatchResourceFailuresTest {

	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
	private static final String DATASET_REQUEST_HEADER = "adv-dataset-request";

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createEndOfLeaseBatchWithoutComputers() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withExternalBatchId(EXTERNAL_BATCH_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.when(Option.IGNORING_ARRAY_ORDER)
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations").isEqualTo("""
						[
							{"field":"computers","message":"must contain at least one computer"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchWithEmptyComputerList() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withExternalBatchId(EXTERNAL_BATCH_ID).withComputers(emptyList()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.when(Option.IGNORING_ARRAY_ORDER)
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations").isEqualTo("""
						[
							{"field":"computers","message":"must contain at least one computer"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchWithComputerWithoutAttributes() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withExternalBatchId(EXTERNAL_BATCH_ID).withComputers(List.of(EndOfLeaseComputer.create())))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.when(Option.IGNORING_ARRAY_ORDER)
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations").isEqualTo("""
						[
							{"field":"computers[0].serialNumber","message":"must be provided"},
							{"field":"computers[0].assetTag","message":"must be provided"},
							{"field":"computers[0].endOfLeaseDate","message":"must be provided"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchWithoutExternalBatchId() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withComputers(List.of(validComputer())))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.when(Option.IGNORING_ARRAY_ORDER)
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations").isEqualTo("""
						[
							{"field":"externalBatchId","message":"must be provided"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@ParameterizedTest
	@MethodSource("invalidExternalBatchIdArguments")
	void createEndOfLeaseBatchWithInvalidExternalBatchId(final String externalBatchId, final String expectedViolations) {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withExternalBatchId(externalBatchId).withComputers(List.of(validComputer())))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.when(Option.IGNORING_ARRAY_ORDER)
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations").isEqualTo(expectedViolations)));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	private static Stream<Arguments> invalidExternalBatchIdArguments() {
		return Stream.of(
			Arguments.of("", """
				[
					{"field":"externalBatchId","message":"must be provided"},
					{"field":"externalBatchId","message":"size must be between 1 and 36"}
				]"""),
			Arguments.of("   ", """
				[
					{"field":"externalBatchId","message":"must be provided"}
				]"""),
			Arguments.of("D".repeat(37), """
				[
					{"field":"externalBatchId","message":"size must be between 1 and 36"}
				]"""));
	}

	@Test
	void createEndOfLeaseBatchWithTooLongSerialNumber() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create()
				.withExternalBatchId(EXTERNAL_BATCH_ID)
				.withComputers(List.of(validComputer().withSerialNumber("J".repeat(65)))))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.when(Option.IGNORING_ARRAY_ORDER)
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations").isEqualTo("""
						[
							{"field":"computers[0].serialNumber","message":"size must be between 1 and 64"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchThatIsAlreadyRegistered() {

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(validComputer()));

		when(endOfLeaseServiceMock.registerBatch("2281", createEndOfLeaseBatchRequest)).thenThrow(Problem.valueOf(CONFLICT, "Already registered"));

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.header(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isEqualTo(CONFLICT)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectHeader().valueEquals(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.expectBody()
			.jsonPath("$.status").isEqualTo(CONFLICT.value())
			.jsonPath("$.detail").isEqualTo("Already registered");

		verify(endOfLeaseServiceMock).registerBatch("2281", createEndOfLeaseBatchRequest);
	}

	@Test
	void createEndOfLeaseBatchThatIsAlreadyRegisteredWithoutDatasetRequestHeader() {

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(validComputer()));

		when(endOfLeaseServiceMock.registerBatch("2281", createEndOfLeaseBatchRequest)).thenThrow(Problem.valueOf(CONFLICT, "Already registered"));

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isEqualTo(CONFLICT)
			.expectHeader().doesNotExist(DATASET_REQUEST_HEADER);

		verify(endOfLeaseServiceMock).registerBatch("2281", createEndOfLeaseBatchRequest);
	}

	@Test
	void createEndOfLeaseBatchThatFailsValidationWithDatasetRequestHeader() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.header(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withExternalBatchId(EXTERNAL_BATCH_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectHeader().valueEquals(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID);

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchWithUnreadableBodyWithDatasetRequestHeader() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.header(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.bodyValue("{")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectHeader().valueEquals(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID);

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchWhenServiceFailsWithDatasetRequestHeader() {

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(validComputer()));

		when(endOfLeaseServiceMock.registerBatch("2281", createEndOfLeaseBatchRequest)).thenThrow(new IllegalStateException("Database is unavailable"));

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.header(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectHeader().valueEquals(DATASET_REQUEST_HEADER, EXTERNAL_BATCH_ID);

		verify(endOfLeaseServiceMock).registerBatch("2281", createEndOfLeaseBatchRequest);
	}

	private static EndOfLeaseComputer validComputer() {
		return EndOfLeaseComputer.create()
			.withSerialNumber("J123ABC")
			.withAssetTag("AB12345")
			.withEndOfLeaseDate(LocalDate.of(2026, 11, 30));
	}

	@Test
	void getEndOfLeaseBatchWithInvalidMunicipalityId() {

		webTestClient.get().uri("/not-a-municipality-id/endOfLeaseBatches/8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].field").isEqualTo("getEndOfLeaseBatch.municipalityId")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	/**
	 * The id is one we generated, so anything that is not a uuid is a mistake rather than a batch nobody has.
	 */
	@Test
	void getEndOfLeaseBatchWithInvalidBatchId() {

		webTestClient.get().uri("/2281/endOfLeaseBatches/not-a-uuid")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].field").isEqualTo("getEndOfLeaseBatch.batchId")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void getEndOfLeaseBatchWithAStateThatDoesNotExist() {

		webTestClient.get().uri("/2281/endOfLeaseBatches/8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c?status=GONE")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].message").isEqualTo("must be one of: [PENDING, SENT, FAILED, EXCLUDED]")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}
}
