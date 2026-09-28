package se.sundsvall.supportcenter.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

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
		registerValueGenerator(() -> EndOfLeaseComputerCounts.create().withTotal(new Random().nextInt(1000)), EndOfLeaseComputerCounts.class);
		registerValueGenerator(() -> PagingMetaData.create().withPage(new Random().nextInt(1000)), PagingMetaData.class);
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
		final var counts = EndOfLeaseComputerCounts.create()
			.withTotal(982)
			.withSent(961)
			.withFailed(3)
			.withExcluded(18);
		final var computers = List.of(EndOfLeaseComputerStatus.create().withSerialNumber("J123ABC"));
		final var metadata = PagingMetaData.create()
			.withPage(1)
			.withLimit(100)
			.withCount(1)
			.withTotalRecords(982)
			.withTotalPages(10);

		final var endOfLeaseBatchStatusResponse = EndOfLeaseBatchStatusResponse.create()
			.withId(id)
			.withExternalBatchId(externalBatchId)
			.withCreated(created)
			.withCounts(counts)
			.withComputers(computers)
			.withMetadata(metadata);

		assertThat(endOfLeaseBatchStatusResponse).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseBatchStatusResponse.getId()).isEqualTo(id);
		assertThat(endOfLeaseBatchStatusResponse.getExternalBatchId()).isEqualTo(externalBatchId);
		assertThat(endOfLeaseBatchStatusResponse.getCreated()).isEqualTo(created);
		assertThat(endOfLeaseBatchStatusResponse.getCounts()).isEqualTo(counts);
		assertThat(endOfLeaseBatchStatusResponse.getComputers()).isEqualTo(computers);
		assertThat(endOfLeaseBatchStatusResponse.getMetadata()).isEqualTo(metadata);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseBatchStatusResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new EndOfLeaseBatchStatusResponse()).hasAllNullFieldsOrProperties();
	}
}
