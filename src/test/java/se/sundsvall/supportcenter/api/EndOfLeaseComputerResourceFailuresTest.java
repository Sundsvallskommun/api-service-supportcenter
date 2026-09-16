package se.sundsvall.supportcenter.api;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseComputerResourceFailuresTest {

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void retryWithInvalidMunicipalityId() {

		webTestClient.post().uri("/not-a-municipality-id/endOfLeaseComputers/retry")
			.contentType(APPLICATION_JSON)
			.bodyValue(RetryEndOfLeaseComputersRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].field").isEqualTo("retryEndOfLeaseComputers.municipalityId")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	/**
	 * The serial numbers become one IN clause, so a caller who sends their whole fleet is turned down rather than
	 * handed to the database.
	 */
	@Test
	void retryWithTooManySerialNumbers() {

		final var serialNumbers = IntStream.rangeClosed(1, 1001).mapToObj("J%dABC"::formatted).toList();

		webTestClient.post().uri("/2281/endOfLeaseComputers/retry")
			.contentType(APPLICATION_JSON)
			.bodyValue(RetryEndOfLeaseComputersRequest.create().withSerialNumbers(serialNumbers))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].field").isEqualTo("serialNumbers")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}
}
