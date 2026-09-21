package se.sundsvall.supportcenter.api.model;

import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseStatisticsParametersTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseStatisticsParameters.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var from = LocalDate.of(2026, 8, 18);
		final var to = LocalDate.of(2026, 9, 18);

		final var parameters = EndOfLeaseStatisticsParameters.create()
			.withFrom(from)
			.withTo(to)
			.withPage(2)
			.withLimit(50);

		assertThat(parameters.getFrom()).isEqualTo(from);
		assertThat(parameters.getTo()).isEqualTo(to);
		assertThat(parameters.getPage()).isEqualTo(2);
		assertThat(parameters.getLimit()).isEqualTo(50);
	}

	/**
	 * Neither day has a value of its own. The window they stand for is settled in the service, which is the only place
	 * that knows what today is.
	 */
	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseStatisticsParameters.create()).hasAllNullFieldsOrPropertiesExcept("page", "limit");
		assertThat(EndOfLeaseStatisticsParameters.create().getPage()).isEqualTo(1);
		assertThat(EndOfLeaseStatisticsParameters.create().getLimit()).isEqualTo(100);
	}
}
