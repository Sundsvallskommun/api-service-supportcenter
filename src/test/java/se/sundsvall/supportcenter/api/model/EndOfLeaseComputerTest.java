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
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseComputerTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseComputer.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var serialNumber = "serialNumber";
		final var assetTag = "assetTag";
		final var endOfLeaseDate = LocalDate.now().plusDays(90L);

		final var endOfLeaseComputer = EndOfLeaseComputer.create()
			.withSerialNumber(serialNumber)
			.withAssetTag(assetTag)
			.withEndOfLeaseDate(endOfLeaseDate);

		assertThat(endOfLeaseComputer).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseComputer.getSerialNumber()).isEqualTo(serialNumber);
		assertThat(endOfLeaseComputer.getAssetTag()).isEqualTo(assetTag);
		assertThat(endOfLeaseComputer.getEndOfLeaseDate()).isEqualTo(endOfLeaseDate);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseComputer.create()).hasAllNullFieldsOrProperties();
		assertThat(new EndOfLeaseComputer()).hasAllNullFieldsOrProperties();
	}
}
