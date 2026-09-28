package se.sundsvall.supportcenter.integration.db;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatusCount;

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
 * The reads the statistics and the single batch are built from, against a database rather than a mock.
 *
 * The grouped counts are constructor expressions, which no unit test can tell apart from ones the database rejects, and
 * every read here carries an order or a page the answers are expected to keep.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@Transactional
class EndOfLeaseStatisticsQueryTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

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
	void countsEachStateOfEachBatchItIsGiven() {
		final var now = now(systemDefault());
		final var older = aBatch(now.minusDays(2));
		final var newer = aBatch(now.minusDays(1));

		aComputer(older, "J111AAA", SENT);
		aComputer(older, "J222BBB", SENT);
		aComputer(older, "J333CCC", FAILED);
		aComputer(newer, "J444DDD", PENDING);
		aComputer(newer, "J555EEE", EXCLUDED);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(List.of(older.getId(), newer.getId()));

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId, EndOfLeaseBatchStatusCount::status, EndOfLeaseBatchStatusCount::count)
			.containsExactlyInAnyOrder(
				tuple(newer.getId(), PENDING, 1L),
				tuple(newer.getId(), EXCLUDED, 1L),
				tuple(older.getId(), SENT, 2L),
				tuple(older.getId(), FAILED, 1L));
	}

	/**
	 * The caller names the batches, so a batch it did not name is not counted even when it sits in the same table and
	 * the same municipality. That is what keeps the count to the size of one page.
	 */
	@Test
	void countsOnlyTheBatchesItIsGiven() {
		final var asked = aBatch(now(systemDefault()));
		final var notAsked = aBatch(now(systemDefault()));

		aComputer(asked, "J111AAA", SENT);
		aComputer(notAsked, "J222BBB", SENT);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(List.of(asked.getId()));

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId)
			.containsExactly(asked.getId())
			.doesNotContain(notAsked.getId());
	}

	@Test
	void carriesTheColumnsOfTheBatchAlong() {
		final var created = now(systemDefault()).minusDays(3);
		final var batch = aBatch(created);
		aComputer(batch, "J666FFF", SENT);

		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(List.of(batch.getId()));

		assertThat(counts)
			.extracting(EndOfLeaseBatchStatusCount::batchId, EndOfLeaseBatchStatusCount::externalBatchId)
			.contains(tuple(batch.getId(), batch.getExternalBatchId()));
		assertThat(counts)
			.allSatisfy(count -> assertThat(count.created()).isCloseTo(created, within(1, SECONDS)));
	}

	@Test
	void countsEveryStateOfAWindowWithoutNamingTheBatches() {
		final var now = now(systemDefault());
		final var first = aBatch(now.minusDays(2));
		final var second = aBatch(now.minusDays(1));

		aComputer(first, "J111AAA", SENT);
		aComputer(first, "J222BBB", SENT);
		aComputer(second, "J333CCC", SENT);
		aComputer(second, "J444DDD", FAILED);

		final var counts = endOfLeaseComputerRepository.countByStatusInWindow(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO);

		assertThat(counts)
			.extracting(EndOfLeaseStatusCount::status, EndOfLeaseStatusCount::count)
			.containsExactlyInAnyOrder(
				tuple(SENT, 3L),
				tuple(FAILED, 1L));
	}

	@Test
	void leavesAnotherMunicipalitysBatchesAlone() {
		final var ours = aBatch(now(systemDefault()));
		aComputer(ours, "J777GGG", SENT);

		final var theirs = endOfLeaseBatchRepository.save(EndOfLeaseBatchEntity.create()
			.withMunicipalityId("2260")
			.withExternalBatchId(randomUUID().toString()));
		aComputer(theirs, "J888HHH", FAILED);

		assertThat(endOfLeaseComputerRepository.countByStatusInWindow(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO))
			.extracting(EndOfLeaseStatusCount::status)
			.contains(SENT)
			.doesNotContain(FAILED);

		assertThat(endOfLeaseBatchRepository.findInWindow(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO, FIRST_PAGE).getContent())
			.extracting(EndOfLeaseBatchEntity::getId)
			.contains(ours.getId())
			.doesNotContain(theirs.getId());
	}

	/**
	 * State order is the order the states are declared in and not the order their names sort in. Left to the column,
	 * the database would answer EXCLUDED, FAILED, PENDING, SENT, which is alphabetical and is not what the repository
	 * or the response schema says.
	 */
	@Test
	void readsABatchInStateOrderAndThenBySerialNumber() {
		final var batch = aBatch(now(systemDefault()));
		aComputer(batch, "K222BBB", SENT);
		aComputer(batch, "K111AAA", SENT);
		aComputer(batch, "K333CCC", FAILED);
		aComputer(batch, "K444DDD", EXCLUDED);
		aComputer(batch, "K555EEE", PENDING);

		final var computers = endOfLeaseComputerRepository.findByBatchId(batch.getId(), EnumSet.allOf(EndOfLeaseStatus.class), FIRST_PAGE);

		assertThat(computers.getContent())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getStatus)
			.containsExactly(
				tuple("K555EEE", PENDING),
				tuple("K111AAA", SENT),
				tuple("K222BBB", SENT),
				tuple("K333CCC", FAILED),
				tuple("K444DDD", EXCLUDED));
	}

	/**
	 * The states are narrowed by the query, so a page is cut out of what was asked for rather than out of the whole
	 * batch. The total counts the states that were asked for and not the batch, which is why the counts are a query of
	 * their own.
	 */
	@Test
	void readsOnlyTheStatesItIsGiven() {
		final var batch = aBatch(now(systemDefault()));
		aComputer(batch, "K111AAA", SENT);
		aComputer(batch, "K222BBB", FAILED);
		aComputer(batch, "K333CCC", FAILED);
		aComputer(batch, "K444DDD", EXCLUDED);

		final var computers = endOfLeaseComputerRepository.findByBatchId(batch.getId(), EnumSet.of(FAILED), FIRST_PAGE);

		assertThat(computers.getContent())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber)
			.containsExactly("K222BBB", "K333CCC");
		assertThat(computers.getTotalElements()).isEqualTo(2);
	}

	/**
	 * A page is cut out of the states that were asked for, and the total says how many there were to cut from. Without
	 * the filter in the query, page two of the failures would be page two of the batch.
	 */
	@Test
	void readsOnePageOfTheStatesItIsGiven() {
		final var batch = aBatch(now(systemDefault()));
		aComputer(batch, "K111AAA", FAILED);
		aComputer(batch, "K222BBB", FAILED);
		aComputer(batch, "K333CCC", FAILED);
		aComputer(batch, "K444DDD", SENT);

		final var secondPage = endOfLeaseComputerRepository.findByBatchId(batch.getId(), EnumSet.of(FAILED), PageRequest.of(1, 2));

		assertThat(secondPage.getContent())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber)
			.containsExactly("K333CCC");
		assertThat(secondPage.getTotalElements()).isEqualTo(3);
		assertThat(secondPage.getTotalPages()).isEqualTo(2);
	}

	@Test
	void findsOneBatchOfOneMunicipality() {
		final var batch = aBatch(now(systemDefault()));

		assertThat(endOfLeaseBatchRepository.findByIdAndMunicipalityId(batch.getId(), MUNICIPALITY_ID)).contains(batch);
		assertThat(endOfLeaseBatchRepository.findByIdAndMunicipalityId(batch.getId(), "2260")).isEmpty();
	}

	/**
	 * The window is what decides how many rows the counts are taken over, so a batch outside it has to be left out
	 * rather than merely sorted last.
	 */
	@Test
	void readsOnlyTheBatchesInsideTheWindow() {
		final var now = now(systemDefault());
		final var inside = aBatch(now.minusDays(3));
		final var tooOld = aBatch(now.minusMonths(6));

		aComputer(inside, "J111AAA", SENT);
		aComputer(tooOld, "J222BBB", SENT);

		final var batches = endOfLeaseBatchRepository.findInWindow(MUNICIPALITY_ID, now.minusMonths(1), now.plusDays(1), FIRST_PAGE);

		assertThat(batches.getContent())
			.extracting(EndOfLeaseBatchEntity::getId)
			.contains(inside.getId())
			.doesNotContain(tooOld.getId());
		assertThat(batches.getTotalElements()).isEqualTo(1);
	}

	/**
	 * The first moment is read and the last is not, which is what lets a caller ask for whole days without the day
	 * after leaking in.
	 */
	@Test
	void readsFromTheFirstMomentUpToButNotIncludingTheLast() {
		final var start = OffsetDateTime.parse("2026-06-01T00:00:00+02:00");
		final var onTheEdge = aBatch(start);
		final var justOutside = aBatch(start.plusDays(1));

		aComputer(onTheEdge, "J333CCC", SENT);
		aComputer(justOutside, "J444DDD", SENT);

		assertThat(endOfLeaseBatchRepository.findInWindow(MUNICIPALITY_ID, start, start.plusDays(1), FIRST_PAGE).getContent())
			.extracting(EndOfLeaseBatchEntity::getId)
			.containsExactly(onTheEdge.getId());

		assertThat(endOfLeaseComputerRepository.countByStatusInWindow(MUNICIPALITY_ID, start, start.plusDays(1)))
			.extracting(EndOfLeaseStatusCount::count)
			.containsExactly(1L);
	}

	/**
	 * The batches are paged newest first, and the total is the number of batches in the window rather than the number
	 * of rows on the page. That total is what the answer reports as the number of batches.
	 */
	@Test
	void readsTheBatchesOnePageAtATimeNewestFirst() {
		final var now = now(systemDefault());
		final var oldest = aBatch(now.minusDays(3));
		final var middle = aBatch(now.minusDays(2));
		final var newest = aBatch(now.minusDays(1));

		aComputer(oldest, "J111AAA", SENT);
		aComputer(middle, "J222BBB", SENT);
		aComputer(newest, "J333CCC", SENT);

		final var firstPage = endOfLeaseBatchRepository.findInWindow(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO, PageRequest.of(0, 2));

		assertThat(firstPage.getContent())
			.extracting(EndOfLeaseBatchEntity::getId)
			.containsExactly(newest.getId(), middle.getId());
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);

		assertThat(endOfLeaseBatchRepository.findInWindow(MUNICIPALITY_ID, EVERYTHING_FROM, EVERYTHING_TO, PageRequest.of(1, 2)).getContent())
			.extracting(EndOfLeaseBatchEntity::getId)
			.containsExactly(oldest.getId());
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
}
