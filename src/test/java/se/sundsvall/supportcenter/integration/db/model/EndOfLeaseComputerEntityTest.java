package se.sundsvall.supportcenter.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

class EndOfLeaseComputerEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
		registerValueGenerator(() -> now().plusSeconds(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> EndOfLeaseBatchEntity.create().withId(randomUUID().toString()), EndOfLeaseBatchEntity.class);
	}

	@Test
	void isValidBean() {
		assertThat(EndOfLeaseComputerEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("batch"),
			hasValidBeanEqualsExcluding("batch"),
			hasValidBeanToStringExcluding("batch")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var batch = EndOfLeaseBatchEntity.create().withId("batchId");
		final var serialNumber = "J123ABC";
		final var assetTag = "AB12345";
		final var endOfLeaseDate = LocalDate.now().plusDays(90L);
		final var assetMunicipalityId = "2260";
		final var attempts = 2;
		final var errorMessage = "errorMessage";
		final var sentAt = now();
		final var created = now();
		final var modified = now();

		final var endOfLeaseComputerEntity = EndOfLeaseComputerEntity.create()
			.withId(id)
			.withBatch(batch)
			.withSerialNumber(serialNumber)
			.withAssetTag(assetTag)
			.withEndOfLeaseDate(endOfLeaseDate)
			.withStatus(PENDING)
			.withAssetMunicipalityId(assetMunicipalityId)
			.withAttempts(attempts)
			.withErrorMessage(errorMessage)
			.withSentAt(sentAt)
			.withCreated(created)
			.withModified(modified);

		assertThat(endOfLeaseComputerEntity).hasNoNullFieldsOrProperties();
		assertThat(endOfLeaseComputerEntity.getId()).isEqualTo(id);
		assertThat(endOfLeaseComputerEntity.getBatch()).isEqualTo(batch);
		assertThat(endOfLeaseComputerEntity.getSerialNumber()).isEqualTo(serialNumber);
		assertThat(endOfLeaseComputerEntity.getAssetTag()).isEqualTo(assetTag);
		assertThat(endOfLeaseComputerEntity.getEndOfLeaseDate()).isEqualTo(endOfLeaseDate);
		assertThat(endOfLeaseComputerEntity.getStatus()).isEqualTo(PENDING);
		assertThat(endOfLeaseComputerEntity.getAssetMunicipalityId()).isEqualTo(assetMunicipalityId);
		assertThat(endOfLeaseComputerEntity.getAttempts()).isEqualTo(attempts);
		assertThat(endOfLeaseComputerEntity.getErrorMessage()).isEqualTo(errorMessage);
		assertThat(endOfLeaseComputerEntity.getSentAt()).isEqualTo(sentAt);
		assertThat(endOfLeaseComputerEntity.getCreated()).isEqualTo(created);
		assertThat(endOfLeaseComputerEntity.getModified()).isEqualTo(modified);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EndOfLeaseComputerEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new EndOfLeaseComputerEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsCreatedAndDefaultsAttempts() {
		final var endOfLeaseComputerEntity = EndOfLeaseComputerEntity.create();

		endOfLeaseComputerEntity.prePersist();

		assertThat(endOfLeaseComputerEntity.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(endOfLeaseComputerEntity.getAttempts()).isZero();
	}

	@Test
	void prePersistKeepsAttemptsThatAreAlreadySet() {
		final var endOfLeaseComputerEntity = EndOfLeaseComputerEntity.create().withAttempts(3);

		endOfLeaseComputerEntity.prePersist();

		assertThat(endOfLeaseComputerEntity.getAttempts()).isEqualTo(3);
	}

	@Test
	void prePersistKeepsCreatedThatIsAlreadySet() {
		final var created = now().minusDays(1);
		final var endOfLeaseComputerEntity = EndOfLeaseComputerEntity.create().withCreated(created);

		endOfLeaseComputerEntity.prePersist();

		assertThat(endOfLeaseComputerEntity.getCreated()).isEqualTo(created);
	}

	@Test
	void preUpdateSetsModified() {
		final var endOfLeaseComputerEntity = EndOfLeaseComputerEntity.create();

		endOfLeaseComputerEntity.preUpdate();

		assertThat(endOfLeaseComputerEntity.getModified()).isCloseTo(now(), within(2, SECONDS));
	}
}
