package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import generated.client.pob.PobPayloadWithTriggerResults;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportcenter.api.model.CreateAssetRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DELIVERY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_END_WARRANTY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID_OPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MAC_ADDRESS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MANUFACTURER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MUNICIPALITY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SERIAL_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_START_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_ITEM;

class CreateAssetMapperTest {

	@Test
	void getPobPayloadForCreatingItem() {

		//Parameter values
		final var modelName = "modelName";
		final var modelDescription = "modelDescription";
		final var manufacturer = "manufacturer";

		final var createAssetRequest = CreateAssetRequest.create()
			.withModelDescription(modelDescription)
			.withManufacturer(manufacturer)
			.withModelName(modelName);

		// Call
		final var result = CreateAssetMapper.toPobPayloadForCreatingItem(createAssetRequest);

		// Verification
		assertThat(result).isNotNull();
		assertThat(result.getType()).isEqualTo(TYPE_ITEM);
		assertThat(result.getData()).containsEntry(KEY_DESCRIPTION, modelDescription);
		assertThat(result.getData()).containsEntry(KEY_ITEM_ID_OPTION, modelName);
		assertThat(result.getData()).containsEntry(KEY_MANUFACTURER, manufacturer);
		assertThat(result.getData()).containsKey(KEY_START_DATE);
		final var startDate = (String) result.getData().get(KEY_START_DATE);
		assertThat(startDate).startsWith(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
	}

	@Test
	void getPobPayloadForCreatingConfigurationItem() {

		//Parameter values
		final var itemId = "itemId";
		final var modelName = "modelName";
		final var manufacturer = "manufacturer";
		final var serialNumber = "serialNumber";
		final var macAddress = "macAddress";
		final var supplierStatus = "supplierStatus";
		final var hardwareStatus = "hardwareStatus";
		final var municipalityId = "2281";

		final var createAssetRequest = CreateAssetRequest.create()
			.withManufacturer(manufacturer)
			.withSerialNumber(serialNumber)
			.withMacAddress(macAddress)
			.withSupplierStatus(supplierStatus)
			.withHardwareStatus(hardwareStatus)
			.withModelName(modelName)
			.withDeliveryDate(LocalDate.now().plusDays(2))
			.withWarrantyEndDate(LocalDate.now().plusDays(360))
			.withMunicipalityId(municipalityId);

		// Call
		final var result = CreateAssetMapper.toPobPayloadForCreatingConfigurationItem(itemId, createAssetRequest);

		// Verification
		assertThat(result).isNotNull();
		assertThat(result.getType()).isEqualTo(TYPE_CONFIGURATION_ITEM);
		assertThat(result.getData()).containsEntry(KEY_SERIAL_NUMBER, serialNumber);
		assertThat(result.getData()).containsEntry(KEY_MAC_ADDRESS, macAddress);
		assertThat(result.getData()).containsEntry(KEY_ITEM, itemId);
		assertThat(result.getData()).containsEntry(KEY_HARDWARE_STATUS, hardwareStatus);
		assertThat(result.getData()).containsEntry(KEY_DELIVERY_DATE, createAssetRequest.getDeliveryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		assertThat(result.getData()).containsEntry(KEY_END_WARRANTY_DATE, createAssetRequest.getWarrantyEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		assertThat(result.getData()).containsEntry(KEY_MUNICIPALITY, "Sundsvall");
	}


	@Test
	void toItemList() {

		// Parameter values
		final var pobPayloadList = List.of(
			new PobPayload()
				.type("Item")
				.data(Map.of("Id", "ItemId-1")),
			new PobPayload()
				.type("Item")
				.data(Map.of("Id", "ItemId-2")),
			new PobPayload()
				.type("SomethingElseThanItem")
				.data(Map.of(
					"Id", "NotItemId-3")));

		// Call
		final var result = CreateAssetMapper.toItemIdList(pobPayloadList);

		// Verification
		assertThat(result).isNotNull().hasSize(2);
		assertThat(result.get(0)).isEqualTo("ItemId-1");
		assertThat(result.get(1)).isEqualTo("ItemId-2");
	}

	@Test
	void toItemId() {

		// Parameter values
		final var pobPayloadList = List.of(
			new PobPayloadWithTriggerResults()
				.type("Item")
				.data(Map.of("Id", "ItemId-1")),
			new PobPayloadWithTriggerResults()
				.type("Item")
				.data(Map.of("Id", "ItemId-2")),
			new PobPayloadWithTriggerResults()
				.type("SomethingElseThanItem")
				.data(Map.of(
					"Id", "NotItemId-3")));

		// Call
		final var result = CreateAssetMapper.toItemId(pobPayloadList, TYPE_ITEM, "Id");

		// Verification
		assertThat(result).isNotNull().isEqualTo("ItemId-1");
	}
}
