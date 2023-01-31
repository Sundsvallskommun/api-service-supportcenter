package se.sundsvall.supportcenter.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.dept44.test.annotation.resource.Load.ResourceType.JSON;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import generated.client.pob.PobPayload;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.supportcenter.api.model.Address;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

@ExtendWith(ResourceLoaderExtension.class)
class CaseMapperTest {

	@Test
	void toPobPayload() {

		// Parameter values
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
		final var pobPayload = CaseMapper.toPobPayload(request);

		// Verification

		assertThat(pobPayload).isNotNull();
		assertThat(pobPayload.getLinks()).isNotNull().isEmpty();
		assertThat(pobPayload.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayload.getData()).isNotNull()
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

		// Two different notes.
		assertThat(pobPayload.getMemo()).isNotNull();
		assertThat(pobPayload.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo("text");
	}

	@Test
	void toCase(@Load(value = "/GetCaseTest/pobPayload.json", as = JSON) PobPayload pobPayload) {

		final var aCase = CaseMapper.toCase(pobPayload);

		assertThat(aCase).isNotNull();
		assertThat(aCase.getCaseId()).isEqualTo("caseId");
		assertThat(aCase.getDescription()).isEqualTo("description");
		assertThat(aCase.getCaseType()).isEqualTo("caseType");
		assertThat(aCase.getCaseCategory()).isEqualTo("caseCategory");
		assertThat(aCase.getCustomerContact()).isEqualTo("customerContact");
		assertThat(aCase.getExternalArticleNumber()).isEqualTo("externalArticleNumber");
		assertThat(aCase.getManagementCompany()).isEqualTo("managementCompany");
		assertThat(aCase.getPriority()).isEqualTo("priority");
		assertThat(aCase.getPersonal()).isFalse();
		assertThat(aCase.getResponsibleGroup()).isEqualTo("responsibleGroup");
		assertThat(aCase.getExternalServiceId()).isEqualTo("serviceId");
		assertThat(aCase.getOffice()).isTrue();
		assertThat(aCase.getFreeText()).isEqualTo("freeText");
		assertThat(aCase.getJoinContact()).isEqualTo("joinContact");
		assertThat(aCase.getResponsibilityNumber()).isEqualTo("responsibilityNumber");
		assertThat(aCase.getSubaccount()).isEqualTo("subaccount");
		assertThat(aCase.getBusinessNumber()).isEqualTo("businessNumber");
		assertThat(aCase.getActivityNumber()).isEqualTo("activityNumber");
		assertThat(aCase.getProjectNumber()).isEqualTo("projectNumber");
		assertThat(aCase.getObjectNumber()).isEqualTo("objectNumber");
		assertThat(aCase.getCounterPart()).isEqualTo("counterpart");
		assertThat(aCase.getCiDescription()).isEqualTo("ciDescription");
		assertThat(aCase.getContactPerson()).isEqualTo("contactPerson");
		assertThat(aCase.getPhoneNumber()).isEqualTo("phoneNumber");
		assertThat(aCase.getEmail()).isEqualTo("email");
		assertThat(aCase.getAddress().getStreet()).isEqualTo("street");
		assertThat(aCase.getAddress().getCity()).isEqualTo("city");
		assertThat(aCase.getAddress().getPostalCode()).isEqualTo("postalCode");
		assertThat(aCase.getNote().getType()).isEqualTo(NoteType.PROBLEM);
	}
}
