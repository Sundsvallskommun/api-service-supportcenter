package se.sundsvall.supportcenter.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportcenter.api.model.enums.NoteType;

class UpdateCaseRequestTest {

	@Test
	void isValidBean() {
		assertThat(UpdateCaseRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var caseCategory = "caseCategory";
		final var caseStatus = "In Process";
		final var closureCode = "closureCode";
		final var hardwareName = "hardwareName";
		final var externalCaseId = "externalCaseId";
		final var note = Note.create().withType(NoteType.PROBLEM).withText("text");
		final var responsibleGroup = "responsibleGroup";
		final var serialNumber = "serialNumber";
		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withHardwareName(hardwareName)
			.withNote(note)
			.withResponsibleGroup(responsibleGroup)
			.withSerialNumber(serialNumber);

		assertThat(updateCaseRequest).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(updateCaseRequest.getCaseCategory()).isEqualTo(caseCategory);
		assertThat(updateCaseRequest.getCaseStatus()).isEqualTo(caseStatus);
		assertThat(updateCaseRequest.getClosureCode()).isEqualTo(closureCode);
		assertThat(updateCaseRequest.getExternalCaseId()).isEqualTo(externalCaseId);
		assertThat(updateCaseRequest.getHardwareName()).isEqualTo(hardwareName);
		assertThat(updateCaseRequest.getNote()).isEqualTo(note);
		assertThat(updateCaseRequest.getResponsibleGroup()).isEqualTo(responsibleGroup);
		assertThat(updateCaseRequest.getSerialNumber()).isEqualTo(serialNumber);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(UpdateCaseRequest.create()).hasAllNullFieldsOrProperties();
	}
}
