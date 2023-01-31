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

class NoteTest {

	@Test
	void isValidBean() {
		assertThat(Note.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var text = "text";
		final var type = NoteType.PROBLEM;

		final var note = Note.create()
			.withText(text)
			.withType(type);

		assertThat(note).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(note.getText()).isEqualTo(text);
		assertThat(note.getType()).isEqualTo(type);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(Note.create()).hasAllNullFieldsOrProperties();
	}
}
