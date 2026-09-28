package se.sundsvall.supportcenter.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseStatisticsResourceFailuresTest {

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getEndOfLeaseStatisticsWithInvalidMunicipalityId() {

		webTestClient.get().uri("/not-a-municipality-id/endOfLeaseStatistics")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].field").isEqualTo("getEndOfLeaseStatistics.municipalityId")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	/**
	 * The window is read as whole days, so anything that is not a date is turned down at the edge rather than carried
	 * into a query as something the database has to make sense of.
	 */
	@Test
	void getEndOfLeaseStatisticsWithADayThatIsNotADate() {

		webTestClient.get().uri("/2281/endOfLeaseStatistics?from=last-tuesday")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	/**
	 * A wide window is now a count the database takes over more rows rather than a larger answer, but the page it comes
	 * back on is still capped.
	 */
	@Test
	void getEndOfLeaseStatisticsWithALimitAboveTheCap() {

		webTestClient.get().uri("/2281/endOfLeaseStatistics?limit=201")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(String.class)
			.consumeWith(response -> assertThatJson(response.getResponseBody())
				.and(
					json -> json.node("title").isEqualTo("Constraint Violation"),
					json -> json.node("status").isEqualTo(BAD_REQUEST.value()),
					json -> json.node("violations[0].message").isEqualTo("Page limit cannot be greater than 200")));

		verifyNoInteractions(endOfLeaseServiceMock);
	}

	@Test
	void getEndOfLeaseStatisticsWithAPageBelowTheFirst() {

		webTestClient.get().uri("/2281/endOfLeaseStatistics?page=0")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);

		verifyNoInteractions(endOfLeaseServiceMock);
	}
}
