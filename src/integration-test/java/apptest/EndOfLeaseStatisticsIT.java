package apptest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatusResponse;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;

/**
 * What the statistics answer is the state of the rows, which is why both endpoints are asserted against rows that were
 * put in the states the two runs leave behind rather than against a mocked service.
 *
 * The suite stubs nothing, and the .gitkeep under __files keeps the warning about a missing WireMock root out of the
 * build output.
 */
@WireMockAppTestSuite(files = "classpath:/EndOfLeaseStatistics/", classes = Application.class)
@Sql(scripts = "/db/scripts/truncate.sql")
@Sql(scripts = "/db/scripts/truncate.sql", executionPhase = AFTER_TEST_METHOD)
class EndOfLeaseStatisticsIT extends AbstractAppTest {

	private static final String STATISTICS_PATH = "/2281/endOfLeaseStatistics";
	private static final String BATCH_PATH = "/2281/endOfLeaseBatches/";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test001_statisticsOverEveryBatch() throws Exception {
		final var now = OffsetDateTime.now();
		final var older = givenABatch("2281", now.minusDays(2));
		final var newer = givenABatch("2281", now.minusDays(1));

		givenAComputer(older, "J111AAA", SENT.name(), 1, null);
		givenAComputer(older, "J222BBB", SENT.name(), 2, null);
		givenAComputer(older, "J333CCC", FAILED.name(), 5, "POB is unwell");
		givenAComputer(newer, "J444DDD", PENDING.name(), 0, null);
		givenAComputer(newer, "J555EEE", EXCLUDED.name(), 0, null);

		final var statistics = setupCall()
			.withServicePath(STATISTICS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		assertThat(statistics.getMetadata().getTotalRecords()).isEqualTo(2);
		assertThat(statistics.getCounts().getTotal()).isEqualTo(5);
		assertThat(statistics.getCounts().getPending()).isEqualTo(1);
		assertThat(statistics.getCounts().getSent()).isEqualTo(2);
		assertThat(statistics.getCounts().getFailed()).isEqualTo(1);
		assertThat(statistics.getCounts().getExcluded()).isEqualTo(1);

		assertThat(statistics.getBatches())
			.as("the newest batch is answered first")
			.extracting("id", "counts.total", "counts.pending", "counts.sent", "counts.failed", "counts.excluded")
			.containsExactly(
				tuple(newer, 2L, 1L, 0L, 0L, 1L),
				tuple(older, 3L, 0L, 2L, 1L, 0L));
	}

	/**
	 * The municipality in the path is the sender's, and one sender's numbers must not carry another sender's batches.
	 */
	@Test
	void test002_anotherMunicipalitysBatchesAreLeftOut() throws Exception {
		final var ours = givenABatch("2281", OffsetDateTime.now());
		givenAComputer(ours, "J123ABC", SENT.name(), 1, null);

		final var theirs = givenABatch("2260", OffsetDateTime.now());
		givenAComputer(theirs, "K456DEF", SENT.name(), 1, null);

		final var statistics = setupCall()
			.withServicePath(STATISTICS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		assertThat(statistics.getMetadata().getTotalRecords()).isEqualTo(1);
		assertThat(statistics.getCounts().getTotal()).isEqualTo(1);
		assertThat(statistics.getBatches()).extracting("id").containsExactly(ours);
	}

	/**
	 * A municipality that has registered nothing is answered with zeroes rather than with nothing at all, so that the
	 * answer alone says the queue is empty.
	 */
	@Test
	void test003_statisticsWithoutASingleBatch() throws Exception {
		final var statistics = setupCall()
			.withServicePath(STATISTICS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		assertThat(statistics.getMetadata().getTotalRecords()).isZero();
		assertThat(statistics.getCounts().getTotal()).isZero();
		assertThat(statistics.getBatches()).isEmpty();
	}

	@Test
	void test004_readOneBatchWithEveryComputerInIt() throws Exception {
		final var batchId = givenABatch("2281", OffsetDateTime.now());

		givenAComputer(batchId, "J222BBB", SENT.name(), 3, null);
		givenAComputer(batchId, "J111AAA", FAILED.name(), 5, "Gave up on serial number J111AAA after 5 attempts: POB is unwell");
		givenAComputer(batchId, "J333CCC", EXCLUDED.name(), 0, null);

		final var batch = setupCall()
			.withServicePath(BATCH_PATH + batchId)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchStatusResponse.class);

		assertThat(batch.getId()).isEqualTo(batchId);
		assertThat(batch.getCounts().getTotal()).isEqualTo(3);
		assertThat(batch.getCounts().getSent()).isEqualTo(1);
		assertThat(batch.getCounts().getFailed()).isEqualTo(1);
		assertThat(batch.getCounts().getExcluded()).isEqualTo(1);
		assertThat(batch.getCounts().getPending()).isZero();
		assertThat(batch.getMetadata().getTotalRecords()).isEqualTo(3);

		assertThat(batch.getComputers())
			.as("the states come in the order they are declared in and not the order their names sort in")
			.extracting("serialNumber", "status", "attempts", "errorMessage")
			.containsExactly(
				tuple("J222BBB", SENT.name(), 3, null),
				tuple("J111AAA", FAILED.name(), 5, "Gave up on serial number J111AAA after 5 attempts: POB is unwell"),
				tuple("J333CCC", EXCLUDED.name(), 0, null));
	}

	/**
	 * Holding the id of another sender's batch is not enough to read it, so it is answered the same way as a batch that
	 * does not exist.
	 */
	@Test
	void test005_readABatchAnotherMunicipalityRegistered() throws Exception {
		final var theirs = givenABatch("2260", OffsetDateTime.now());
		givenAComputer(theirs, "J123ABC", SENT.name(), 1, null);

		setupCall()
			.withServicePath(BATCH_PATH + theirs)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequest();
	}

	private String givenABatch(final String municipalityId, final OffsetDateTime created) {
		final var batchId = UUID.randomUUID().toString();

		jdbcTemplate.update(
			"insert into end_of_lease_batch (id, external_batch_id, municipality_id, created) values (?, ?, ?, ?)",
			batchId, UUID.randomUUID().toString(), municipalityId, created);

		return batchId;
	}

	private void givenAComputer(final String batchId, final String serialNumber, final String status, final int attempts, final String errorMessage) {
		jdbcTemplate.update(
			"insert into end_of_lease_computer (id, batch_id, serial_number, asset_tag, end_of_lease_date, status, attempts, error_message, created) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
			UUID.randomUUID().toString(), batchId, serialNumber, "AB" + serialNumber, LocalDate.of(2026, 11, 30), status, attempts, errorMessage, OffsetDateTime.now());
	}

	/**
	 * The states asked for decide what is listed. The counts keep covering the whole batch, which is what makes one
	 * failure out of three readable as that rather than as the whole batch.
	 */
	@Test
	void test006_readOnlyTheComputersThatFailed() throws Exception {
		final var batchId = givenABatch("2281", OffsetDateTime.now());

		givenAComputer(batchId, "J111AAA", FAILED.name(), 5, "POB is unwell");
		givenAComputer(batchId, "J222BBB", SENT.name(), 1, null);
		givenAComputer(batchId, "J333CCC", PENDING.name(), 0, null);

		final var batch = setupCall()
			.withServicePath(BATCH_PATH + batchId + "?status=FAILED")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchStatusResponse.class);

		assertThat(batch.getComputers())
			.extracting("serialNumber", "status", "errorMessage")
			.containsExactly(tuple("J111AAA", FAILED.name(), "POB is unwell"));

		assertThat(batch.getCounts().getTotal())
			.as("the counts cover the whole batch, not the states that were asked for")
			.isEqualTo(3);
		assertThat(batch.getCounts().getFailed()).isEqualTo(1);
		assertThat(batch.getCounts().getSent()).isEqualTo(1);
		assertThat(batch.getCounts().getPending()).isEqualTo(1);
		assertThat(batch.getMetadata().getTotalRecords())
			.as("the page is cut out of the states that were asked for")
			.isEqualTo(1);
	}

	@Test
	void test007_readTheComputersInEitherOfTwoStates() throws Exception {
		final var batchId = givenABatch("2281", OffsetDateTime.now());

		givenAComputer(batchId, "J111AAA", FAILED.name(), 5, "POB is unwell");
		givenAComputer(batchId, "J222BBB", SENT.name(), 1, null);
		givenAComputer(batchId, "J333CCC", PENDING.name(), 0, null);

		final var batch = setupCall()
			.withServicePath(BATCH_PATH + batchId + "?status=FAILED&status=PENDING")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchStatusResponse.class);

		assertThat(batch.getComputers())
			.as("PENDING is declared before FAILED, so it comes first however the two names sort")
			.extracting("serialNumber")
			.containsExactly("J333CCC", "J111AAA");
	}

