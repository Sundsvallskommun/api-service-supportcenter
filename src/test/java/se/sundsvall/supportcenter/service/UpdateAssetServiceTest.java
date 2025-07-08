package se.sundsvall.supportcenter.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.service.mapper.ConfigurationMapper;

@ExtendWith(MockitoExtension.class)
class UpdateAssetServiceTest {

	@Mock
	private POBIntegration pobIntegrationMock;

	@Mock
	private ConfigurationService configurationServiceMock;

	@InjectMocks
	private AssetService assetService;

	@Test
	void updateConfigurationItem() {

		// Parameter values
		final var pobKey = "pobKey";
		final var updateAssetRequest = UpdateAssetRequest.create()
			.withHardwareName("hardwareName")
			.withHardwareStatus("hardwareStatus");
		final var serialNbr = "ABC123";
		final var id = "123456";

		// Mock
		when(configurationServiceMock.getSerialNumberId(pobKey, serialNbr)).thenReturn(id);
		// Call
		assetService.updateConfigurationItem(pobKey, serialNbr, updateAssetRequest);

		// Verification
		verify(configurationServiceMock).getSerialNumberId(pobKey, serialNbr);
		verify(pobIntegrationMock).updateConfigurationItem(pobKey, ConfigurationMapper.toPobPayload(id, updateAssetRequest));
	}
}
