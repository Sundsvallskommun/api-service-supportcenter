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

class CaseTest {

	@Test
	void isValidBean() {
		assertThat(Case.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var caseId = "caseId";
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

		final var aCase = Case.create()
				.withCaseId(caseId)
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

		assertThat(aCase).hasNoNullFieldsOrProperties();
		assertThat(aCase.getCaseId()).isEqualTo(caseId);
		assertThat(aCase.getNote()).isEqualTo(note);
		assertThat(aCase.getDescription()).isEqualTo(description);
		assertThat(aCase.getCaseType()).isEqualTo(caseType);
		assertThat(aCase.getCaseCategory()).isEqualTo(caseCategory);
		assertThat(aCase.getCustomerContact()).isEqualTo(customerContact);
		assertThat(aCase.getExternalArticleNumber()).isEqualTo(externalArticleNumber);
		assertThat(aCase.getManagementCompany()).isEqualTo(managementCompany);
		assertThat(aCase.getPriority()).isEqualTo(priority);
		assertThat(aCase.getPersonal()).isEqualTo(personal);
		assertThat(aCase.getResponsibleGroup()).isEqualTo(responsibleGroup);
		assertThat(aCase.getExternalServiceId()).isEqualTo(externalServiceId);
		assertThat(aCase.getOffice()).isEqualTo(office);
		assertThat(aCase.getFreeText()).isEqualTo(freeText);
		assertThat(aCase.getJoinContact()).isEqualTo(joinContact);
		assertThat(aCase.getResponsibilityNumber()).isEqualTo(responsibilityNumber);
		assertThat(aCase.getSubaccount()).isEqualTo(subaccount);
		assertThat(aCase.getBusinessNumber()).isEqualTo(businessNumber);
		assertThat(aCase.getActivityNumber()).isEqualTo(activityNumber);
		assertThat(aCase.getProjectNumber()).isEqualTo(projectNumber);
		assertThat(aCase.getObjectNumber()).isEqualTo(objectNumber);
		assertThat(aCase.getCounterPart()).isEqualTo(counterPart);
		assertThat(aCase.getCiDescription()).isEqualTo(ciDescription);
		assertThat(aCase.getContactPerson()).isEqualTo(contactPerson);
		assertThat(aCase.getPhoneNumber()).isEqualTo(phoneNumber);
		assertThat(aCase.getEmail()).isEqualTo(email);
		assertThat(aCase.getAddress()).isEqualTo(address);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(Case.create()).hasAllNullFieldsOrProperties();
	}
}
