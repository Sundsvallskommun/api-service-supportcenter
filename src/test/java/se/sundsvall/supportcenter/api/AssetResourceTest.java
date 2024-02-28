package se.sundsvall.supportcenter.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.api.model.Asset;
import se.sundsvall.supportcenter.api.model.CreateAssetRequest;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;
import se.sundsvall.supportcenter.service.AssetService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class AssetResourceTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";

	@MockBean
	private AssetService assetServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createAsset() {

		// Parameter values
		final var createAssetRequest = CreateAssetRequest.create()
			.withManufacturer("manufacturer")
			.withModelName("modelName")
			.withModelDescription("modelDescription")
			.withSerialNumber("serialNumber")
			.withMacAddress("00:00:0a:bb:28:fc")
			.withWarrantyEndDate(LocalDate.now().plusDays(360L))
			.withSupplierStatus("supplierStatus")
			.withDeliveryDate(LocalDate.now().plusDays(5L))
			.withMunicipalityId("municipalityId");
		final var pobId = "pobId";

		when(assetServiceMock.createAsset(POBKEY_HEADER_VALUE, createAssetRequest)).thenReturn(pobId);

		webTestClient.post().uri("/assets")
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.contentType(APPLICATION_JSON)
			.bodyValue(createAssetRequest)
			.exchange()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/assets?serialNumber=" + pobId)
			.expectStatus().isCreated()
			.expectBody().jsonPath("$.['id']").isEqualTo(pobId);

		verify(assetServiceMock).createAsset(POBKEY_HEADER_VALUE, createAssetRequest);
	}

	@Test
	void updateAsset() {

		// Parameter values
		final var id = "12345";
		final var deliveryDate = LocalDate.now().plusDays(1);
		final var warrantyEndDate = LocalDate.now().plusDays(700);

		final var updateAssetRequest = UpdateAssetRequest.create()
			.withHardwareName("hardwareName")
			.withSupplierStatus("supplierStatus")
			.withMacAddress("00:00:0a:bb:28:fc")
			.withDeliveryDate(deliveryDate)
			.withWarrantyEndDate(warrantyEndDate)
			.withMunicipalityId("municipalityId");

		webTestClient.patch().uri("/assets/{id}", id)
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.contentType(APPLICATION_JSON)
			.bodyValue(updateAssetRequest)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(assetServiceMock).updateConfigurationItem(POBKEY_HEADER_VALUE, id, updateAssetRequest);
	}

	@Test
	void getAssets() {

		// Parameter values
		final var serialNumber = "12345";

		// Mock
		when(assetServiceMock.getConfigurationItemsBySerialNumber(POBKEY_HEADER_VALUE, serialNumber)).thenReturn(List.of(Asset.create().withSerialNumber(serialNumber)));

		webTestClient.get().uri(uriBuilder -> uriBuilder.path("/assets")
			.queryParam("serialNumber", serialNumber).build())
			.header(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.[0]['serialNumber']").isEqualTo(serialNumber);

		verify(assetServiceMock).getConfigurationItemsBySerialNumber(POBKEY_HEADER_VALUE, serialNumber);
	}
}
