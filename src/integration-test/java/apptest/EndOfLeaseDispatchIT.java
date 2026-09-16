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
import se.sundsvall.supportcenter.service.scheduler.EndOfLeaseDispatchWorker;
import se.sundsvall.supportcenter.service.scheduler.EndOfLeaseLookupWorker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Both runs, one after the other, which is the only order they work in. The lookup writes the municipality and the
 * dispatch reads it to pick the installation, so a dispatch on its own would find nothing to take.
 */
@WireMockAppTestSuite(files = "classpath:/EndOfLeaseDispatch/", classes = Application.class)
@Sql(scripts = "/db/scripts/truncate.sql")
@Sql(scripts = "/db/scripts/truncate.sql", executionPhase = AFTER_TEST_METHOD)
class EndOfLeaseDispatchIT extends AbstractAppTest {

	private static final String PATH = "/2281/endOfLeaseBatches";
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EndOfLeaseLookupWorker endOfLeaseLookupWorker;

	@Autowired
	private EndOfLeaseDispatchWorker endOfLeaseDispatchWorker;

	/**
	 * The SysMan stub only matches a call naming the asset tag and the configured message, so a call carrying the serial
	 * number or another message would leave the computer unsent and fail this.
	 */
	@Test
	void test001_dispatchComputers() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();
		endOfLeaseDispatchWorker.processComputersReadyToSend();

		// Every stub was called and nothing arrived that no stub matched. Without this a run that sent the wrong body
		// would only show up indirectly, as a row that never reached SENT.
		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id", "attempts")
			.containsExactly(
				tuple("J123ABC", "SENT", "2281", 0),
				tuple("K456DEF", "EXCLUDED", null, 0));

		assertThat(jdbcTemplate.queryForObject(
			"select sent_at from end_of_lease_computer where serial_number = 'J123ABC'", Timestamp.class)).isNotNull();
	}

	/**
	 * A computer SysMan does not know about is left out of the answer rather than reported as an error, so the call
	 * succeeds and the computer still has to keep its turn.
	 */
	@Test
	void test002_dispatchComputerSysManDoesNotRecognize() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();
		endOfLeaseDispatchWorker.processComputersReadyToSend();

		// Every stub was called and nothing arrived that no stub matched. Without this a run that sent the wrong body
		// would only show up indirectly, as a row that never reached SENT.
		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id", "attempts")
			.containsExactly(
				tuple("J123ABC", "PENDING", "2281", 1),
				tuple("K456DEF", "EXCLUDED", null, 0));

		assertThat(jdbcTemplate.queryForObject(
			"select sent_at from end_of_lease_computer where serial_number = 'J123ABC'", Timestamp.class)).isNull();
	}

	/**
	 * The one test that proves the routing rather than assuming it. Each installation is stubbed to accept only its own
	 * computer name, so a mistake that sent both to the same host, or wired both clients to the same url, leaves one of
	 * them unsent and fails here. Nothing else exercises the Ange half against a real round trip.
	 */
	@Test
	void test003_dispatchComputersToBothInstallations() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();
		endOfLeaseDispatchWorker.processComputersReadyToSend();

		// Every stub was called and nothing arrived that no stub matched. Without this a run that sent the wrong body
		// would only show up indirectly, as a row that never reached SENT.
		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id")
			.containsExactly(
				tuple("J123ABC", "SENT", "2281"),
				tuple("L789GHI", "SENT", "2260"));
	}

	/**
	 * The other half of the attempt budget, end to end. An installation that answers 5xx is unwell, which is no fact
	 * about the computers in the call, so nobody pays an attempt for it. The group is held back instead and carries
	 * what SysMan said, which is the only thing a person has to go on when the queue stops moving.
	 */
	@Test
	void test004_sysManIsUnwell() throws Exception {
		final var batchId = sendBatch();

		endOfLeaseLookupWorker.processComputersAwaitingLookup();
		endOfLeaseDispatchWorker.processComputersReadyToSend();

		verifyStubs();

		assertThat(computers(batchId))
			.extracting("serial_number", "status", "asset_municipality_id", "attempts")
			.containsExactly(tuple("J123ABC", "PENDING", "2281", 0));

		assertThat(jdbcTemplate.queryForObject(
			"select error_message from end_of_lease_computer where serial_number = 'J123ABC'", String.class))
				.as("the reason SysMan gave survives the error decoder and reaches the row")
				.contains("The message service is not responding")
				.contains("the queue host refused the connection");

		assertThat(jdbcTemplate.queryForObject(
			"select retry_after from end_of_lease_computer where serial_number = 'J123ABC'", Timestamp.class)).isNotNull();
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
