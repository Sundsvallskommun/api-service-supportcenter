package se.sundsvall.supportcenter.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class EndOfLeaseComputerResourceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private EndOfLeaseService endOfLeaseServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void retryNamedComputers() {

		final var retryEndOfLeaseComputersRequest = RetryEndOfLeaseComputersRequest.create()
			.withSerialNumbers(List.of("J123ABC", "K456DEF"));

		when(endOfLeaseServiceMock.retryComputersGivenUpOn(MUNICIPALITY_ID, retryEndOfLeaseComputersRequest)).thenReturn(2);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseComputers/retry", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(retryEndOfLeaseComputersRequest)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().jsonPath("$.reset").isEqualTo(2);

		verify(endOfLeaseServiceMock).retryComputersGivenUpOn(MUNICIPALITY_ID, retryEndOfLeaseComputersRequest);
	}

	/**
	 * A body without serial numbers means every computer the municipality has been given up on, which is the shape an
	 * operator reaches for when the health endpoint says the queue needs a person.
	 */
	@Test
	void retryEveryComputerGivenUpOn() {

		final var retryEndOfLeaseComputersRequest = RetryEndOfLeaseComputersRequest.create();

		when(endOfLeaseServiceMock.retryComputersGivenUpOn(MUNICIPALITY_ID, retryEndOfLeaseComputersRequest)).thenReturn(7);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseComputers/retry", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(retryEndOfLeaseComputersRequest)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.reset").isEqualTo(7);

		verify(endOfLeaseServiceMock).retryComputersGivenUpOn(MUNICIPALITY_ID, retryEndOfLeaseComputersRequest);
	}

	/**
	 * Nothing to take is not an error. The operator asked and the answer is zero.
	 */
	@Test
	void retryWithNothingToTake() {

		final var retryEndOfLeaseComputersRequest = RetryEndOfLeaseComputersRequest.create();

		when(endOfLeaseServiceMock.retryComputersGivenUpOn(MUNICIPALITY_ID, retryEndOfLeaseComputersRequest)).thenReturn(0);

		webTestClient.post().uri("/{municipalityId}/endOfLeaseComputers/retry", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(retryEndOfLeaseComputersRequest)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.reset").isEqualTo(0);

		verify(endOfLeaseServiceMock).retryComputersGivenUpOn(MUNICIPALITY_ID, retryEndOfLeaseComputersRequest);
	}
}
