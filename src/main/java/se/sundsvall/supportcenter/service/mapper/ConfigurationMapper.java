package se.sundsvall.supportcenter.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DELIVERY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_END_WARRANTY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_NAME;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_LEASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MAC_ADDRESS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MUNICIPALITY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SUPPLIER_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.MUNICIPALITY_MAP;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;

import generated.client.pob.PobPayload;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;

public final class ConfigurationMapper {

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
			.links(emptyList())
			.type(TYPE_CONFIGURATION_ITEM)
			.memo(ofNullable(toMemo(updateAssetRequest.getNote())).orElse(emptyMap()))
			.data(toData(id, updateAssetRequest));
	}

	/**
	 * Fetch a list of String values by type. The key of the attribute to return should also be provided.
	 *
	 * @param  pobPayloadList the list of PobPayload
	 * @param  type           the PobPayload-type
	 * @param  key            the JSON-key (in the data-object) of the value to fetch.
	 * @return                a List of String values
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
		ofNullable(updateAssetRequest.getMunicipalityId()).ifPresent(value -> dataMap.put(KEY_MUNICIPALITY, MUNICIPALITY_MAP.get(value)));
		ofNullable(updateAssetRequest.getLeaseStatus()).ifPresent(value -> dataMap.put(KEY_LEASE_STATUS, value));
		return dataMap;
	}
}
