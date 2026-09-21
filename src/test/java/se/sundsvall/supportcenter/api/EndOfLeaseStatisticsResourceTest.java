package se.sundsvall.supportcenter.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatistics;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerCounts;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsResponse;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseStatisticsResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String BATCH_ID = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c";
	private static final LocalDate FROM = LocalDate.of(2026, 8, 18);
	private static final LocalDate TO = LocalDate.of(2026, 9, 18);

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getEndOfLeaseStatistics() {

		when(endOfLeaseServiceMock.getStatistics(MUNICIPALITY_ID, null, null)).thenReturn(EndOfLeaseStatisticsResponse.create()
			.withFrom(FROM)
			.withTo(TO)
			.withBatches(1)
			.withComputers(EndOfLeaseComputerCounts.create()
				.withTotal(982)
				.withPending(0)
				.withSent(961)
				.withFailed(3)
				.withExcluded(18))
			.withPerBatch(List.of(EndOfLeaseBatchStatistics.create()
				.withId(BATCH_ID)
				.withExternalBatchId("d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90")
				.withCreated(OffsetDateTime.parse("2026-09-17T06:03:11+02:00"))
				.withTotal(982)
				.withSent(961)
				.withFailed(3)
				.withExcluded(18))));

		webTestClient.get().uri("/{municipalityId}/endOfLeaseStatistics", MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.from").isEqualTo("2026-08-18")
			.jsonPath("$.to").isEqualTo("2026-09-18")
			.jsonPath("$.batches").isEqualTo(1)
			.jsonPath("$.computers.total").isEqualTo(982)
			.jsonPath("$.computers.sent").isEqualTo(961)
			.jsonPath("$.computers.failed").isEqualTo(3)
			.jsonPath("$.computers.excluded").isEqualTo(18)
			.jsonPath("$.perBatch[0].id").isEqualTo(BATCH_ID)
			.jsonPath("$.perBatch[0].total").isEqualTo(982);

		verify(endOfLeaseServiceMock).getStatistics(MUNICIPALITY_ID, null, null);
	}

	/**
	 * A municipality that has never registered a batch is answered with zeroes rather than with nothing, so that a
	 * reader can tell the two apart from the answer alone.
	 */
	@Test
	void getEndOfLeaseStatisticsForAMunicipalityWithNoBatches() {

		when(endOfLeaseServiceMock.getStatistics(MUNICIPALITY_ID, null, null)).thenReturn(EndOfLeaseStatisticsResponse.create()
			.withFrom(FROM)
			.withTo(TO)
			.withBatches(0)
			.withComputers(EndOfLeaseComputerCounts.create())
			.withPerBatch(List.of()));

		webTestClient.get().uri("/{municipalityId}/endOfLeaseStatistics", MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.batches").isEqualTo(0)
			.jsonPath("$.computers.total").isEqualTo(0)
			.jsonPath("$.perBatch").isEmpty();

		verify(endOfLeaseServiceMock).getStatistics(MUNICIPALITY_ID, null, null);
	}

	/**
	 * The window is what a caller reaches for to look further back than the month the endpoint answers with on its own.
	 */
	@Test
	void getEndOfLeaseStatisticsForAWindow() {

		when(endOfLeaseServiceMock.getStatistics(MUNICIPALITY_ID, FROM, TO)).thenReturn(EndOfLeaseStatisticsResponse.create()
			.withFrom(FROM)
			.withTo(TO)
			.withBatches(30)
			.withComputers(EndOfLeaseComputerCounts.create().withTotal(29460))
			.withPerBatch(List.of()));

		webTestClient.get()
			.uri(builder -> builder.path("/{municipalityId}/endOfLeaseStatistics").queryParam("from", "2026-08-18").queryParam("to", "2026-09-18").build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.from").isEqualTo("2026-08-18")
			.jsonPath("$.to").isEqualTo("2026-09-18")
			.jsonPath("$.batches").isEqualTo(30);

		verify(endOfLeaseServiceMock).getStatistics(MUNICIPALITY_ID, FROM, TO);
	}

	@Test
	void getEndOfLeaseStatisticsFromOneDayOnwards() {

		when(endOfLeaseServiceMock.getStatistics(MUNICIPALITY_ID, FROM, null)).thenReturn(EndOfLeaseStatisticsResponse.create().withFrom(FROM).withTo(TO));

		webTestClient.get().uri(builder -> builder.path("/{municipalityId}/endOfLeaseStatistics").queryParam("from", "2026-08-18").build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk();

		verify(endOfLeaseServiceMock).getStatistics(MUNICIPALITY_ID, FROM, null);
	}
}
