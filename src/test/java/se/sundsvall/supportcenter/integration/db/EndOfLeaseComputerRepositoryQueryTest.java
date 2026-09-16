package se.sundsvall.supportcenter.integration.db;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

/**
 * What the two queue reads leave out.
 *
 * Both runs take a page from the front of the queue in age order, and a dependency failure costs a computer no attempt,
 * so before the hold was added a row that kept failing on something outside itself kept its place in every page for
 * good. A page worth of them stopped everything behind them. These tests are that scenario at page size one.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@Transactional
class EndOfLeaseComputerRepositoryQueryTest {

	@Autowired
	private EndOfLeaseComputerRepository endOfLeaseComputerRepository;

	@Autowired
	private EndOfLeaseBatchRepository endOfLeaseBatchRepository;

	@Test
	void theLookupPageReadsPastAComputerThatIsHeldBack() {
		final var batch = aBatch();
		final var now = now(systemDefault());

		final var heldBack = endOfLeaseComputerRepository.save(computer(batch, "J111AAA", "AB11111", now.minusHours(5))
			.withRetryAfter(now.plusHours(1)));
		final var waiting = endOfLeaseComputerRepository.save(computer(batch, "J222BBB", "AB22222", now.minusHours(4)));

		final var page = endOfLeaseComputerRepository.findAwaitingLookup(PENDING, now, PageRequest.ofSize(1));

		assertThat(page)
			.as("the older row is held back, so the one behind it gets the page it would otherwise have filled")
			.extracting(EndOfLeaseComputerEntity::getId)
			.containsExactly(waiting.getId())
			.doesNotContain(heldBack.getId());
	}

	@Test
	void theLookupPageTakesAComputerWhoseHoldIsUp() {
		final var batch = aBatch();
		final var now = now(systemDefault());

		final var holdIsUp = endOfLeaseComputerRepository.save(computer(batch, "J333CCC", "AB33333", now.minusHours(5))
			.withRetryAfter(now.minusMinutes(1)));

		final var page = endOfLeaseComputerRepository.findAwaitingLookup(PENDING, now, PageRequest.ofSize(10));

		assertThat(page).extracting(EndOfLeaseComputerEntity::getId).containsExactly(holdIsUp.getId());
	}

	@Test
	void theDispatchPageReadsPastAComputerThatIsHeldBack() {
		final var batch = aBatch();
		final var now = now(systemDefault());

		final var heldBack = endOfLeaseComputerRepository.save(computer(batch, "J444DDD", "AB44444", now.minusHours(5))
			.withAssetMunicipalityId("2260")
			.withRetryAfter(now.plusHours(1)));
		final var waiting = endOfLeaseComputerRepository.save(computer(batch, "J555EEE", "AB55555", now.minusHours(4))
			.withAssetMunicipalityId("2281"));

		final var page = endOfLeaseComputerRepository.findReadyToSend(PENDING, now, PageRequest.ofSize(1));

		assertThat(page)
			.as("an installation that is down cannot starve the one that is up")
			.extracting(EndOfLeaseComputerEntity::getId)
			.containsExactly(waiting.getId())
			.doesNotContain(heldBack.getId());
	}

	private EndOfLeaseBatchEntity aBatch() {
		return endOfLeaseBatchRepository.save(EndOfLeaseBatchEntity.create()
			.withMunicipalityId("2281")
			.withExternalBatchId(randomUUID().toString()));
	}

	private static EndOfLeaseComputerEntity computer(final EndOfLeaseBatchEntity batch, final String serialNumber, final String assetTag, final OffsetDateTime created) {
		return EndOfLeaseComputerEntity.create()
			.withBatch(batch)
			.withSerialNumber(serialNumber)
			.withAssetTag(assetTag)
			.withEndOfLeaseDate(LocalDate.of(2026, 12, 31))
			.withStatus(PENDING)
			.withCreated(created);
	}
}