	/**
	 * Without a window the counts reach a month back, which is what keeps the read from growing with everything the
	 * municipality has ever registered. A batch older than that is not gone, it is behind a window the caller has to ask
	 * for.
	 */
	@Test
	void test008_aBatchOlderThanAMonthIsBehindTheWindow() throws Exception {
		final var now = OffsetDateTime.now();
		final var recent = givenABatch("2281", now.minusDays(3));
		final var old = givenABatch("2281", now.minusMonths(6));

		givenAComputer(recent, "J111AAA", SENT.name(), 1, null);
		givenAComputer(old, "J222BBB", SENT.name(), 1, null);

		final var byDefault = setupCall()
			.withServicePath(STATISTICS_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		final var today = LocalDate.now(ZoneId.systemDefault());
		assertThat(byDefault.getFrom()).isEqualTo(today.minusMonths(1));
		assertThat(byDefault.getTo()).isEqualTo(today);
		assertThat(byDefault.getMetadata().getTotalRecords()).isEqualTo(1);
		assertThat(byDefault.getBatches()).extracting("id").containsExactly(recent);

		final var widened = setupCall()
			.withServicePath(STATISTICS_PATH + "?from=" + today.minusYears(1))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		assertThat(widened.getMetadata().getTotalRecords()).isEqualTo(2);
		assertThat(widened.getCounts().getTotal()).isEqualTo(2);
		assertThat(widened.getBatches()).extracting("id").containsExactly(recent, old);
	}

	@Test
	void test009_aWindowThatEndsBeforeItStarts() throws Exception {
		setupCall()
			.withServicePath(STATISTICS_PATH + "?from=2026-09-18&to=2026-08-18")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();
	}

	/**
	 * A batch comes back one page at a time, and the counts stay over the whole of it however small the page is. A page
	 * past the last one is empty rather than an error, which is what a caller walking the pages runs into at the end.
	 */
	@Test
	void test010_readOneBatchOnePageAtATime() throws Exception {
		final var batchId = givenABatch("2281", OffsetDateTime.now());

		givenAComputer(batchId, "J111AAA", PENDING.name(), 0, null);
		givenAComputer(batchId, "J222BBB", SENT.name(), 1, null);
		givenAComputer(batchId, "J333CCC", FAILED.name(), 5, "POB is unwell");

		final var firstPage = setupCall()
			.withServicePath(BATCH_PATH + batchId + "?page=1&limit=2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchStatusResponse.class);

		assertThat(firstPage.getComputers()).extracting("serialNumber").containsExactly("J111AAA", "J222BBB");
		assertThat(firstPage.getCounts().getTotal()).isEqualTo(3);
		assertThat(firstPage.getMetadata().getPage()).isEqualTo(1);
		assertThat(firstPage.getMetadata().getLimit()).isEqualTo(2);
		assertThat(firstPage.getMetadata().getCount()).isEqualTo(2);
		assertThat(firstPage.getMetadata().getTotalRecords()).isEqualTo(3);
		assertThat(firstPage.getMetadata().getTotalPages()).isEqualTo(2);

		final var secondPage = setupCall()
			.withServicePath(BATCH_PATH + batchId + "?page=2&limit=2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchStatusResponse.class);

		assertThat(secondPage.getComputers()).extracting("serialNumber").containsExactly("J333CCC");
		assertThat(secondPage.getCounts().getTotal()).isEqualTo(3);

		final var pastTheEnd = setupCall()
			.withServicePath(BATCH_PATH + batchId + "?page=9&limit=2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchStatusResponse.class);

		assertThat(pastTheEnd.getComputers()).isEmpty();
		assertThat(pastTheEnd.getCounts().getTotal()).isEqualTo(3);
	}

	/**
	 * The batches are paged and the window counts are not. A reader on the second page is still told what the whole
	 * window holds, which is what a page of one batch cannot say on its own.
	 */
	@Test
	void test011_readTheStatisticsOnePageAtATime() throws Exception {
		final var now = OffsetDateTime.now();
		final var oldest = givenABatch("2281", now.minusDays(3));
		final var middle = givenABatch("2281", now.minusDays(2));
		final var newest = givenABatch("2281", now.minusDays(1));

		givenAComputer(oldest, "J111AAA", SENT.name(), 1, null);
		givenAComputer(middle, "J222BBB", SENT.name(), 1, null);
		givenAComputer(newest, "J333CCC", FAILED.name(), 5, "POB is unwell");

		final var firstPage = setupCall()
			.withServicePath(STATISTICS_PATH + "?page=1&limit=2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		assertThat(firstPage.getBatches()).extracting("id").containsExactly(newest, middle);
		assertThat(firstPage.getMetadata().getTotalRecords()).isEqualTo(3);
		assertThat(firstPage.getMetadata().getTotalPages()).isEqualTo(2);
		assertThat(firstPage.getCounts().getTotal())
			.as("the counts cover the window and not the page")
			.isEqualTo(3);
		assertThat(firstPage.getCounts().getFailed()).isEqualTo(1);

		final var secondPage = setupCall()
			.withServicePath(STATISTICS_PATH + "?page=2&limit=2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(EndOfLeaseStatisticsResponse.class);

		assertThat(secondPage.getBatches()).extracting("id").containsExactly(oldest);
		assertThat(secondPage.getBatches()).extracting("counts.total").containsExactly(1L);
		assertThat(secondPage.getCounts().getTotal()).isEqualTo(3);
	}

	/**
	 * The limit is capped by dept44.models.api.paging.max.limit, which is what stops a single call from asking for a
	 * whole batch and getting back what paging was added to stop.
	 */
	@Test
	void test012_aLimitAboveTheCap() throws Exception {
		final var batchId = givenABatch("2281", OffsetDateTime.now());
		givenAComputer(batchId, "J111AAA", SENT.name(), 1, null);

		setupCall()
			.withServicePath(BATCH_PATH + batchId + "?limit=201")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		setupCall()
			.withServicePath(STATISTICS_PATH + "?limit=201")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();
	}
}
