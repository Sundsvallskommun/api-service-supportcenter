package se.sundsvall.supportcenter.api.model;

import java.time.LocalDate;
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

class CreateEndOfLeaseBatchRequestTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> List.of(EndOfLeaseComputer.create().withSerialNumber(randomUUID().toString())), List.class);
	}

	@Test
	void isValidBean() {
		assertThat(CreateEndOfLeaseBatchRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var computers = List.of(EndOfLeaseComputer.create()
			.withSerialNumber("serialNumber")
			.withAssetTag("assetTag")
			.withEndOfLeaseDate(LocalDate.now().plusDays(90L)));

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withComputers(computers);

		assertThat(createEndOfLeaseBatchRequest).hasNoNullFieldsOrProperties();
		assertThat(createEndOfLeaseBatchRequest.getComputers()).isEqualTo(computers);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(CreateEndOfLeaseBatchRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new CreateEndOfLeaseBatchRequest()).hasAllNullFieldsOrProperties();
	}
}
