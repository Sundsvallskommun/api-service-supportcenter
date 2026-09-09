package se.sundsvall.supportcenter.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EndOfLeaseBatchEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusSeconds(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> List.of(EndOfLeaseComputerEntity.create().withSerialNumber(randomUUID().toString())), List.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseBatchEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("computers"),
			hasValidBeanEqualsExcluding("computers"),
			hasValidBeanToStringExcluding("computers")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var externalBatchId = "externalBatchId";
		final var municipalityId = "2281";
		final var created = now();
		final var computers = List.of(EndOfLeaseComputerEntity.create().withSerialNumber("J123ABC"));

		final var endOfLeaseBatchEntity = EndOfLeaseBatchEntity.create()
			.withId(id)
			.withExternalBatchId(externalBatchId)
			.withMunicipalityId(municipalityId)
			.withCreated(created)
			.withComputers(computers);

		assertThat(endOfLeaseBatchEntity).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseBatchEntity.getId()).isEqualTo(id);
		assertThat(endOfLeaseBatchEntity.getExternalBatchId()).isEqualTo(externalBatchId);
		assertThat(endOfLeaseBatchEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(endOfLeaseBatchEntity.getCreated()).isEqualTo(created);
		assertThat(endOfLeaseBatchEntity.getComputers()).isEqualTo(computers);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseBatchEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new EndOfLeaseBatchEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsCreated() {
		final var endOfLeaseBatchEntity = EndOfLeaseBatchEntity.create();

		endOfLeaseBatchEntity.prePersist();

		assertThat(endOfLeaseBatchEntity.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void prePersistKeepsCreatedThatIsAlreadySet() {
		final var created = now().minusDays(1);
		final var endOfLeaseBatchEntity = EndOfLeaseBatchEntity.create().withCreated(created);

		endOfLeaseBatchEntity.prePersist();

		assertThat(endOfLeaseBatchEntity.getCreated()).isEqualTo(created);
	}
}
