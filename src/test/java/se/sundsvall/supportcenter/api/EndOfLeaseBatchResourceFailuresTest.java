package se.sundsvall.supportcenter.api;

import java.time.LocalDate;
import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static java.util.Collections.emptyList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseBatchResourceFailuresTest {

	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";

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
							{"field":"externalBatchId","message":"not a valid UUID"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void createEndOfLeaseBatchWithInvalidExternalBatchId() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withExternalBatchId("not-a-uuid").withComputers(List.of(validComputer())))
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
							{"field":"externalBatchId","message":"not a valid UUID"}
						]""")));

		verifyNoInteractions(endOfLeaseServiceMock);
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

	private static EndOfLeaseComputer validComputer() {
		return EndOfLeaseComputer.create()
			.withSerialNumber("J123ABC")
			.withAssetTag("AB12345")
			.withEndOfLeaseDate(LocalDate.of(2026, 11, 30));
	}
}
