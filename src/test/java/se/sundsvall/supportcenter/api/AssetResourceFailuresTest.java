package se.sundsvall.supportcenter.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SUPPLIERNOTE;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.CreateAssetRequest;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;
import se.sundsvall.supportcenter.service.AssetService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class AssetResourceFailuresTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";

	@MockBean
	private AssetService assetServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createAssetNoAttributesSet() {

		webTestClient.post().uri("/2281/assets")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(CreateAssetRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("modelName")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided")
			.jsonPath("$.violations[1].field").isEqualTo("serialNumber")
			.jsonPath("$.violations[1].message").isEqualTo("must be provided");
		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void createAssetBlankInStrings() {

		webTestClient.post().uri("/2281/assets")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(CreateAssetRequest.create()
				.withManufacturer(" ")
				.withModelName(" ")
				.withModelDescription(" ")
				.withSerialNumber(" ")
				.withMacAddress(" ")
				.withWarrantyEndDate(LocalDate.now().plusDays(360L))
				.withSupplierStatus(" ")
				.withDeliveryDate(LocalDate.now().plusDays(5L)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("macAddress")
			.jsonPath("$.violations[0].message").isEqualTo("must contain a valid mac address")
			.jsonPath("$.violations[1].field").isEqualTo("modelName")
			.jsonPath("$.violations[1].message").isEqualTo("must be provided")
			.jsonPath("$.violations[2].field").isEqualTo("serialNumber")
			.jsonPath("$.violations[2].message").isEqualTo("must be provided");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void createAssetFaultyMacAddress() {

		webTestClient.post().uri("/2281/assets")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(CreateAssetRequest.create()
				.withModelName("modelName")
				.withSerialNumber("serialNumber")
				.withMacAddress("FaultyMacAddress"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("macAddress")
			.jsonPath("$.violations[0].message").isEqualTo("must contain a valid mac address");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void createAssetNoContentTypeSet() {

		webTestClient.post().uri("/2281/assets")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(UNSUPPORTED_MEDIA_TYPE.value())
			.jsonPath("$.detail").isEqualTo("Content-Type is not supported");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void createAssetMissingBody() {

		webTestClient.post().uri("/2281/assets")
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isEqualTo(BAD_REQUEST)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo(
				"Required request body is missing: public org.springframework.http.ResponseEntity<se.sundsvall.supportcenter.api.model.CreateAssetResponse> se.sundsvall.supportcenter.api.AssetResource.createAsset(java.lang.String,java.lang.String,se.sundsvall.supportcenter.api.model.CreateAssetRequest)");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void createAssetNoPobKeySet() {

		webTestClient.post().uri("/2281/assets")
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateAssetRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetMissingBody() {

		// Parameter values
		final var id = "12345";

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo(
				"Required request body is missing: public org.springframework.http.ResponseEntity<java.lang.Void> se.sundsvall.supportcenter.api.AssetResource.updateAsset(java.lang.String,java.lang.String,java.lang.String,se.sundsvall.supportcenter.api.model.UpdateAssetRequest)");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetMissingPobKeyHeader() {

		// Parameter values
		final var id = "12345";

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetMissingContentType() {

		// Parameter values
		final var id = "12345";

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(UNSUPPORTED_MEDIA_TYPE.value())
			.jsonPath("$.detail").isEqualTo("Content-Type is not supported");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetWithFaultyMacAddress() {

		// Parameter values
		final var id = "12345";
		final var body = UpdateAssetRequest.create().withMacAddress("faulty_mac_address");

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(BAD_REQUEST)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("macAddress")
			.jsonPath("$.violations[0].message").isEqualTo("must contain a valid mac address");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetMissingNoteType() {

		// Parameter values
		final var id = "12345";
		final var body = UpdateAssetRequest.create().withNote(Note.create().withText("text"));

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.type")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetMissingNoteText() {

		// Parameter values
		final var id = "12345";
		final var body = UpdateAssetRequest.create().withNote(Note.create().withType(SUPPLIERNOTE));

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.text")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void updateAssetBlankNoteText() {

		// Parameter values
		final var id = "12345";
		final var body = UpdateAssetRequest.create().withNote(Note.create().withType(SUPPLIERNOTE).withText(" "));

		webTestClient.patch().uri("/2281/assets/{id}", id)
			.contentType(APPLICATION_JSON)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.violations[0].field").isEqualTo("note.text")
			.jsonPath("$.violations[0].message").isEqualTo("must be provided");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void getAssetsMissingPobKeyHeader() {

		// Parameter values
		final var serialNumber = "12345";

		webTestClient.get().uri(uriBuilder -> uriBuilder.path("/2281/assets").queryParam("serialNumber", serialNumber).build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request header 'pobKey' for method parameter type String is not present");

		verifyNoInteractions(assetServiceMock);
	}

	@Test
	void getAssetsMissingRequiredFilterParameter() {

		webTestClient.get().uri("/2281/assets")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.title").isEqualTo(BAD_REQUEST.getReasonPhrase())
			.jsonPath("$.status").isEqualTo(BAD_REQUEST.value())
			.jsonPath("$.detail").isEqualTo("Required request parameter 'serialNumber' for method parameter type String is not present");

		verifyNoInteractions(assetServiceMock);
	}
}
