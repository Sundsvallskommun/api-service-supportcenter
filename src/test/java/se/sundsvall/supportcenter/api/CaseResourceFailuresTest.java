package se.sundsvall.supportcenter.api;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.Address;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.CaseService;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.PROBLEM;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class CaseResourceFailuresTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";

	@MockitoBean
	private CaseService caseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createCaseMissingBody() {

		webTestClient.post().uri("/2281/cases")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Failed to read request");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingPobKeyHeader() {

		webTestClient.post().uri("/2281/cases").contentType(APPLICATION_JSON)
			.bodyValue(CreateCaseRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required header 'pobKey' is not present.");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingContentType() {

		webTestClient.post().uri("/2281/cases")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(UNSUPPORTED_MEDIA_TYPE.value())
			.jsonPath("$.detail").isEqualTo("Content-Type 'null' is not supported.");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingNoteType() {

		webTestClient.post().uri("/2281/cases")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(createCaseRequest().withNote(Note.create().withText("text")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.type")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingNoteText() {

		webTestClient.post().uri("/2281/cases")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(createCaseRequest().withNote(Note.create().withType(PROBLEM)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.text")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseBlankNoteText() {

		webTestClient.post().uri("/2281/cases")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(createCaseRequest().withNote(Note.create().withType(PROBLEM).withText(" ")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.text")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseNoAttributesSet() {

		webTestClient.post().uri("/2281/cases")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(CreateCaseRequest.create())
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
							{"field":"activityNumber","message":"must not be blank"},
							{"field":"businessNumber","message":"must not be blank"},
							{"field":"caseCategory","message":"must not be blank"},
							{"field":"caseType","message":"must not be blank"},
							{"field":"ciDescription","message":"must not be blank"},
							{"field":"contactPerson","message":"must not be blank"},
							{"field":"counterPart","message":"must not be blank"},
							{"field":"customerContact","message":"must not be blank"},
							{"field":"description","message":"must not be blank"},
							{"field":"email","message":"must not be blank"},
							{"field":"externalArticleNumber","message":"must not be blank"},
							{"field":"externalServiceId","message":"must not be blank"},
							{"field":"freeText","message":"must not be blank"},
							{"field":"joinContact","message":"must not be blank"},
							{"field":"managementCompany","message":"must not be blank"},
							{"field":"objectNumber","message":"must not be blank"},
							{"field":"office","message":"must not be null"},
							{"field":"personal","message":"must not be null"},
							{"field":"phoneNumber","message":"must not be blank"},
							{"field":"priority","message":"must not be blank"},
							{"field":"projectNumber","message":"must not be blank"},
							{"field":"responsibilityNumber","message":"must not be blank"},
							{"field":"responsibleGroup","message":"must not be blank"},
							{"field":"subaccount","message":"must not be blank"}
						]""")));

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingBody() {

		webTestClient.patch().uri("/2281/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Failed to read request");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingPobKeyHeader() {

		webTestClient.patch().uri("/2281/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required header 'pobKey' is not present.");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingContentType() {

		webTestClient.patch().uri("/2281/cases/{caseId}", "12345")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(UNSUPPORTED_MEDIA_TYPE.value())
			.jsonPath("$.detail").isEqualTo("Content-Type 'null' is not supported.");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingNoteType() {

		webTestClient.patch().uri("/2281/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(UpdateCaseRequest.create().withNote(Note.create().withText("text")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.type")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingNoteText() {

		webTestClient.patch().uri("/2281/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(UpdateCaseRequest.create().withNote(Note.create().withType(PROBLEM)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.text")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseBlankNoteText() {

		webTestClient.patch().uri("/2281/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(UpdateCaseRequest.create().withNote(Note.create().withType(PROBLEM).withText(" ")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.text")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void getCaseMissingPobKeyHeader() {

		webTestClient.get().uri("/2281/cases/{caseId}", "12345")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required header 'pobKey' is not present.");

		verifyNoInteractions(caseServiceMock);
	}

	private CreateCaseRequest createCaseRequest() {
		return CreateCaseRequest.create()
			.withDescription("description")
			.withCaseType("caseType")
			.withCaseCategory("caseCategory")
			.withCustomerContact("customerContact")
			.withExternalArticleNumber("externalArticleNumber")
			.withManagementCompany("managementCompany")
			.withPriority("priority")
			.withPersonal(true)
			.withResponsibleGroup("responsibleGroup")
			.withExternalServiceId("externalServiceId")
			.withOffice(true)
			.withFreeText("freeText")
			.withJoinContact("joinContact")
			.withResponsibilityNumber("responsibilityNumber")
			.withSubaccount("subaccount")
			.withBusinessNumber("businessNumber")
			.withActivityNumber("activityNumber")
			.withProjectNumber("projectNumber")
			.withObjectNumber("objectNumber")
			.withCounterPart("counterPart")
			.withCiDescription("ciDescription")
			.withContactPerson("contactPerson")
			.withPhoneNumber("phoneNumber")
			.withEmail("email")
			.withAddress(Address.create().withStreet("street").withCity("city").withPostalCode("postalCode"));
	}
}
