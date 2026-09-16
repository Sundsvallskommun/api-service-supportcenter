package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The queue reaching the health endpoint is the whole point of the indicator, so it is asserted where the endpoint
 * actually answers rather than only against the bean.
 *
 * The suite stubs nothing. Its directory does not have to exist for the suite to run, since the WireMock factory
 * answers a missing resource with a warning and falls back on the annotation, but the .gitkeep under __files keeps that
 * warning out of the build output.
 */
@WireMockAppTestSuite(files = "classpath:/EndOfLeaseQueueHealth/", classes = Application.class)
@Sql(scripts = "/db/scripts/truncate.sql")
@Sql(scripts = "/db/scripts/truncate.sql", executionPhase = AFTER_TEST_METHOD)
class EndOfLeaseQueueHealthIT extends AbstractAppTest {

	private static final String COMPONENT = "endOfLeaseQueue";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void test001_anEmptyQueueIsUp() {
		final var component = healthComponent();

		assertThat(component.path("status").asString()).isEqualTo("UP");
		assertThat(component.path("details").path("Reason").asString()).isEqualTo("No computer is waiting");
	}

	/**
	 * Neither run leaves a computer FAILED over an outage, so a queue that stops moving is the only sign of one. The
	 * endpoint still answers 200, since an ageing queue is something to look at and not a reason to take the instance
	 * out of rotation.
	 */
	@Test
	void test002_aQueueStandingStillIsRestricted() {
		givenAComputerWaitingSince(OffsetDateTime.now().minusHours(30));

		final var component = healthComponent();

		assertThat(component.path("status").asString()).isEqualTo("RESTRICTED");
		assertThat(component.path("details").path("Oldest waiting serial number").asString()).isEqualTo("J123ABC");
		assertThat(component.path("details").path("Reason").asString()).contains("has been waiting");
	}

	/**
	 * The outcome a person is meant to act on. A computer nobody can report stays counted until somebody deals with it,
	 * so this is the one that has to reach the endpoint.
	 */
	@Test
	void test003_aComputerGivenUpOnIsRestricted() {
		givenAComputerGivenUpOn();

		final var component = healthComponent();

		assertThat(component.path("status").asString()).isEqualTo("RESTRICTED");
		assertThat(component.path("details").path("Given up on").asLong()).isEqualTo(1L);
		assertThat(component.path("details").path("Reason").asString()).contains("given up on");
	}

	private JsonNode healthComponent() {
		final var response = restTemplate.getForEntity("/actuator/health", String.class);

		assertThat(response.getStatusCode()).isEqualTo(OK);

		return JsonMapper.builder().build()
			.readTree(response.getBody())
			.path("components")
			.path(COMPONENT);
	}

	private void givenAComputerGivenUpOn() {
		final var batchId = UUID.randomUUID().toString();
		final var created = OffsetDateTime.now().minusHours(1);

		jdbcTemplate.update(
			"insert into end_of_lease_batch (id, external_batch_id, municipality_id, created) values (?, ?, ?, ?)",
			batchId, UUID.randomUUID().toString(), "2281", created);

		jdbcTemplate.update(
			"insert into end_of_lease_computer (id, batch_id, serial_number, asset_tag, end_of_lease_date, status, attempts, created) values (?, ?, ?, ?, ?, ?, ?, ?)",
			UUID.randomUUID().toString(), batchId, "J999ZZZ", "AB99999", LocalDate.of(2026, 11, 30), "FAILED", 5, created);
	}

	private void givenAComputerWaitingSince(final OffsetDateTime created) {
		final var batchId = UUID.randomUUID().toString();

		jdbcTemplate.update(
			"insert into end_of_lease_batch (id, external_batch_id, municipality_id, created) values (?, ?, ?, ?)",
			batchId, UUID.randomUUID().toString(), "2281", created);

		jdbcTemplate.update(
			"insert into end_of_lease_computer (id, batch_id, serial_number, asset_tag, end_of_lease_date, status, attempts, created) values (?, ?, ?, ?, ?, ?, ?, ?)",
			UUID.randomUUID().toString(), batchId, "J123ABC", "AB12345", LocalDate.of(2026, 11, 30), "PENDING", 0, created);
	}
}
