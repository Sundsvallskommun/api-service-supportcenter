package se.sundsvall.supportcenter.service.scheduler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static java.time.Clock.systemUTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Proves that the lookup job holds a ShedLock row of its own, under its own name, and that the row keeps a second run
 * out while the first one is still going.
 *
 * The lock name is the one thing the two jobs must not share. Give them the same one by accident and they hold each
 * other off, so only one of the two runs at a time and the queue drains at half the rate it should, with nothing
 * failing to say so.
 *
 * Deliberately does not name the configuration class the way the other tests here do. Naming it skips the search for
 * nested configuration classes, and the mock below is declared in one.
 */
@SpringBootTest(properties = {
	"scheduler.end-of-lease.lookup.cron=* * * * * *", // Every second, so that a tick lands while the first run is still going
	// More than one thread on purpose. The default pool holds one, and the run below never returns, so without this
	// no second tick is ever attempted and the test would pass with the lock taken away entirely.
	"spring.task.scheduling.pool.size=2",
	"server.shutdown=immediate",
	"spring.lifecycle.timeout-per-shutdown-phase=0s"
})
@ActiveProfiles("junit")
class EndOfLeaseLookupSchedulerShedlockTest {

	private static final String LOCK_NAME = "end-of-lease-lookup";

	private static LocalDateTime mockCalledTime;

	@TestConfiguration
	public static class ShedlockTestConfiguration {

		@Bean
		@Primary
		public EndOfLeaseLookupWorker createMock() {
			final var mockBean = mock(EndOfLeaseLookupWorker.class);

			// Never returns, so the first run is still holding the lock when the following ticks come around. Stubbed
			// here rather than in a setup method, because the first tick lands before one would have run.
			doAnswer(invocation -> {
				mockCalledTime = LocalDateTime.now();
				await().forever().until(() -> false);
				return null;
			}).when(mockBean).processComputersAwaitingLookup();

			return mockBean;
		}
	}

	@Autowired
	private EndOfLeaseLookupWorker endOfLeaseLookupWorkerMock;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Test
	void verifyShedLock() {
		// Let the cron fire more than once.
		await().until(() -> mockCalledTime != null && LocalDateTime.now().isAfter(mockCalledTime.plusSeconds(2)));

		await().atMost(5, SECONDS)
			.untilAsserted(() -> assertThat(getLockedAt()).isCloseTo(LocalDateTime.now(systemUTC()), within(10, ChronoUnit.SECONDS)));

		// The ticks that came while the first run held the lock found nothing to do, which is the whole point of it.
		verify(endOfLeaseLookupWorkerMock).processComputersAwaitingLookup();
		verifyNoMoreInteractions(endOfLeaseLookupWorkerMock);
	}

	private LocalDateTime getLockedAt() {
		return namedParameterJdbcTemplate.query(
			"SELECT locked_at FROM shedlock WHERE name = :name",
			Map.of("name", LOCK_NAME),
			this::mapTimestamp);
	}

	private LocalDateTime mapTimestamp(final ResultSet resultSet) throws SQLException {
		if (resultSet.next()) {
			return resultSet.getTimestamp("locked_at").toLocalDateTime();
		}
		return null;
	}
}
