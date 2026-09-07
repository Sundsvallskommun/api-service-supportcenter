package se.sundsvall.supportcenter.api;

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

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createEndOfLeaseBatchWithoutComputers() {

		webTestClient.post().uri("/2281/endOfLeaseBatches")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateEndOfLeaseBatchRequest.create())
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
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withComputers(emptyList()))
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
			.bodyValue(CreateEndOfLeaseBatchRequest.create().withComputers(List.of(EndOfLeaseComputer.create())))
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
}
