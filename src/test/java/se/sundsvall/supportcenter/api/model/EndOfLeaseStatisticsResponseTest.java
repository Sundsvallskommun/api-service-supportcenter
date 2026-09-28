package se.sundsvall.supportcenter.api.model;

import java.time.LocalDate;
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
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseStatisticsResponseTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
		registerValueGenerator(() -> EndOfLeaseComputerCounts.create().withTotal(new Random().nextInt(1000)), EndOfLeaseComputerCounts.class);
		registerValueGenerator(() -> PagingMetaData.create().withPage(new Random().nextInt(1000)), PagingMetaData.class);
		registerValueGenerator(() -> List.of(EndOfLeaseBatchStatistics.create().withId(randomUUID().toString())), List.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseStatisticsResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var counts = EndOfLeaseComputerCounts.create().withTotal(1240).withSent(1180);
		final var batches = List.of(EndOfLeaseBatchStatistics.create().withId("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c"));
		final var metadata = PagingMetaData.create()
			.withPage(1)
			.withLimit(100)
			.withCount(1)
			.withTotalRecords(128)
			.withTotalPages(2);

		final var from = LocalDate.of(2026, 8, 18);
		final var to = LocalDate.of(2026, 9, 18);

		final var endOfLeaseStatisticsResponse = EndOfLeaseStatisticsResponse.create()
			.withFrom(from)
			.withTo(to)
			.withCounts(counts)
			.withBatches(batches)
			.withMetadata(metadata);

		assertThat(endOfLeaseStatisticsResponse).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseStatisticsResponse.getFrom()).isEqualTo(from);
		assertThat(endOfLeaseStatisticsResponse.getTo()).isEqualTo(to);
		assertThat(endOfLeaseStatisticsResponse.getCounts()).isEqualTo(counts);
		assertThat(endOfLeaseStatisticsResponse.getBatches()).isEqualTo(batches);
		assertThat(endOfLeaseStatisticsResponse.getMetadata()).isEqualTo(metadata);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseStatisticsResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new EndOfLeaseStatisticsResponse()).hasAllNullFieldsOrProperties();
	}
}
