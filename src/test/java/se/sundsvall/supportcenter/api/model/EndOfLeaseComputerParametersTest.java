package se.sundsvall.supportcenter.api.model;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseComputerParametersTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> List.of(randomUUID().toString()), List.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseComputerParameters.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var status = List.of("FAILED", "PENDING");

		final var parameters = EndOfLeaseComputerParameters.create()
			.withStatus(status)
			.withPage(3)
			.withLimit(25);

		assertThat(parameters.getStatus()).isEqualTo(status);
		assertThat(parameters.getPage()).isEqualTo(3);
		assertThat(parameters.getLimit()).isEqualTo(25);
	}

	/**
	 * The page and limit come from the base with values already in them, which is what makes a call that names neither
	 * answer with the first hundred rather than with nothing.
	 */
	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseComputerParameters.create()).hasAllNullFieldsOrPropertiesExcept("page", "limit");
		assertThat(EndOfLeaseComputerParameters.create().getPage()).isEqualTo(1);
		assertThat(EndOfLeaseComputerParameters.create().getLimit()).isEqualTo(100);
		assertThat(EndOfLeaseComputerParameters.create().getStatus()).isNull();
	}
}
