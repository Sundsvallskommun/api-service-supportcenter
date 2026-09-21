package se.sundsvall.supportcenter.api.model;

import java.time.OffsetDateTime;
import java.util.List;
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
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseBatchStatusResponseTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusSeconds(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> List.of(EndOfLeaseComputerStatus.create().withSerialNumber(randomUUID().toString())), List.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseBatchStatusResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c";
		final var externalBatchId = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
		final var created = now();
		final var computers = List.of(EndOfLeaseComputerStatus.create().withSerialNumber("J123ABC"));

		final var endOfLeaseBatchStatusResponse = EndOfLeaseBatchStatusResponse.create()
			.withId(id)
			.withExternalBatchId(externalBatchId)
			.withCreated(created)
			.withTotal(982)
			.withPending(0)
			.withSent(961)
			.withFailed(3)
			.withExcluded(18)
			.withComputers(computers);

		assertThat(endOfLeaseBatchStatusResponse).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseBatchStatusResponse.getId()).isEqualTo(id);
		assertThat(endOfLeaseBatchStatusResponse.getExternalBatchId()).isEqualTo(externalBatchId);
		assertThat(endOfLeaseBatchStatusResponse.getCreated()).isEqualTo(created);
		assertThat(endOfLeaseBatchStatusResponse.getTotal()).isEqualTo(982);
		assertThat(endOfLeaseBatchStatusResponse.getPending()).isZero();
		assertThat(endOfLeaseBatchStatusResponse.getSent()).isEqualTo(961);
		assertThat(endOfLeaseBatchStatusResponse.getFailed()).isEqualTo(3);
		assertThat(endOfLeaseBatchStatusResponse.getExcluded()).isEqualTo(18);
		assertThat(endOfLeaseBatchStatusResponse.getComputers()).isEqualTo(computers);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseBatchStatusResponse.create()).hasAllNullFieldsOrPropertiesExcept("total", "pending", "sent", "failed", "excluded");
		assertThat(new EndOfLeaseBatchStatusResponse()).hasAllNullFieldsOrPropertiesExcept("total", "pending", "sent", "failed", "excluded");
		assertThat(EndOfLeaseBatchStatusResponse.create().getTotal()).isZero();
	}
}
