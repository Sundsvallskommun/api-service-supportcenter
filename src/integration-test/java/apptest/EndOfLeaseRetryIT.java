package apptest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * FAILED is where a computer ends up once its attempts are gone, and neither run can take it out again. This is the
 * only way back, so it is asserted against the database rather than against the answer alone.
 *
 * The suite stubs nothing, but its directory still has to exist for the WireMock root to resolve.
 */
@WireMockAppTestSuite(files = "classpath:/EndOfLeaseRetry/", classes = Application.class)
@Sql(scripts = "/db/scripts/truncate.sql")
@Sql(scripts = "/db/scripts/truncate.sql", executionPhase = AFTER_TEST_METHOD)
class EndOfLeaseRetryIT extends AbstractAppTest {

	private static final String PATH = "/2281/endOfLeaseComputers/retry";
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test001_retryEveryComputerGivenUpOn() throws Exception {
		givenAComputerGivenUpOn("J123ABC", "2281");
		givenAComputerGivenUpOn("K456DEF", "2281");

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(computers())
			.extracting("serial_number", "status", "attempts", "error_message")
			.containsExactly(
				tuple("J123ABC", "PENDING", 0, null),
				tuple("K456DEF", "PENDING", 0, null));
	}

	@Test
	void test002_retryANamedComputer() throws Exception {
		givenAComputerGivenUpOn("J123ABC", "2281");
		givenAComputerGivenUpOn("K456DEF", "2281");

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(computers())
			.extracting("serial_number", "status", "attempts")
			.containsExactly(
				tuple("J123ABC", "PENDING", 0),
				tuple("K456DEF", "FAILED", 5));
	}

	/**
	 * The municipality in the path is the sender's, and a sender must not be handed another sender's computers back.
	 */
	@Test
	void test003_anotherMunicipalitysComputerIsLeftAlone() throws Exception {
		givenAComputerGivenUpOn("J123ABC", "2281");
		givenAComputerGivenUpOn("L789GHI", "2260");

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		assertThat(computers())
			.extracting("serial_number", "status")
			.containsExactly(
				tuple("J123ABC", "PENDING"),
				tuple("L789GHI", "FAILED"));
	}

	private List<Map<String, Object>> computers() {
		return jdbcTemplate.queryForList(
			"select serial_number, status, attempts, error_message from end_of_lease_computer order by serial_number");
	}

	private void givenAComputerGivenUpOn(final String serialNumber, final String municipalityId) {
		final var batchId = UUID.randomUUID().toString();

		jdbcTemplate.update(
			"insert into end_of_lease_batch (id, external_batch_id, municipality_id, created) values (?, ?, ?, ?)",
			batchId, UUID.randomUUID().toString(), municipalityId, OffsetDateTime.now());

		jdbcTemplate.update(
			"insert into end_of_lease_computer (id, batch_id, serial_number, asset_tag, end_of_lease_date, status, attempts, error_message, created) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
			UUID.randomUUID().toString(), batchId, serialNumber, "AB12345", LocalDate.of(2026, 11, 30), "FAILED", 5,
			"Gave up on serial number " + serialNumber, OffsetDateTime.now());
	}
}
