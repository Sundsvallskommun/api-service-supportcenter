package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Asset;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DELIVERY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_END_WARRANTY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_NAME;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MAC_ADDRESS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SERIAL_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SUPPLIER_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;

public class ConfigurationMapper {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private ConfigurationMapper() {}

	public static List<String> toCaseCategoryList(List<PobPayload> pobPayLoadList) {
		return fetchListOfValues(pobPayLoadList, TYPE_CASE_CATEGORY, KEY_CASE_CATEGORY);
	}

	public static List<String> toClosureCodeList(List<PobPayload> pobPayLoadList) {
		return fetchListOfValues(pobPayLoadList, TYPE_CLOSURE_CODE, KEY_CLOSURE_CODE);
	}

	public static List<String> toConfigurationItemList(List<PobPayload> pobPayLoadList) {
		return fetchListOfValues(pobPayLoadList, TYPE_CONFIGURATION_ITEM, KEY_CONFIGURATION_ITEM);
	}

	public static PobPayload toPobPayload(String id, UpdateAssetRequest updateAssetRequest) {
		return new PobPayload()
			.type(TYPE_CONFIGURATION_ITEM)
			.memo(ofNullable(toMemo(updateAssetRequest.getNote())).orElse(emptyMap()))
			.data(toData(id, updateAssetRequest));
	}

	public static List<Asset> toAssetList(List<PobPayload> pobPayloadList) {
		return Optional.ofNullable(pobPayloadList).orElse(emptyList()).stream()
			.map(ConfigurationMapper::toAsset)
			.toList();
	}

	private static Asset toAsset(PobPayload pobPayload) {
		return Optional.ofNullable(pobPayload.getData())
			.map(data -> Asset.create()
				.withId((String) data.get(KEY_CONFIGURATION_ITEM))
				.withMacAddress((String) data.get(KEY_MAC_ADDRESS))
				.withSupplierStatus((String) data.get(KEY_SUPPLIER_STATUS))
				.withHardwareStatus((String) data.get(KEY_HARDWARE_STATUS))
				.withHardwareDescription((String) data.get(KEY_DESCRIPTION))
				.withHardwareName((String) data.get(KEY_HARDWARE_NAME))
				.withSerialNumber((String) data.get(KEY_SERIAL_NUMBER)))
			.orElse(null);
	}

	/**
	 * Fetch a list of String values by type. The key of the attribute to return should also be provided.
	 *
	 * @param pobPayloadList the list of PobPayload
	 * @param type           the PobPayload-type
	 * @param key            the JSON-key (in the data-object) of the value to fetch.
	 * @return a List of String values
	 */
	private static List<String> fetchListOfValues(List<PobPayload> pobPayloadList, String type, String key) {
		return ofNullable(pobPayloadList).orElse(emptyList()).stream()
			.filter(pobPayload -> pobPayload.getType().equalsIgnoreCase(type))
			.map(PobPayload::getData)
			.filter(Objects::nonNull)
			.map(data -> data.get(key))
			.filter(Objects::nonNull)
			.map(Object::toString)
			.toList();
	}

	private static Map<String, Object> toData(String id, UpdateAssetRequest updateAssetRequest) {
		final var dataMap = new HashMap<String, Object>();
		ofNullable(id).ifPresent(value -> dataMap.put(KEY_CONFIGURATION_ITEM, value));
		ofNullable(updateAssetRequest.getHardwareName()).ifPresent(value -> dataMap.put(KEY_HARDWARE_NAME, value));
		ofNullable(updateAssetRequest.getHardwareStatus()).ifPresent(value -> dataMap.put(KEY_HARDWARE_STATUS, value));
		ofNullable(updateAssetRequest.getMacAddress()).ifPresent(value -> dataMap.put(KEY_MAC_ADDRESS, value));
		ofNullable(updateAssetRequest.getSupplierStatus()).ifPresent(value -> dataMap.put(KEY_SUPPLIER_STATUS, value));
		ofNullable(updateAssetRequest.getDeliveryDate()).ifPresent(value -> dataMap.put(KEY_DELIVERY_DATE, value.format(DATE_FORMATTER)));
		ofNullable(updateAssetRequest.getWarrantyEndDate()).ifPresent(value -> dataMap.put(KEY_END_WARRANTY_DATE, value.format(DATE_FORMATTER)));
		return dataMap;
	}
}
