package se.sundsvall.supportcenter.service.mapper;

import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toNote;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.DEFAULT_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ACTIVITY_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_BUSINESS_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CITY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CONTACT_PERSON;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_COUNTERPART;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CUSTOMER_CONTACT;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EMAIL;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_ARTICLE_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_SERVICE_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_FREE_TEXT;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_JOIN_CONTACT;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_MANAGEMENT_COMPANY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_OBJECT_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_OFFICE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PERSONAL;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PHONE_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_POSTAL_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PRIORITY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_PROJECT_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_GROUP;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_NUMBER;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_STREET;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_SUBACCOUNT;
import static se.sundsvall.supportcenter.service.util.CaseUtil.extractValueFromJsonPath;
import static se.sundsvall.supportcenter.service.util.CaseUtil.jsonPathExists;

import java.util.HashMap;
import java.util.Map;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Address;
import se.sundsvall.supportcenter.api.model.Case;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;

public class CaseMapper {

	private CaseMapper() {}

	private static final String JSON_PATH_CASE_TYPE = "$['Data']['CaseType']['Data']['Id']";
	private static final String JSON_PATH_CASE_CATEGORY = "$['Data']['CaseCategory']['Data']['Id']";
	private static final String JSON_PATH_RESPONSIBLE_GROUP = "$['Data']['ResponsibleGroup']['Data']['Id']";
	private static final String JSON_PATH_CUSTOMER_CONTACT = "$['Data']['Contact.Customer']['Data']['Id']";
	private static final String JSON_PATH_PRIORITY = "$['Data']['PriorityInfo.Priority']['Data']['Id']";
	private static final String JSON_PATH_JOIN_CONTACT = "$['Data']['Virtual.Shop_JoinContact']['Data']['Id']";

	public static PobPayload toPobPayload(CreateCaseRequest createCaseRequest) {

		return new PobPayload()
			.type(DEFAULT_TYPE)
			.data(toData(createCaseRequest))
			.memo(toMemo(createCaseRequest.getNote()));
	}

	private static Map<String, Object> toData(CreateCaseRequest createCaseRequest) {
		final var dataMap = new HashMap<String, Object>();
		ofNullable(createCaseRequest.getDescription()).ifPresent(value -> dataMap.put(KEY_DESCRIPTION, value));
		ofNullable(createCaseRequest.getCaseType()).ifPresent(value -> dataMap.put(KEY_CASE_TYPE, value));
		ofNullable(createCaseRequest.getCustomerContact()).ifPresent(value -> dataMap.put(KEY_CUSTOMER_CONTACT, value));
		ofNullable(createCaseRequest.getPriority()).ifPresent(value -> dataMap.put(KEY_PRIORITY, value));
		ofNullable(createCaseRequest.getCaseCategory()).ifPresent(value -> dataMap.put(KEY_CASE_CATEGORY, value));
		ofNullable(createCaseRequest.getResponsibleGroup()).ifPresent(value -> dataMap.put(KEY_RESPONSIBLE_GROUP, value));
		ofNullable(createCaseRequest.getExternalServiceId()).ifPresent(value -> dataMap.put(KEY_EXTERNAL_SERVICE_ID, value));
		ofNullable(createCaseRequest.getExternalArticleNumber()).ifPresent(value -> dataMap.put(KEY_EXTERNAL_ARTICLE_NUMBER, value));
		ofNullable(createCaseRequest.getManagementCompany()).ifPresent(value -> dataMap.put(KEY_MANAGEMENT_COMPANY, value));
		ofNullable(createCaseRequest.getPersonal()).ifPresent(value -> addBoolean(KEY_PERSONAL, value, dataMap));
		ofNullable(createCaseRequest.getOffice()).ifPresent(value -> addBoolean(KEY_OFFICE, value, dataMap));
		ofNullable(createCaseRequest.getFreeText()).ifPresent(value -> dataMap.put(KEY_FREE_TEXT, value));
		ofNullable(createCaseRequest.getJoinContact()).ifPresent(value -> dataMap.put(KEY_JOIN_CONTACT, value));
		ofNullable(createCaseRequest.getResponsibilityNumber()).ifPresent(value -> dataMap.put(KEY_RESPONSIBLE_NUMBER, value));
		ofNullable(createCaseRequest.getSubaccount()).ifPresent(value -> dataMap.put(KEY_SUBACCOUNT, value));
		ofNullable(createCaseRequest.getBusinessNumber()).ifPresent(value -> dataMap.put(KEY_BUSINESS_NUMBER, value));
		ofNullable(createCaseRequest.getActivityNumber()).ifPresent(value -> dataMap.put(KEY_ACTIVITY_NUMBER, value));
		ofNullable(createCaseRequest.getProjectNumber()).ifPresent(value -> dataMap.put(KEY_PROJECT_NUMBER, value));
		ofNullable(createCaseRequest.getObjectNumber()).ifPresent(value -> dataMap.put(KEY_OBJECT_NUMBER, value));
		ofNullable(createCaseRequest.getCounterPart()).ifPresent(value -> dataMap.put(KEY_COUNTERPART, value));
		ofNullable(createCaseRequest.getCiDescription()).ifPresent(value -> dataMap.put(KEY_CI_DESCRIPTION, value));
		ofNullable(createCaseRequest.getAddress()).ifPresent(address -> {
			ofNullable(address.getStreet()).ifPresent(value -> dataMap.put(KEY_STREET, value));
			ofNullable(address.getPostalCode()).ifPresent(value -> dataMap.put(KEY_POSTAL_CODE, value));
			ofNullable(address.getCity()).ifPresent(value -> dataMap.put(KEY_CITY, value));
		});
		ofNullable(createCaseRequest.getContactPerson()).ifPresent(value -> dataMap.put(KEY_CONTACT_PERSON, value));
		ofNullable(createCaseRequest.getPhoneNumber()).ifPresent(value -> dataMap.put(KEY_PHONE_NUMBER, value));
		ofNullable(createCaseRequest.getEmail()).ifPresent(value -> dataMap.put(KEY_EMAIL, value));

		return dataMap;
	}

