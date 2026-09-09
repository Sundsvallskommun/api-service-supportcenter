package se.sundsvall.supportcenter.api;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseBatchResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String BATCH_ID = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c";
	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createEndOfLeaseBatch() {

		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(
				EndOfLeaseComputer.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30)),
				EndOfLeaseComputer.create()
					.withSerialNumber("K456DEF")
					.withAssetTag("PUB16604")
					.withEndOfLeaseDate(LocalDate.of(2026, 11, 30))));

		when(endOfLeaseServiceMock.registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest)).thenReturn(BATCH_ID);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseBatches", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(createEndOfLeaseBatchRequest)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/2281/endOfLeaseBatches/" + BATCH_ID)
			.expectBody().jsonPath("$.id").isEqualTo(BATCH_ID);

		verify(endOfLeaseServiceMock).registerBatch(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);
	}
}
