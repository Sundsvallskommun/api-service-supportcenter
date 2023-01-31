package se.sundsvall.supportcenter.api.model;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class CreateCaseRequestTest {

	@Test
	void isValidBean() {
		assertThat(CreateCaseRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
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

		final var createCaseRequest = CreateCaseRequest.create()
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

		assertThat(createCaseRequest).hasNoNullFieldsOrProperties();
		assertThat(createCaseRequest.getNote()).isEqualTo(note);
		assertThat(createCaseRequest.getDescription()).isEqualTo(description);
		assertThat(createCaseRequest.getCaseType()).isEqualTo(caseType);
		assertThat(createCaseRequest.getCaseCategory()).isEqualTo(caseCategory);
		assertThat(createCaseRequest.getCustomerContact()).isEqualTo(customerContact);
		assertThat(createCaseRequest.getExternalArticleNumber()).isEqualTo(externalArticleNumber);
		assertThat(createCaseRequest.getManagementCompany()).isEqualTo(managementCompany);
		assertThat(createCaseRequest.getPriority()).isEqualTo(priority);
		assertThat(createCaseRequest.getPersonal()).isEqualTo(personal);
		assertThat(createCaseRequest.getResponsibleGroup()).isEqualTo(responsibleGroup);
		assertThat(createCaseRequest.getExternalServiceId()).isEqualTo(externalServiceId);
		assertThat(createCaseRequest.getOffice()).isEqualTo(office);
		assertThat(createCaseRequest.getFreeText()).isEqualTo(freeText);
		assertThat(createCaseRequest.getJoinContact()).isEqualTo(joinContact);
		assertThat(createCaseRequest.getResponsibilityNumber()).isEqualTo(responsibilityNumber);
		assertThat(createCaseRequest.getSubaccount()).isEqualTo(subaccount);
		assertThat(createCaseRequest.getBusinessNumber()).isEqualTo(businessNumber);
		assertThat(createCaseRequest.getActivityNumber()).isEqualTo(activityNumber);
		assertThat(createCaseRequest.getProjectNumber()).isEqualTo(projectNumber);
		assertThat(createCaseRequest.getObjectNumber()).isEqualTo(objectNumber);
		assertThat(createCaseRequest.getCounterPart()).isEqualTo(counterPart);
		assertThat(createCaseRequest.getCiDescription()).isEqualTo(ciDescription);
		assertThat(createCaseRequest.getContactPerson()).isEqualTo(contactPerson);
		assertThat(createCaseRequest.getPhoneNumber()).isEqualTo(phoneNumber);
		assertThat(createCaseRequest.getEmail()).isEqualTo(email);
		assertThat(createCaseRequest.getAddress()).isEqualTo(address);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(CreateCaseRequest.create()).hasAllNullFieldsOrProperties();
	}
}
