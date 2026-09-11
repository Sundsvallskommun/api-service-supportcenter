package se.sundsvall.supportcenter.service.scheduler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Proves that the ShedLock row keeps a second run out while the first one is still going, which is what stops a
 * computer from being reported to SysMan once per instance.
 *
 * Deliberately does not name the configuration class the way the other tests here do. Naming it skips the search for
 * nested configuration classes, and the mock below is declared in one.
 */
@SpringBootTest(properties = {
	"scheduler.end-of-lease.cron=* * * * * *", // Every second, so that a tick lands while the first run is still going
	"server.shutdown=immediate",
	"spring.lifecycle.timeout-per-shutdown-phase=0s"
})
@ActiveProfiles("junit")
class EndOfLeaseSchedulerShedlockTest {

	private static final String LOCK_NAME = "end-of-lease";

	private static LocalDateTime mockCalledTime;

	@TestConfiguration
	public static class ShedlockTestConfiguration {

		@Bean
		@Primary
		public EndOfLeaseSchedulerWorker createMock() {
			final var mockBean = Mockito.mock(EndOfLeaseSchedulerWorker.class);

			// Never returns, so the first run is still holding the lock when the following ticks come around. Stubbed
			// here rather than in a setup method, because the first tick lands before one would have run.
			doAnswer(invocation -> {
				mockCalledTime = LocalDateTime.now();
				await().forever().until(() -> false);
				return null;
			}).when(mockBean).processPendingComputers();

			return mockBean;
		}
	}

	@Autowired
	private EndOfLeaseSchedulerWorker endOfLeaseSchedulerWorkerMock;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Test
	void verifyShedLock() {
		// Let the cron fire more than once.
		await().until(() -> mockCalledTime != null && LocalDateTime.now().isAfter(mockCalledTime.plusSeconds(2)));

		await().atMost(5, SECONDS)
			.untilAsserted(() -> assertThat(getLockedAt()).isCloseTo(LocalDateTime.now(systemUTC()), within(10, ChronoUnit.SECONDS)));

		// The ticks that came while the first run held the lock found nothing to do, which is the whole point of it.
		verify(endOfLeaseSchedulerWorkerMock).processPendingComputers();
		verifyNoMoreInteractions(endOfLeaseSchedulerWorkerMock);
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
