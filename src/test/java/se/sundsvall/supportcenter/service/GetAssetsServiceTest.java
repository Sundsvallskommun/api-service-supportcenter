package se.sundsvall.supportcenter.service;

import generated.client.pob.PobPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportcenter.api.model.Asset;
import se.sundsvall.supportcenter.integration.pob.POBClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DELIVERY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_END_WARRANTY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID_OPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MAC_ADDRESS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MANUFACTURER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SERIAL_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SUPPLIER_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_ITEM;

@ExtendWith(MockitoExtension.class)
class GetAssetsServiceTest {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Mock
	private POBClient pobClientMock;

	@InjectMocks
	private AssetService assetService;

	@Test
	void getConfigurationItemsBySerialNumber() {

		// Parameter values
		final var configurationItemId = "configurationItemId";
		final var itemId = "itemId";
		final var pobKey = "pobKey";
		final var serialNumber = "serialNumber";
		final var macAddress = "macAddress";
		final var supplierStatus = "supplierStatus";
		final var hardwareStatus = "hardwareStatus";
		final var hardwareDescription = "hardwareDescription";
		final var modelName = "modelName";
		final var modelDescription = "modelDescription";
		final var deliveryDate = "2023-01-01 00:00:00";
		final var warrantyEndDate = "2025-01-01 00:00:00";
		final var manufacturer = "manufacturer";

		final var itemData = new LinkedHashMap<String, Object>();
		itemData.put(KEY_ITEM_ID, itemId);
		itemData.put(KEY_DESCRIPTION, modelDescription);
		final var item = new LinkedHashMap<String, Object>();
		item.put("Data", itemData);

		final var manufacturerData = new LinkedHashMap<String, Object>();
		manufacturerData.put("Id", manufacturer);

		final var manufacturerMap = new LinkedHashMap<String, Object>();
		manufacturerMap.put("Type", "Organization");
		manufacturerMap.put("Data", manufacturerData);

		final var configurationData =  Map.of(KEY_CONFIGURATION_ITEM, configurationItemId,
			KEY_ITEM, item,
			KEY_DELIVERY_DATE, deliveryDate,
			KEY_END_WARRANTY_DATE, warrantyEndDate,
			KEY_SERIAL_NUMBER, serialNumber,
			KEY_MAC_ADDRESS, macAddress,
			KEY_SUPPLIER_STATUS, supplierStatus,
			KEY_HARDWARE_STATUS, hardwareStatus,
			KEY_DESCRIPTION, (Object) hardwareDescription);

		// Mock
		when(pobClientMock.getConfigurationItemsBySerialNumber(pobKey, serialNumber)).thenReturn(setupListOfPobPayload(configurationData));

		when(pobClientMock.getItemsById(pobKey, itemId)).thenReturn(setupItemPobPayload(List.of(KEY_ITEM_ID_OPTION, KEY_DESCRIPTION), List.of(modelName, modelDescription), manufacturerMap));

		// Call
		final var result = assetService.getConfigurationItemsBySerialNumber(pobKey, serialNumber);

		// Verification
		verify(pobClientMock).getConfigurationItemsBySerialNumber(pobKey, serialNumber);
		verify(pobClientMock).getItemsById(pobKey, itemId);

		assertThat(result).extracting(Asset::getId).containsExactly(configurationItemId);
		assertThat(result).extracting(Asset::getModelName).containsExactly(modelName);
		assertThat(result).extracting(Asset::getModelDescription).containsExactly(modelDescription);
		assertThat(result).extracting(Asset::getManufacturer).containsExactly(manufacturer);
		assertThat(result).extracting(Asset::getSerialNumber).containsExactly(serialNumber);
		assertThat(result).extracting(Asset::getSupplierStatus).containsExactly(supplierStatus);
		assertThat(result).extracting(Asset::getHardwareStatus).containsExactly(hardwareStatus);
		assertThat(result).extracting(Asset::getMacAddress).containsExactly(macAddress);
		assertThat(result).extracting(Asset::getDeliveryDate).containsExactly(LocalDate.parse(deliveryDate, DATE_TIME_FORMATTER));
		assertThat(result).extracting(Asset::getWarrantyEndDate).containsExactly(LocalDate.parse(warrantyEndDate, DATE_TIME_FORMATTER));
	}

	private List<PobPayload> setupListOfPobPayload(Map<String, Object> configurationItemData) {

		final var result = new ArrayList<PobPayload>();
		result.add(new PobPayload().type(TYPE_CONFIGURATION_ITEM).data(configurationItemData));
		return result;
	}

	private List<PobPayload> setupItemPobPayload(List<String> keys, List<String> values, LinkedHashMap<String, Object> manufacturerMap) {
		final var result = new ArrayList<PobPayload>();
		final var keyIterator = keys.iterator();
		final var item = new PobPayload().type(TYPE_ITEM);
		final Map<String, Object> data = new HashMap<>();
		for (String value : values) {
			data.put(keyIterator.next(), value);
		}

		data.put(KEY_MANUFACTURER, manufacturerMap);

		result.add(item.data(data));

		return result;
	}

}
