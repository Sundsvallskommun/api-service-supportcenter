package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import generated.client.pob.PobPayloadWithTriggerResults;
import se.sundsvall.supportcenter.api.model.CreateAssetRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DELIVERY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_END_WARRANTY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID_OPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MAC_ADDRESS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MANUFACTURER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SERIAL_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_START_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SUPPLIER_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_ITEM;

public class CreateAssetMapper {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private CreateAssetMapper() {}

	public static PobPayload toPobPayloadForCreatingItem(CreateAssetRequest createAssetRequest) {
		return new PobPayload()
			.links(emptyList())
			.type(TYPE_ITEM)
			.data(toDataForCreatingItem(createAssetRequest))
			.memo(emptyMap());
	}

	public static PobPayload toPobPayloadForCreatingConfigurationItem(String id, CreateAssetRequest createAssetRequest) {
		return new PobPayload()
			.links(emptyList())
			.type(TYPE_CONFIGURATION_ITEM)
			.data(toDataForCreatingConfigurationItem(id, createAssetRequest))
			.memo(emptyMap());
	}

	/**
	 * Fetch a list of itemIds.
	 *
	 * @param pobPayloadList the list of PobPayload
	 * @return a List of itemIds
	 */
	public static List<String> toItemIdList(List<PobPayload> pobPayloadList) {
		return Optional.ofNullable(pobPayloadList).orElse(emptyList()).stream()
			.filter(pobPayload -> pobPayload.getType().equalsIgnoreCase(TYPE_ITEM))
			.map(PobPayload::getData)
			.filter(Objects::nonNull)
			.map(data -> data.get(KEY_CONFIGURATION_ITEM))
			.filter(Objects::nonNull)
			.map(Object::toString)
			.toList();

	}

	public static String toItemId(List<PobPayloadWithTriggerResults> pobPayloadList, String type, String key) {
		return Optional.ofNullable(pobPayloadList).orElse(emptyList()).stream()
			.filter(pobPayload -> pobPayload.getType().equalsIgnoreCase(type))
			.map(PobPayloadWithTriggerResults::getData)
			.map(data -> data.get(key))
			.findFirst()
			.map(Object::toString)
			.orElse(null);
	}

	private static Map<String, Object> toDataForCreatingItem(CreateAssetRequest createAssetRequest) {
		final var dataMap = new HashMap<String, Object>();
		Optional.ofNullable(createAssetRequest.getModelName()).ifPresent(value -> dataMap.put(KEY_ITEM_ID_OPTION, value));
		Optional.ofNullable(createAssetRequest.getModelDescription()).ifPresent(value -> dataMap.put(KEY_DESCRIPTION, value));
		Optional.ofNullable(createAssetRequest.getManufacturer()).ifPresent(value -> dataMap.put(KEY_MANUFACTURER, value));
		dataMap.put(KEY_START_DATE, LocalDateTime.now().format(DATE_TIME_FORMATTER));

		return dataMap;
	}

	private static Map<String, Object> toDataForCreatingConfigurationItem(String id, CreateAssetRequest createAssetRequest) {
		final var dataMap = new HashMap<String, Object>();
		Optional.ofNullable(createAssetRequest.getSerialNumber()).ifPresent(value -> dataMap.put(KEY_SERIAL_NUMBER, value));
		Optional.ofNullable(createAssetRequest.getMacAddress()).ifPresent(value -> dataMap.put(KEY_MAC_ADDRESS, value));
		Optional.ofNullable(id).ifPresent(value -> dataMap.put(KEY_ITEM, value));
		Optional.ofNullable(createAssetRequest.getSupplierStatus()).ifPresent(value -> dataMap.put(KEY_SUPPLIER_STATUS, value));
		Optional.ofNullable(createAssetRequest.getHardwareStatus()).ifPresent(value -> dataMap.put(KEY_HARDWARE_STATUS, value));
		Optional.ofNullable(createAssetRequest.getDeliveryDate()).ifPresent(value -> dataMap.put(KEY_DELIVERY_DATE, value.format(DATE_FORMATTER)));
		Optional.ofNullable(createAssetRequest.getWarrantyEndDate()).ifPresent(value -> dataMap.put(KEY_END_WARRANTY_DATE, value.format(DATE_FORMATTER)));

		return dataMap;
	}
}
