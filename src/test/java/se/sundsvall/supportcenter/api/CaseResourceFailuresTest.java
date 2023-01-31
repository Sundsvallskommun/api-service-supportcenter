package se.sundsvall.supportcenter.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.Address;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.CaseService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.PROBLEM;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class CaseResourceFailuresTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";

	@MockBean
	private CaseService caseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createCaseMissingBody() {

		webTestClient.post().uri("/cases")
				.contentType(APPLICATION_JSON)
				.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
				.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
				.jsonPath("$.detail").isEqualTo(
						"Required request body is missing: public org.springframework.http.ResponseEntity<java.lang.Void> se.sundsvall.supportcenter.api.CaseResource.createCase(org.springframework.web.util.UriComponentsBuilder,java.lang.String,se.sundsvall.supportcenter.api.model.CreateCaseRequest)");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingPobKeyHeader() {

		webTestClient.post().uri("/cases").contentType(APPLICATION_JSON)
				.bodyValue(CreateCaseRequest.create())
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
				.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
				.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingContentType() {

		webTestClient.post().uri("/cases")
				.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
				.exchange()
				.expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
				.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo(UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
				.jsonPath("$.status").isEqualTo(UNSUPPORTED_MEDIA_TYPE.value())
				.jsonPath("$.detail").isEqualTo("Content type '' not supported");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void createCaseMissingNoteType() {

		webTestClient.post().uri("/cases")
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

		webTestClient.post().uri("/cases")
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

		webTestClient.post().uri("/cases")
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

		webTestClient.post().uri("/cases")
				.contentType(APPLICATION_JSON)
				.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
				.bodyValue(CreateCaseRequest.create())
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Constraint Violation")
				.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
				.jsonPath("$.violations[0].field").isEqualTo("activityNumber")
				.jsonPath("$.violations[0].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[1].field").isEqualTo("businessNumber")
				.jsonPath("$.violations[1].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[2].field").isEqualTo("caseCategory")
				.jsonPath("$.violations[2].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[3].field").isEqualTo("caseType")
				.jsonPath("$.violations[3].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[4].field").isEqualTo("ciDescription")
				.jsonPath("$.violations[4].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[5].field").isEqualTo("contactPerson")
				.jsonPath("$.violations[5].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[6].field").isEqualTo("counterPart")
				.jsonPath("$.violations[6].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[7].field").isEqualTo("customerContact")
				.jsonPath("$.violations[7].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[8].field").isEqualTo("description")
				.jsonPath("$.violations[8].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[9].field").isEqualTo("email")
				.jsonPath("$.violations[9].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[10].field").isEqualTo("externalArticleNumber")
				.jsonPath("$.violations[10].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[11].field").isEqualTo("externalServiceId")
				.jsonPath("$.violations[11].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[12].field").isEqualTo("freeText")
				.jsonPath("$.violations[12].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[13].field").isEqualTo("joinContact")
				.jsonPath("$.violations[13].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[14].field").isEqualTo("managementCompany")
				.jsonPath("$.violations[14].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[15].field").isEqualTo("objectNumber")
				.jsonPath("$.violations[15].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[16].field").isEqualTo("office")
				.jsonPath("$.violations[16].message").isEqualTo("must not be null")
				.jsonPath("$.violations[17].field").isEqualTo("personal")
				.jsonPath("$.violations[17].message").isEqualTo("must not be null")
				.jsonPath("$.violations[18].field").isEqualTo("phoneNumber")
				.jsonPath("$.violations[18].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[19].field").isEqualTo("priority")
				.jsonPath("$.violations[19].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[20].field").isEqualTo("projectNumber")
				.jsonPath("$.violations[20].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[21].field").isEqualTo("responsibilityNumber")
				.jsonPath("$.violations[21].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[22].field").isEqualTo("responsibleGroup")
				.jsonPath("$.violations[22].message").isEqualTo("must not be blank")
				.jsonPath("$.violations[23].field").isEqualTo("subaccount");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingBody() {

		webTestClient.patch().uri("/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo(
				"Required request body is missing: public org.springframework.http.ResponseEntity<java.lang.Void> se.sundsvall.supportcenter.api.CaseResource.updateCase(java.lang.String,java.lang.String,se.sundsvall.supportcenter.api.model.UpdateCaseRequest)");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingPobKeyHeader() {

		webTestClient.patch().uri("/cases/{caseId}", "12345")
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingContentType() {

		webTestClient.patch().uri("/cases/{caseId}", "12345")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(UNSUPPORTED_MEDIA_TYPE.value())
			.jsonPath("$.detail").isEqualTo("Content type '' not supported");

		verifyNoInteractions(caseServiceMock);
	}

	@Test
	void updateCaseMissingNoteType() {

		webTestClient.patch().uri("/cases/{caseId}", "12345")
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

		webTestClient.patch().uri("/cases/{caseId}", "12345")
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

		webTestClient.patch().uri("/cases/{caseId}", "12345")
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

		webTestClient.get().uri("/cases/{caseId}", "12345")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
				.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
				.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

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
