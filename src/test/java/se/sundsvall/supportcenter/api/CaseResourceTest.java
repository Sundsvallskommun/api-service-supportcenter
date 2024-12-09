package se.sundsvall.supportcenter.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.Address;
import se.sundsvall.supportcenter.api.model.Case;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.model.enums.NoteType;
import se.sundsvall.supportcenter.service.CaseService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class CaseResourceTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";

	@MockBean
	private CaseService caseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createCase() {

		when(caseServiceMock.getCaseCategoryList(POBKEY_HEADER_VALUE)).thenReturn(List.of("caseCategory"));

		final var createCaseRequest = CreateCaseRequest.create()
			.withNote(Note.create().withText("text").withType(NoteType.WORKNOTE))
			.withDescription("description")
			.withCaseType("caseType")
			.withCaseCategory("caseCategory")
			.withCustomerContact("customerContact")
			.withExternalArticleNumber("externalArticleNumber")
			.withPriority("priority")
			.withPersonal(false)
			.withResponsibleGroup("responsibleGroup")
			.withExternalServiceId("externalServiceId")
			.withOffice(true)
			.withFreeText("freeText")
			.withJoinContact("joinContact")
			.withResponsibilityNumber("responsibilityNumber")
			.withSubaccount("subaccount")
			.withActivityNumber("activityNumber")
			.withManagementCompany("managementCompany")
			.withBusinessNumber("businessNumber")
			.withProjectNumber("projectNumber")
			.withObjectNumber("objectNumber")
			.withCounterPart("counterPart")
			.withCiDescription("ciDescription")
			.withContactPerson("contactPerson")
			.withPhoneNumber("phoneNumber")
			.withEmail("email")
			.withAddress(Address.create().withCity("city").withPostalCode("postalCode").withStreet("street"));

		when(caseServiceMock.createCase(POBKEY_HEADER_VALUE, createCaseRequest)).thenReturn("caseId");

		webTestClient.post().uri("/2281/cases")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.contentType(APPLICATION_JSON)
			.bodyValue(createCaseRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().location("/2281/cases/" + "caseId")
			.expectBody().isEmpty();

		verify(caseServiceMock).createCase(POBKEY_HEADER_VALUE, createCaseRequest);
	}

	@Test
	void updateCase() {

		// Parameter values
		final var caseId = "12345";

		webTestClient.patch().uri("/2281/cases/{caseId}", caseId)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.contentType(APPLICATION_JSON)
			.bodyValue(UpdateCaseRequest.create())
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(caseServiceMock).updateCase(eq(POBKEY_HEADER_VALUE), eq(caseId), any());
	}

	@Test
	void getCase() {

		// Parameter values
		final var caseId = "12345";

		when(caseServiceMock.getCase(POBKEY_HEADER_VALUE, caseId)).thenReturn(Case.create());

		webTestClient.get().uri("/2281/cases/{caseId}", caseId)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$").isMap();

		verify(caseServiceMock).getCase(POBKEY_HEADER_VALUE, caseId);
	}
}