	public static Case toCase(PobPayload pobPayload) {
		var data = pobPayload.getData();

		return Case.create()
			.withCaseId((String) data.get(KEY_ID))
			.withNote(toNote(pobPayload))
			.withDescription((String) data.get(KEY_DESCRIPTION))
			.withCaseType(getValue(pobPayload, JSON_PATH_CASE_TYPE))
			.withCustomerContact(getValue(pobPayload, JSON_PATH_CUSTOMER_CONTACT))
			.withPriority(getValue(pobPayload, JSON_PATH_PRIORITY))
			.withCaseCategory(getValue(pobPayload, JSON_PATH_CASE_CATEGORY))
			.withResponsibleGroup(getValue(pobPayload, JSON_PATH_RESPONSIBLE_GROUP))
			.withExternalServiceId((String) data.get(KEY_EXTERNAL_SERVICE_ID))
			.withExternalArticleNumber((String) data.get(KEY_EXTERNAL_ARTICLE_NUMBER))
			.withManagementCompany((String) data.get(KEY_MANAGEMENT_COMPANY))
			.withPersonal(getBooleanValue((String) data.get(KEY_PERSONAL)))
			.withOffice(getBooleanValue((String) data.get(KEY_OFFICE)))
			.withFreeText((String) data.get(KEY_FREE_TEXT))
			.withJoinContact(getValue(pobPayload, JSON_PATH_JOIN_CONTACT))
			.withResponsibilityNumber((String) data.get(KEY_RESPONSIBLE_NUMBER))
			.withSubaccount((String) data.get(KEY_SUBACCOUNT))
			.withBusinessNumber((String) data.get(KEY_BUSINESS_NUMBER))
			.withActivityNumber((String) data.get(KEY_ACTIVITY_NUMBER))
			.withProjectNumber((String) data.get(KEY_PROJECT_NUMBER))
			.withObjectNumber((String) data.get(KEY_OBJECT_NUMBER))
			.withCounterPart((String) data.get(KEY_COUNTERPART))
			.withContactPerson((String) data.get(KEY_CONTACT_PERSON))
			.withCiDescription((String) data.get(KEY_CI_DESCRIPTION))
			.withPhoneNumber((String) data.get(KEY_PHONE_NUMBER))
			.withEmail((String) data.get(KEY_EMAIL))
			.withAddress(Address.create()
				.withStreet((String) data.get(KEY_STREET))
				.withPostalCode((String) data.get(KEY_POSTAL_CODE))
				.withCity((String) data.get(KEY_CITY)));
	}

	private static void addBoolean(String key, Boolean value, Map<String, Object> dataMap) {
		if (Boolean.TRUE.equals(value)) {
			dataMap.put(key, "1");
		} else {
			dataMap.put(key, "0");
		}
	}

	private static Boolean getBooleanValue(String pobValue) {
		return ofNullable(pobValue)
			.map(value -> value.equals("1"))
			.orElse(null);
	}

	private static String getValue(PobPayload pobPayload, String jsonPath) {
		if (jsonPathExists(pobPayload, jsonPath)) {
			return extractValueFromJsonPath(pobPayload, jsonPath, true);
		}
		return null;
	}
}
