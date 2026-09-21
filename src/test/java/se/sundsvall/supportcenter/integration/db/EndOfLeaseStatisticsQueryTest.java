package se.sundsvall.supportcenter.integration.db;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;

/**
 * The two reads the statistics are built from, against a database rather than a mock.
 *
 * The grouped count is a constructor expression, which no unit test can tell apart from one the database rejects, and
 * both reads carry an order the answers are expected to keep.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@Transactional
class EndOfLeaseStatisticsQueryTest {

	private static final String MUNICIPALITY_ID = "2281";

	/**
	 * A window wide enough to hold everything a test seeds, so that the tests that are not about the window do not have
	 * to think about it.
	 */
	private static final OffsetDateTime EVERYTHING_FROM = now(systemDefault()).minusYears(10);
	private static final OffsetDateTime EVERYTHING_TO = now(systemDefault()).plusYears(10);

	@Autowired
	private EndOfLeaseComputerRepository endOfLeaseComputerRepository;

	@Autowired
	private EndOfLeaseBatchRepository endOfLeaseBatchRepository;

	@Test
	void countsEachStateOfEachBatchWithTheNewestBatchFirst() {
		final var now = now(systemDefault());
		final var older = aBatch(now.minusDays(2));
		final var newer = aBatch(now.minusDays(1));

		aComputer(older, "J111AAA", SENT);
		aComputer(older, "J222BBB", SENT);
		aComputer(older, "J333CCC", FAILED);
		aComputer(newer, "J444DDD", PENDING);
		aComputer(newer, "J555EEE", EXCLUDED);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO);

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId, EndOfLeaseBatchStatusCount::status, EndOfLeaseBatchStatusCount::count)
			.containsExactlyInAnyOrder(
				tuple(newer.getId(), PENDING, 1L),
				tuple(newer.getId(), EXCLUDED, 1L),
				tuple(older.getId(), SENT, 2L),
				tuple(older.getId(), FAILED, 1L));

		assertThat(counts)
			.as("the newest batch comes first, so that the rows read in the order they are answered in")
			.extracting(EndOfLeaseBatchStatusCount::batchId)
			.startsWith(newer.getId(), newer.getId());
	}

	@Test
	void carriesTheColumnsOfTheBatchAlong() {
		final var created = now(systemDefault()).minusDays(3);
		final var batch = aBatch(created);
		aComputer(batch, "J666FFF", SENT);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO);

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId, EndOfLeaseBatchStatusCount::externalBatchId)
			.contains(tuple(batch.getId(), batch.getExternalBatchId()));
		assertThat(counts)
			.filteredOn(count -> count.batchId().equals(batch.getId()))
			.allSatisfy(count -> assertThat(count.created()).isCloseTo(created, within(1, SECONDS)));
	}

	@Test
	void leavesAnotherMunicipalitysBatchesAlone() {
		final var ours = aBatch(now(systemDefault()));
		aComputer(ours, "J777GGG", SENT);

		final var theirs = endOfLeaseBatchRepository.save(EndOfLeaseBatchEntity.create()
			.withMunicipalityId("2260")
			.withExternalBatchId(randomUUID().toString()));
		aComputer(theirs, "J888HHH", SENT);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO);

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId)
			.contains(ours.getId())
			.doesNotContain(theirs.getId());
	}

	@Test
	void readsABatchInStateOrderAndThenBySerialNumber() {
		final var batch = aBatch(now(systemDefault()));
		aComputer(batch, "K222BBB", SENT);
		aComputer(batch, "K111AAA", SENT);
		aComputer(batch, "K333CCC", FAILED);
		aComputer(batch, "K444DDD", EXCLUDED);

		final var computers = endOfLeaseComputerRepository.findByBatchId(batch.getId());

		assertThat(computers)
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getStatus)
			.containsExactly(
				tuple("K444DDD", EXCLUDED),
				tuple("K333CCC", FAILED),
				tuple("K111AAA", SENT),
				tuple("K222BBB", SENT));
	}

	@Test
	void findsOneBatchOfOneMunicipality() {
		final var batch = aBatch(now(systemDefault()));

		assertThat(endOfLeaseBatchRepository.findByIdAndMunicipalityId(batch.getId(), MUNICIPALITY_ID)).contains(batch);
		assertThat(endOfLeaseBatchRepository.findByIdAndMunicipalityId(batch.getId(), "2260")).isEmpty();
	}

	private EndOfLeaseBatchEntity aBatch(final OffsetDateTime created) {
		return endOfLeaseBatchRepository.save(EndOfLeaseBatchEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withExternalBatchId(randomUUID().toString())
			.withCreated(created));
	}

	private void aComputer(final EndOfLeaseBatchEntity batch, final String serialNumber, final EndOfLeaseStatus status) {
		endOfLeaseComputerRepository.save(EndOfLeaseComputerEntity.create()
			.withBatch(batch)
			.withSerialNumber(serialNumber)
			.withAssetTag("AB" + serialNumber)
			.withEndOfLeaseDate(LocalDate.of(2026, 12, 31))
			.withStatus(status));
	}

	/**
	 * The window is what keeps the read from growing with everything the municipality has ever registered, so a batch
	 * outside it has to be left out rather than merely sorted last.
	 */
	@Test
	void countsOnlyTheBatchesInsideTheWindow() {
		final var now = now(systemDefault());
		final var inside = aBatch(now.minusDays(3));
		final var tooOld = aBatch(now.minusMonths(6));

		aComputer(inside, "J111AAA", SENT);
		aComputer(tooOld, "J222BBB", SENT);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(MUNICIPALITY_ID, now.minusMonths(1), now.plusDays(1));

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId)
			.contains(inside.getId())
			.doesNotContain(tooOld.getId());
	}

	/**
	 * The first moment is counted and the last is not, which is what lets a caller ask for whole days without the day
	 * after leaking in.
	 */
	@Test
	void countsFromTheFirstMomentUpToButNotIncludingTheLast() {
		final var start = OffsetDateTime.parse("2026-06-01T00:00:00+02:00");
		final var onTheEdge = aBatch(start);
		final var justOutside = aBatch(start.plusDays(1));

		aComputer(onTheEdge, "J333CCC", SENT);
		aComputer(justOutside, "J444DDD", SENT);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(MUNICIPALITY_ID, start, start.plusDays(1));

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId)
			.containsExactly(onTheEdge.getId())
			.doesNotContain(justOutside.getId());
	}
}
