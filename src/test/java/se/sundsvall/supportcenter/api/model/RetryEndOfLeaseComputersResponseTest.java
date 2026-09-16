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

class RetryEndOfLeaseComputersResponseTest {

	@Test
	void isValidBean() {
		assertThat(RetryEndOfLeaseComputersResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var retryEndOfLeaseComputersResponse = RetryEndOfLeaseComputersResponse.create()
			.withReset(3);

		assertThat(retryEndOfLeaseComputersResponse.getReset()).isEqualTo(3);
	}

	/**
	 * The count is a primitive, so a created bean is not empty but zero, which is the answer for a municipality with
	 * nothing to take.
	 */
	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(RetryEndOfLeaseComputersResponse.create().getReset()).isZero();
		assertThat(new RetryEndOfLeaseComputersResponse().getReset()).isZero();
	}
}
