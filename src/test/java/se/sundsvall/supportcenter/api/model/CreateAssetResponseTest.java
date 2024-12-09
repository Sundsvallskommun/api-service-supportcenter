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

class CreateAssetResponseTest {

	@Test
	void isValidBean() {
		assertThat(CreateAssetResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var createAssetResponse = CreateAssetResponse.create()
			.withId(id);

		assertThat(createAssetResponse).hasNoNullFieldsOrProperties();
		assertThat(createAssetResponse.getId()).isEqualTo(id);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(CreateAssetResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new CreateAssetResponse()).hasAllNullFieldsOrProperties();
	}
}
