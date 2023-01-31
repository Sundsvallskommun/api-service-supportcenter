package se.sundsvall.supportcenter.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static se.sundsvall.supportcenter.service.mapper.GetAssetMapper.toItemId;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID_OPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MANUFACTURER;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Asset;

class GetAssetMapperTest {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void getItemId() {

		// Parameter values
		final var itemId = "itemId";
		final var data = new LinkedHashMap<String, Object>();
		data.put("Id", itemId);
		final var item = new LinkedHashMap<String, Object>();
		item.put("Data", data);

		final var pobPayloadList = List.of(
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", "111",
					"Item", item)));

		// Call
		final var result = toItemId(pobPayloadList);

		// Verification
		assertThat(result).isNotNull().isEqualTo(itemId);
	}

	@Test
	void getMapOfAttributes() {

		// Parameter values
		final var attribute1 = "attribute-1";
		final var attribute2 = "attribute-2";
		final var attribute3 = "attribute-3";
		final var value1 = "value-1";
		final var value2 = "value-2";
		final var value3 = "value-3";

		final var pobPayloadList = List.of(
			new PobPayload()
				.type("Item")
				.data(Map.of(
					attribute1, "value-1",
					attribute2, "value-2",
					attribute3, "value-3")));

		// Call
		final var result = GetAssetMapper.toMapOfAttributes(pobPayloadList);

		// Verification
		assertThat(result)
			.hasSize(3)
			.containsEntry(attribute1, value1)
			.containsEntry(attribute2, value2)
			.containsEntry(attribute3, value3);
	}

	@Test
	void getMapOfAttributesWhenPobPayloadListIsNull() {

		// Call
		final var result = GetAssetMapper.toMapOfAttributes(null);

		// Verification
		assertThat(result).isNull();
	}

	@Test
	void getAssetList() {
		// Parameter values
		final var itemId = "itemId";
		final var modelName = "modelName";
		final var modelDescription = "modelDescription";
		final var deliveryDate1 = "2023-01-01 00:00:00";
		final var warrantyEndDate1 = "2025-01-01 00:00:00";
		final var configurationItemId1 = "111";
		final var hardwareName1 = "hardwareName-1";
		final var hardwareDescription1 = "hardwareDescription-1";
		final var serialNumber1 = "serialNumber-1";
		final var macAddress1 = "macAddress-1";
		final var supplierStatus1 = "supplierStatus-1";
		final var hardwareStatus1 = "hardwareStatus-1";
		final var configurationItemId2 = "222";
		final var hardwareName2 = "hardwareName-2";
		final var hardwareDescription2 = "hardwareDescription-2";
		final var serialNumber2 = "serialNumber-2";
		final var macAddress2 = "macAddress-2";
		final var supplierStatus2 = "supplierStatus-2";
		final var hardwareStatus2 = "hardwareStatus-2";
		final var deliveryDate2 = "2024-01-01 00:00:00";
		final var warrantyEndDate2 = "2026-01-01 00:00:00";

		final var manufacturer = "manufacturer";

		final var data = new LinkedHashMap<String, Object>();
		data.put("Id", manufacturer);

		final var manufacturerMap = new LinkedHashMap<String, Object>();
		manufacturerMap.put("Type", "Organization");
		manufacturerMap.put("Data", data);

		final var configurationItems = List.of(
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", configurationItemId1,
					"Description", hardwareDescription1,
					"OptionalNumber", hardwareName1,
					"SerialNumber", serialNumber1,
					"Virtual.BluetoothAddress", macAddress1,
					"Virtual.LeverantorensStatus", supplierStatus1,
					"CiStatus", hardwareStatus1,
					"DeliveryDate", deliveryDate1,
					"EndWarrantyDate", warrantyEndDate1)),
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", configurationItemId2,
					"Description", hardwareDescription2,
					"OptionalNumber", hardwareName2,
					"SerialNumber", serialNumber2,
					"Virtual.BluetoothAddress", macAddress2,
					"Virtual.LeverantorensStatus", supplierStatus2,
					"CiStatus", hardwareStatus2,
					"DeliveryDate", deliveryDate2,
					"EndWarrantyDate", warrantyEndDate2)));

		final var itemAttributes = Map.of(
			KEY_ITEM_ID, itemId,
			KEY_ITEM_ID_OPTION, modelName,
			KEY_DESCRIPTION, modelDescription,
			KEY_MANUFACTURER, (Object) manufacturerMap);

		// Call
		final var result = GetAssetMapper.toAssetList(configurationItems, itemAttributes);

		// Verification
		assertThat(result)
			.hasSize(2)
			.extracting(
				Asset::getId,
				Asset::getHardwareDescription,
				Asset::getSerialNumber,
				Asset::getMacAddress,
				Asset::getHardwareName,
				Asset::getSupplierStatus,
				Asset::getHardwareStatus,
				Asset::getDeliveryDate,
				Asset::getWarrantyEndDate,
				Asset::getManufacturer,
				Asset::getModelDescription,
				Asset::getModelName)
			.containsExactly(
				tuple(
					configurationItemId1,
					hardwareDescription1,
					serialNumber1,
					macAddress1,
					hardwareName1,
					supplierStatus1,
					hardwareStatus1,
					LocalDate.parse(deliveryDate1, DATE_TIME_FORMATTER),
					LocalDate.parse(warrantyEndDate1, DATE_TIME_FORMATTER),
					manufacturer,
					modelDescription,
					modelName),
				tuple(
					configurationItemId2,
					hardwareDescription2,
					serialNumber2,
					macAddress2,
					hardwareName2,
					supplierStatus2,
					hardwareStatus2,
					LocalDate.parse(deliveryDate2, DATE_TIME_FORMATTER),
					LocalDate.parse(warrantyEndDate2, DATE_TIME_FORMATTER),
					manufacturer,
					modelDescription,
					modelName));
	}

	@Test
	void getAssetListWithEmptyItemAttributes() {
		// Parameter values
		final var configurationItemId = "111";
		final var hardwareName = "hardwareName";
		final var hardwareDescription = "hardwareDescription";
		final var serialNumber = "serialNumber";
		final var macAddress = "macAddress";
		final var supplierStatus = "supplierStatus";
		final var hardwareStatus = "hardwareStatus";
		final var deliveryDate = "2023-01-01 00:00:00";
		final var warrantyEndDate = "2025-01-01 00:00:00";

		final var configurationItems = List.of(
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", configurationItemId,
					"Description", hardwareDescription,
					"OptionalNumber", hardwareName,
					"SerialNumber", serialNumber,
					"Virtual.BluetoothAddress", macAddress,
					"Virtual.LeverantorensStatus", supplierStatus,
					"CiStatus", hardwareStatus,
					"DeliveryDate", deliveryDate,
					"EndWarrantyDate", warrantyEndDate)));

		final var itemAttributes = new HashMap<String, Object>();

		// Call
		final var result = GetAssetMapper.toAssetList(configurationItems, itemAttributes);

		// Verification
		assertThat(result)
			.hasSize(1)
			.extracting(
				Asset::getId,
				Asset::getHardwareDescription,
				Asset::getSerialNumber,
				Asset::getMacAddress,
				Asset::getHardwareName,
				Asset::getSupplierStatus,
				Asset::getHardwareStatus,
				Asset::getDeliveryDate,
				Asset::getWarrantyEndDate,
				Asset::getManufacturer,
				Asset::getModelName)
			.containsExactly(tuple(
				configurationItemId,
				hardwareDescription,
				serialNumber,
				macAddress,
				hardwareName,
				supplierStatus,
				hardwareStatus,
				LocalDate.parse(deliveryDate, DATE_TIME_FORMATTER),
				LocalDate.parse(warrantyEndDate, DATE_TIME_FORMATTER),
				null,
				null));
	}
}
