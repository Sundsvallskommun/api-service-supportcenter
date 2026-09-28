package se.sundsvall.supportcenter.api.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseComputerStatusTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusSeconds(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseComputerStatus.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var sentAt = now();

		final var endOfLeaseComputerStatus = EndOfLeaseComputerStatus.create()
			.withSerialNumber("J123ABC")
			.withAssetTag("AB12345")
			.withStatus("FAILED")
			.withAttempts(5)
			.withErrorMessage("errorMessage")
			.withSentAt(sentAt);

		assertThat(endOfLeaseComputerStatus).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseComputerStatus.getSerialNumber()).isEqualTo("J123ABC");
		assertThat(endOfLeaseComputerStatus.getAssetTag()).isEqualTo("AB12345");
		assertThat(endOfLeaseComputerStatus.getStatus()).isEqualTo("FAILED");
		assertThat(endOfLeaseComputerStatus.getAttempts()).isEqualTo(5);
		assertThat(endOfLeaseComputerStatus.getErrorMessage()).isEqualTo("errorMessage");
		assertThat(endOfLeaseComputerStatus.getSentAt()).isEqualTo(sentAt);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseComputerStatus.create()).hasAllNullFieldsOrPropertiesExcept("attempts");
		assertThat(new EndOfLeaseComputerStatus()).hasAllNullFieldsOrPropertiesExcept("attempts");
		assertThat(EndOfLeaseComputerStatus.create().getAttempts()).isZero();
	}
}
