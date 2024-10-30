package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import org.apache.commons.lang3.StringUtils;
import se.sundsvall.supportcenter.api.model.Asset;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DELIVERY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_END_WARRANTY_DATE;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_NAME;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_HARDWARE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_ITEM_ID_OPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MAC_ADDRESS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MANUFACTURER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MUNICIPALITY;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SERIAL_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_SUPPLIER_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.MUNICIPALITY_MAP;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_ITEM;

public class GetAssetMapper {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private GetAssetMapper() {}

	public static String toItemId(List<PobPayload> pobPayloadList) {
		return ofNullable(pobPayloadList).orElse(emptyList()).stream()
			.filter(pobPayload -> pobPayload.getType().equalsIgnoreCase(TYPE_CONFIGURATION_ITEM))
			.map(PobPayload::getData)
			.filter(data -> data.containsKey(TYPE_ITEM))
			.map(data -> data.get(TYPE_ITEM))
			.filter(Objects::nonNull)
			.map(LinkedHashMap.class::cast)
			.map(data -> data.get("Data"))
			.filter(Objects::nonNull)
			.map(LinkedHashMap.class::cast)
			.map(id -> id.get(KEY_ITEM_ID))
			.map(Object::toString).findFirst()
			.orElse(null);
	}

	public static Map<String, Object> toMapOfAttributes(List<PobPayload> pobPayloadList) {

		return ofNullable(pobPayloadList).orElse(emptyList()).stream()
			.findFirst()
			.map(PobPayload::getData)
			.orElse(null);
	}

	public static List<Asset> toAssetList(List<PobPayload> configurationItems, Map<String, Object> itemAttributes) {
		return ofNullable(configurationItems).orElse(emptyList()).stream()
			.map(pobPayload -> toAsset(pobPayload, itemAttributes))
			.toList();
	}

	private static Asset toAsset(PobPayload pobPayload, Map<String, Object> itemAttributes) {
		return ofNullable(pobPayload.getData())
			.map(data -> Asset.create()
				.withId((String) data.get(KEY_CONFIGURATION_ITEM))
				.withMacAddress((String) data.get(KEY_MAC_ADDRESS))
				.withSupplierStatus((String) data.get(KEY_SUPPLIER_STATUS))
				.withHardwareStatus((String) data.get(KEY_HARDWARE_STATUS))
				.withHardwareDescription((String) data.get(KEY_DESCRIPTION))
				.withHardwareName((String) data.get(KEY_HARDWARE_NAME))
				.withSerialNumber((String) data.get(KEY_SERIAL_NUMBER))
				.withDeliveryDate(toLocalDate((String) data.get(KEY_DELIVERY_DATE)))
				.withWarrantyEndDate(toLocalDate((String) data.get(KEY_END_WARRANTY_DATE)))
				.withModelName((String) ofNullable(itemAttributes).orElse(emptyMap()).get(KEY_ITEM_ID_OPTION))
				.withModelDescription((String) ofNullable(itemAttributes).orElse(emptyMap()).get(KEY_DESCRIPTION))
				.withManufacturer(toManufacturer(convertObjectToMap(ofNullable(itemAttributes).orElse(emptyMap()).get(KEY_MANUFACTURER))))
				.withMunicipalityId(toMunicipalityId((String) data.get(KEY_MUNICIPALITY))))
			.orElse(null);
	}

	private static LocalDate toLocalDate(String date) {
		return StringUtils.isNotEmpty(date) ? LocalDate.parse(date, DATE_TIME_FORMATTER) : null;
	}

	private static String toManufacturer(Map<String, Object> manufacturerMap) {
		return ofNullable(manufacturerMap).stream()
			.map(objectMap -> objectMap.get("Data"))
			.filter(Objects::nonNull)
			.map(HashMap.class::cast)
			.map(data -> data.get("Id"))
			.filter(Objects::nonNull)
			.map(Object::toString)
			.findFirst()
			.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> convertObjectToMap(Object attributes) {
		return attributes instanceof Map<?, ?> ? (Map<String, Object>) attributes : emptyMap();
	}

	private static String toMunicipalityId(String municipality) {
		return MUNICIPALITY_MAP.entrySet().stream()
			.filter(entry -> entry.getValue().equalsIgnoreCase(municipality))
			.findFirst()
			.map(Map.Entry::getKey)
			.orElse(null);
	}

}
