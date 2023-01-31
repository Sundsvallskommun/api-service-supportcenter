package se.sundsvall.supportcenter.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.service.ConfigurationService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class ConfigurationResourceFailuresTest {

	@MockBean
	private ConfigurationService configurationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getClosureCodeListMissingApiKeyHeader() {

		webTestClient.get().uri("/configuration/closureCodes")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(configurationServiceMock);
	}

	@Test
	void getCaseCategoryListMissingApiKeyHeader() {

		webTestClient.get().uri("/configuration/caseCategories")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(configurationServiceMock);
	}
}
