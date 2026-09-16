package apptest;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchResponse;
import se.sundsvall.supportcenter.service.scheduler.EndOfLeaseLookupWorker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * The lookup run, driven directly rather than waited for. The cron is off under test, so the job is called here the way
 * the scheduler would call it.
 */
@WireMockAppTestSuite(files = "classpath:/EndOfLeaseLookup/", classes = Application.class)
@Sql(scripts = "/db/scripts/truncate.sql")
@Sql(scripts = "/db/scripts/truncate.sql", executionPhase = AFTER_TEST_METHOD)
class EndOfLeaseLookupIT extends AbstractAppTest {

	private static final String PATH = "/2281/endOfLeaseBatches";
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EndOfLeaseLookupWorker endOfLeaseLookupWorker;

	/**
	 * POB answers with the municipality spelled as a name, and the row has to end up holding the id, since that is what
	 * the SysMan routing is keyed on.
	 *
	 * The excluded computer is in the batch on purpose. It is never looked up, so POB is stubbed for one serial number
	 * only and a call for the other would not match anything.
	 */
	@Test
	void test001_lookUpComputers() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		// Every stub was called and nothing arrived that no stub matched. Without this a run that asked POB the wrong
		// question would only show up indirectly, as a row that never got its municipality.
		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id", "attempts")
			.containsExactly(
				tuple("J123ABC", "PENDING", "2281", 0),
				tuple("K456DEF", "EXCLUDED", null, 0));
	}

	/**
	 * A computer POB knows nothing about keeps its turn rather than failing outright, since a missing configuration item
	 * is something someone can put right while the attempts last.
	 */
	@Test
	void test002_lookUpComputerPobKnowsNothingAbout() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		// Every stub was called and nothing arrived that no stub matched. Without this a run that asked POB the wrong
		// question would only show up indirectly, as a row that never got its municipality.
		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id", "attempts")
			.containsExactly(
				tuple("J123ABC", "PENDING", null, 1),
				tuple("K456DEF", "EXCLUDED", null, 0));

		assertThat(jdbcTemplate.queryForObject(
			"select error_message from end_of_lease_computer where serial_number = 'J123ABC'", String.class)).isNotBlank();
	}

	/**
	 * The half of the attempt budget that is otherwise proven with mocks alone. A POB that answers 5xx is the other end
	 * being unwell, which is no fact about this computer, so the attempt is not spent. The row is held back instead, and
	 * carries the reason so that a queue standing still can be read off it.
	 */
	@Test
	void test003_pobIsUnwell() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();

		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id", "attempts")
			.containsExactly(tuple("J123ABC", "PENDING", null, 0));

		assertThat(jdbcTemplate.queryForObject(
			"select error_message from end_of_lease_computer where serial_number = 'J123ABC'", String.class))
				.contains("POB could not be reached");

		assertThat(jdbcTemplate.queryForObject(
			"select retry_after from end_of_lease_computer where serial_number = 'J123ABC'", Timestamp.class))
				.as("held back, or it keeps its place at the front of every page while POB is down")
				.isNotNull();
	}

	private String sendBatch() throws Exception {
		return setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(EndOfLeaseBatchResponse.class)
			.getId();
	}

	private List<Map<String, Object>> computers(final String batchId) {
		return jdbcTemplate.queryForList(
			"select serial_number, status, asset_municipality_id, attempts from end_of_lease_computer where batch_id = ? order by serial_number", batchId);
	}
}
