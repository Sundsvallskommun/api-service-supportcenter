package se.sundsvall.supportcenter.service;

import generated.client.pob.PobPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportcenter.api.model.Address;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.enums.NoteType;
import se.sundsvall.supportcenter.integration.pob.POBClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.DEFAULT_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ACTIVITY_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_BUSINESS_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CITY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CONTACT_PERSON;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_COUNTERPART;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CUSTOMER_CONTACT;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EMAIL;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_ARTICLE_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_SERVICE_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_FREE_TEXT;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_JOIN_CONTACT;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_MANAGEMENT_COMPANY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_OBJECT_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_OFFICE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PERSONAL;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PHONE_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_POSTAL_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PRIORITY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PROJECT_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_GROUP;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_STREET;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_SUBACCOUNT;

@ExtendWith(MockitoExtension.class)
class CreateCaseServiceTest {

	@Mock
	private POBClient pobClientMock;

	@Mock
	private ConfigurationService configurationServiceMock;

	@InjectMocks
	private CaseService caseService;

	@Captor
	private ArgumentCaptor<PobPayload> pobPayloadCaptor;
	
	@Test
	void createCase() {

		// Parameter values
		final var pobKey = "pobKey";
		final var note = Note.create().withText("text").withType(NoteType.WORKNOTE);
		final var description = "description";
		final var caseType = "caseType";
		final var caseCategory = "caseCategory";
		final var customerContact = "customerContact";
		final var externalArticleNumber = "externalArticleNumber";
		final var managementCompany = "managementCompany";
		final var priority = "priority";
		final var personal = false;
		final var responsibleGroup = "responsibleGroup";
		final var externalServiceId = "externalServiceId";
		final var office = true;
		final var freeText = "freeText";
		final var joinContact = "joinContact";
		final var responsibilityNumber = "responsibilityNumber";
		final var subaccount = "subaccount";
		final var businessNumber = "businessNumber";
		final var activityNumber = "activityNumber";
		final var projectNumber = "projectNumber";
		final var objectNumber = "objectNumber";
		final var counterPart = "counterPart";
		final var ciDescription = "ciDescription";
		final var contactPerson = "contactPerson";
		final var phoneNumber = "phoneNumber";
		final var email = "email";
		final var address = Address.create().withCity("city").withPostalCode("postalCode").withStreet("street");

		final var request = CreateCaseRequest.create()
				.withNote(note)
				.withDescription(description)
				.withCaseType(caseType)
				.withCaseCategory(caseCategory)
				.withCustomerContact(customerContact)
				.withExternalArticleNumber(externalArticleNumber)
				.withManagementCompany(managementCompany)
				.withPriority(priority)
				.withPersonal(personal)
				.withResponsibleGroup(responsibleGroup)
				.withExternalServiceId(externalServiceId)
				.withOffice(office)
				.withFreeText(freeText)
				.withJoinContact(joinContact)
				.withResponsibilityNumber(responsibilityNumber)
				.withSubaccount(subaccount)
				.withBusinessNumber(businessNumber)
				.withActivityNumber(activityNumber)
				.withProjectNumber(projectNumber)
				.withObjectNumber(objectNumber)
				.withCounterPart(counterPart)
				.withCiDescription(ciDescription)
				.withContactPerson(contactPerson)
				.withPhoneNumber(phoneNumber)
				.withEmail(email)
				.withAddress(address);
		// Call
		caseService.createCase(pobKey, request);

		// Verification
		verify(pobClientMock).createCase(eq(pobKey), pobPayloadCaptor.capture());
		verifyNoMoreInteractions(pobClientMock);

		final var pobPayloadValue = pobPayloadCaptor.getValue();
		assertThat(pobPayloadValue).isNotNull();
		assertThat(pobPayloadValue.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValue.getData()).isNotNull()
				.containsEntry(KEY_DESCRIPTION, description)
				.containsEntry(KEY_CASE_TYPE, caseType)
				.containsEntry(KEY_CASE_CATEGORY, caseCategory)
				.containsEntry(KEY_CUSTOMER_CONTACT, customerContact)
				.containsEntry(KEY_EXTERNAL_ARTICLE_NUMBER, externalArticleNumber)
				.containsEntry(KEY_MANAGEMENT_COMPANY, managementCompany)
				.containsEntry(KEY_PRIORITY, priority)
				.containsEntry(KEY_PERSONAL, "0")
				.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
				.containsEntry(KEY_EXTERNAL_SERVICE_ID, externalServiceId)
				.containsEntry(KEY_OFFICE, "1")
				.containsEntry(KEY_FREE_TEXT, freeText)
				.containsEntry(KEY_JOIN_CONTACT, joinContact)
				.containsEntry(KEY_RESPONSIBLE_NUMBER, responsibilityNumber)
				.containsEntry(KEY_SUBACCOUNT, subaccount)
				.containsEntry(KEY_BUSINESS_NUMBER, businessNumber)
				.containsEntry(KEY_ACTIVITY_NUMBER, activityNumber)
				.containsEntry(KEY_PROJECT_NUMBER, projectNumber)
				.containsEntry(KEY_OBJECT_NUMBER, objectNumber)
				.containsEntry(KEY_COUNTERPART, counterPart)
				.containsEntry(KEY_CI_DESCRIPTION, ciDescription)
				.containsEntry(KEY_CONTACT_PERSON, contactPerson)
				.containsEntry(KEY_PHONE_NUMBER, phoneNumber)
				.containsEntry(KEY_EMAIL, email)
				.containsEntry(KEY_STREET, address.getStreet())
				.containsEntry(KEY_CITY, address.getCity())
				.containsEntry(KEY_POSTAL_CODE, address.getPostalCode());

		assertThat(pobPayloadValue.getMemo()).isNotNull();
		assertThat(pobPayloadValue.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo("text");
	}

	@Test
	void createCaseValidationFailsDueToBadCaseCategory() {
		// Parameter values
		final var pobKey = "pobKey";
		final var caseCategory = "badCaseCategory";

		final var request = CreateCaseRequest.create()
				.withNote(Note.create().withText("text").withType(NoteType.WORKNOTE))
				.withDescription("description")
				.withCaseType("caseType")
				.withCaseCategory(caseCategory)
				.withCustomerContact("customerContact")
				.withExternalArticleNumber("externalArticleNumber")
				.withManagementCompany("managementCompany")
				.withPriority("priority")
				.withPersonal(false)
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
				.withAddress(Address.create().withCity("city").withPostalCode("postalCode").withStreet("street"));

		// Mock
		doThrow(Problem.valueOf(Status.NOT_FOUND, "Could not find caseCategory")).when(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);

		// Call
		assertThrows(ThrowableProblem.class, () -> caseService.createCase(pobKey, request));

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verifyNoInteractions(pobClientMock);
	}
}
