package se.sundsvall.supportcenter.service.mapper.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportcenter.api.model.enums.NoteType;

class CustomStatusMappingTest {

	@Test
	void isValidBean() {
		assertThat(CustomStatusMapping.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final Map<String, Object> attributes = Map.of("CaseStatus", "Opened");
		final NoteType statusNoteType = NoteType.WORKNOTE;
		final var customStatusMapping = CustomStatusMapping.create()
			.withAttributes(attributes)
			.withStatusNoteType(statusNoteType);

		assertThat(customStatusMapping).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(customStatusMapping.getAttributes()).isEqualTo(attributes);
		assertThat(customStatusMapping.getStatusNoteType()).isEqualTo(statusNoteType);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(CustomStatusMapping.create()).hasAllNullFieldsOrProperties();
	}
}
