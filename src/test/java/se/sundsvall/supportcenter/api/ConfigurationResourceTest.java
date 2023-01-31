package se.sundsvall.supportcenter.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;

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
class ConfigurationResourceTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";

	@MockBean
	private ConfigurationService configurationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getClosureCodeList() {

		// Mock
		when(configurationServiceMock.getClosureCodeList(POBKEY_HEADER_VALUE)).thenReturn(List.of("ClosureCode-1", "ClosureCode-2"));

		webTestClient.get().uri("/configuration/closureCodes")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.[0]").isEqualTo("ClosureCode-1")
			.jsonPath("$.[1]").isEqualTo("ClosureCode-2");

		verify(configurationServiceMock).getClosureCodeList(POBKEY_HEADER_VALUE);
	}

	@Test
	void getClosureCodeListWhenEmpty() {

		webTestClient.get().uri("/configuration/closureCodes")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$").isArray()
			.jsonPath("$").isEmpty();

		verify(configurationServiceMock).getClosureCodeList(POBKEY_HEADER_VALUE);
	}

	@Test
	void getCaseCategoryList() {

		// Mock
		when(configurationServiceMock.getCaseCategoryList(POBKEY_HEADER_VALUE)).thenReturn(List.of("CaseCategory-1", "CaseCategory-2"));

		webTestClient.get().uri("/configuration/caseCategories")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.[0]").isEqualTo("CaseCategory-1")
			.jsonPath("$.[1]").isEqualTo("CaseCategory-2");

		verify(configurationServiceMock).getCaseCategoryList(POBKEY_HEADER_VALUE);
	}

	@Test
	void getCaseCategoryListWhenEmpty() {

		webTestClient.get().uri("/configuration/caseCategories")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$").isArray()
			.jsonPath("$").isEmpty();

		verify(configurationServiceMock).getCaseCategoryList(POBKEY_HEADER_VALUE);
	}
}
