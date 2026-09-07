package se.sundsvall.supportcenter.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseBatchResponseTest {

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseBatchResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var endOfLeaseBatchResponse = EndOfLeaseBatchResponse.create()
			.withId(id);

		assertThat(endOfLeaseBatchResponse).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseBatchResponse.getId()).isEqualTo(id);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseBatchResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new EndOfLeaseBatchResponse()).hasAllNullFieldsOrProperties();
	}
}
