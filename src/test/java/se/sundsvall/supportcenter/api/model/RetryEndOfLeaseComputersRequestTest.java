package se.sundsvall.supportcenter.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class RetryEndOfLeaseComputersRequestTest {

	@Test
	void isValidBean() {
		assertThat(RetryEndOfLeaseComputersRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var serialNumbers = List.of("J123ABC");
		final var retryEndOfLeaseComputersRequest = RetryEndOfLeaseComputersRequest.create()
			.withSerialNumbers(serialNumbers);

		assertThat(retryEndOfLeaseComputersRequest).hasNoNullFieldsOrProperties();
		assertThat(retryEndOfLeaseComputersRequest.getSerialNumbers()).isEqualTo(serialNumbers);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(RetryEndOfLeaseComputersRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new RetryEndOfLeaseComputersRequest()).hasAllNullFieldsOrProperties();
	}
}
