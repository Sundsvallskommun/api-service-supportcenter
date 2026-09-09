package apptest;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchResponse;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

@WireMockAppTestSuite(files = "classpath:/CreateEndOfLeaseBatch/", classes = Application.class)
@Sql(scripts = "/db/scripts/truncate.sql")
@Sql(scripts = "/db/scripts/truncate.sql", executionPhase = AFTER_TEST_METHOD)
class CreateEndOfLeaseBatchIT extends AbstractAppTest {

	private static final String PATH = "/2281/endOfLeaseBatches";
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EndOfLeaseBatchRepository endOfLeaseBatchRepository;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Test
	void test001_createEndOfLeaseBatch() throws Exception {

		final var batchId = sendBatch();

		assertThat(getResponseHeaders().getFirst(LOCATION)).isEqualTo(PATH + "/" + batchId);

		final var batch = jdbcTemplate.queryForMap("select external_batch_id, municipality_id, created from end_of_lease_batch where id = ?", batchId);
		assertThat(batch).containsEntry("external_batch_id", "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90");
		assertThat(batch).containsEntry("municipality_id", "2281");
		assertThat(batch.get("created")).isNotNull();

		final var computers = jdbcTemplate.queryForList(
			"select serial_number, asset_tag, end_of_lease_date, status, attempts, asset_municipality_id, sent_at from end_of_lease_computer where batch_id = ? order by serial_number", batchId);

		assertThat(computers)
			.extracting(row -> row.get("serial_number"), row -> row.get("asset_tag"), row -> ((Date) row.get("end_of_lease_date")).toLocalDate(), row -> row.get("status"), row -> row.get("attempts"))
			.containsExactly(
				tuple("J123ABC", "AB12345", LocalDate.of(2026, 11, 30), "PENDING", 0),
				tuple("K456DEF", "PUB16604", LocalDate.of(2026, 12, 31), "PENDING", 0));

		assertThat(computers).allSatisfy(row -> {
			assertThat(row.get("asset_municipality_id")).isNull();
			assertThat(row.get("sent_at")).isNull();
		});
	}

	@Test
	void test002_createEndOfLeaseBatchThatIsSentAgain() throws Exception {

		final var batchId = sendBatch();
		final var batchIdFromResend = sendBatch();

		assertThat(batchIdFromResend).isEqualTo(batchId);
		assertThat(jdbcTemplate.queryForObject("select count(*) from end_of_lease_batch", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from end_of_lease_computer", Integer.class)).isOne();
	}

	/**
	 * A batch losing on the unique index is an expected outcome, not the database being unwell. Counted as a failure it
	 * opens the repository's circuit breaker after a handful of resends, and every sender then gets a 500 for as long as
	 * the breaker stays open. Driving the repository directly is what proves the exception is ignored, since going
	 * through the endpoint returns on the lookup before the insert is ever attempted.
	 */
	@Test
	void test003_duplicateBatchesDoNotOpenTheCircuitBreaker() {

		final var circuitBreaker = circuitBreakerRegistry.circuitBreaker("endOfLeaseBatchRepository");
		final var failedCallsBefore = circuitBreaker.getMetrics().getNumberOfFailedCalls();
		final var successfulCallsBefore = circuitBreaker.getMetrics().getNumberOfSuccessfulCalls();

		endOfLeaseBatchRepository.save(batch("2281", "e8a1c4d7-5b3e-4f9a-8c2d-6e1f3a5b7c9d"));

		for (var attempt = 0; attempt < 6; attempt++) {
			assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> endOfLeaseBatchRepository.save(batch("2281", "e8a1c4d7-5b3e-4f9a-8c2d-6e1f3a5b7c9d")));
		}

		// Asserting that the successful calls were counted too, since a breaker that never advises the repository has all
		// its metrics sitting at zero and would satisfy the failure assertion without proving anything.
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isGreaterThan(successfulCallsBefore);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(failedCallsBefore);
		assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);

		assertThat(endOfLeaseBatchRepository.save(batch("2281", "f9b2d5e8-6c4f-4a0b-9d3e-7f2a4b6c8d0e")).getId()).isNotBlank();
	}

	/**
	 * The id the sender gave the batch is unique per sender, so the same id sent by another municipality is another
	 * batch and must be stored rather than answered with someone else's id.
	 */
	@Test
	void test004_theSameExternalBatchIdFromAnotherMunicipalityIsItsOwnBatch() {

		final var first = endOfLeaseBatchRepository.save(batch("2281", "a4c7e9b1-2d5f-4a8c-9e1b-3d6f8a0c2e4b"));
		final var second = endOfLeaseBatchRepository.save(batch("2260", "a4c7e9b1-2d5f-4a8c-9e1b-3d6f8a0c2e4b"));

		assertThat(second.getId()).isNotEqualTo(first.getId());
		assertThat(jdbcTemplate.queryForObject("select count(*) from end_of_lease_batch", Integer.class)).isEqualTo(2);
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

	private static EndOfLeaseBatchEntity batch(final String municipalityId, final String externalBatchId) {
		final var endOfLeaseBatchEntity = EndOfLeaseBatchEntity.create()
			.withMunicipalityId(municipalityId)
			.withExternalBatchId(externalBatchId);

		return endOfLeaseBatchEntity.withComputers(List.of("J123ABC").stream()
			.map(serialNumber -> EndOfLeaseComputerEntity.create()
				.withBatch(endOfLeaseBatchEntity)
				.withSerialNumber(serialNumber)
				.withAssetTag("AB12345")
				.withEndOfLeaseDate(LocalDate.of(2026, 11, 30))
				.withStatus(PENDING))
			.collect(toCollection(ArrayList::new)));
	}
}
