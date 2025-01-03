package se.sundsvall.supportcenter.service.mapper.constant;

import java.util.Map;

public final class ConfigurationMapperConstants {

	public static final String KEY_CLOSURE_CODE = "Id";
	public static final String KEY_CASE_CATEGORY = "Id";
	public static final String KEY_CONFIGURATION_ITEM = "Id";
	public static final String KEY_ITEM_ID = "Id";
	public static final String KEY_ITEM_ID_OPTION = "IdOption";
	public static final String KEY_MANUFACTURER = "Virtual.Manufacturer";
	public static final String KEY_START_DATE = "StartDate";
	public static final String KEY_HARDWARE_NAME = "OptionalNumber";
	public static final String KEY_HARDWARE_STATUS = "CiStatus";
	public static final String KEY_DESCRIPTION = "Description";
	public static final String KEY_SERIAL_NUMBER = "SerialNumber";
	public static final String KEY_ITEM = "Item";
	public static final String KEY_DELIVERY_DATE = "DeliveryDate";
	public static final String KEY_END_WARRANTY_DATE = "EndWarrantyDate";
	public static final String KEY_MAC_ADDRESS = "Virtual.BluetoothAddress";
	public static final String KEY_SUPPLIER_STATUS = "Virtual.LeverantorensStatus";
	public static final String KEY_MUNICIPALITY = "Virtual.CIKommun";
	public static final String TYPE_CLOSURE_CODE = "ClosureCode";
	public static final String TYPE_CASE_CATEGORY = "CaseCategory";
	public static final String TYPE_CONFIGURATION_ITEM = "ConfigurationItem";
	public static final String TYPE_ITEM = "Item";
	public static final Map<String, String> MUNICIPALITY_MAP = Map.of("2281", "Sundsvall", "2260", "Ånge");

	private ConfigurationMapperConstants() {}
}
