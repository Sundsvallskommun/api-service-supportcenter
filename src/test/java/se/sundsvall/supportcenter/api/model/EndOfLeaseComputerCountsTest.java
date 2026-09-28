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

class EndOfLeaseComputerCountsTest {

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseComputerCounts.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var endOfLeaseComputerCounts = EndOfLeaseComputerCounts.create()
			.withTotal(1240)
			.withPending(12)
			.withSent(1180)
			.withFailed(8)
			.withExcluded(40);

		assertThat(endOfLeaseComputerCounts.getTotal()).isEqualTo(1240);
		assertThat(endOfLeaseComputerCounts.getPending()).isEqualTo(12);
		assertThat(endOfLeaseComputerCounts.getSent()).isEqualTo(1180);
		assertThat(endOfLeaseComputerCounts.getFailed()).isEqualTo(8);
		assertThat(endOfLeaseComputerCounts.getExcluded()).isEqualTo(40);
	}

	/**
	 * The counts are primitives, so a created bean is not empty but zero, which is the answer for a municipality that
	 * has never registered a batch.
	 */
	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseComputerCounts.create()).usingRecursiveComparison().isEqualTo(new EndOfLeaseComputerCounts());
		assertThat(EndOfLeaseComputerCounts.create().getTotal()).isZero();
		assertThat(EndOfLeaseComputerCounts.create().getPending()).isZero();
		assertThat(EndOfLeaseComputerCounts.create().getSent()).isZero();
		assertThat(EndOfLeaseComputerCounts.create().getFailed()).isZero();
		assertThat(EndOfLeaseComputerCounts.create().getExcluded()).isZero();
	}
}
