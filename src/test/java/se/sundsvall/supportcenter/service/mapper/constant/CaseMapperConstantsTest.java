package se.sundsvall.supportcenter.service.mapper.constant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.supportcenter.api.model.enums.NoteType;
import se.sundsvall.supportcenter.service.SupportCenterStatus;
import se.sundsvall.supportcenter.service.mapper.model.CustomStatusMapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ASSIGN_BACK;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.CANCELLED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED_ACTION_NEEDED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DESPATCHED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ENGINEER_START_WORK;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.OPEN;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ORDER_NOT_COMPLETED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ORDER_UPDATED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.PICKING;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.PROCESSED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESERVED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESOLVED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.SCHEDULE_CHANGED;

class CaseMapperConstantsTest {

	@Test
	void testDataKeys() { // NOSONAR
		assertThat(CaseMapperConstants.KEY_ACTION_NEEDED).isEqualTo("Virtual.ActionNeeded");
		assertThat(CaseMapperConstants.KEY_ACTION_NEEDED_DESCRIPTION).isEqualTo("Virtual.ActionNeededDescription");
		assertThat(CaseMapperConstants.KEY_ACTIVITY_NUMBER).isEqualTo("Virtual.Shop_Kst_Aktivitetsnummer");
		assertThat(CaseMapperConstants.KEY_BUSINESS_NUMBER).isEqualTo("Virtual.Shop_Kst_Verksamhetsnummer");
		assertThat(CaseMapperConstants.KEY_CASE_CATEGORY).isEqualTo("CaseCategory");
		assertThat(CaseMapperConstants.KEY_CASE_STATUS).isEqualTo("CaseStatus");
		assertThat(CaseMapperConstants.KEY_CASE_TYPE).isEqualTo("CaseType");
		assertThat(CaseMapperConstants.KEY_CI_DESCRIPTION).isEqualTo("Virtual.Shop_CI_Description");
		assertThat(CaseMapperConstants.KEY_CI_INFO).isEqualTo("CIInfo.Ci");
		assertThat(CaseMapperConstants.KEY_CI_INFO2).isEqualTo("CIInfo2.Ci");
		assertThat(CaseMapperConstants.KEY_CITY).isEqualTo("Virtual.Shop_Adr_Postort");
		assertThat(CaseMapperConstants.KEY_CLOSURE_CODE).isEqualTo("ClosureCode");
		assertThat(CaseMapperConstants.KEY_CONTACT_PERSON).isEqualTo("Virtual.Shop_Kontaktperson");
		assertThat(CaseMapperConstants.KEY_COUNTERPART).isEqualTo("Virtual.Shop_Kst_Motpart");
		assertThat(CaseMapperConstants.KEY_CUSTOMER_CONTACT).isEqualTo("Contact.Customer");
		assertThat(CaseMapperConstants.KEY_DESCRIPTION).isEqualTo("Description");
		assertThat(CaseMapperConstants.KEY_EMAIL).isEqualTo("Virtual.Shop_Epost");
		assertThat(CaseMapperConstants.KEY_EXTERNAL_ARTICLE_NUMBER).isEqualTo("Virtual.Shop_ExterntArtikelnummer");
		assertThat(CaseMapperConstants.KEY_EXTERNAL_CASE_ID).isEqualTo("Virtual.ExternalCaseId");
		assertThat(CaseMapperConstants.KEY_EXTERNAL_SERVICE_ID).isEqualTo("Virtual.Shop_ServiceIdExternalSync");
		assertThat(CaseMapperConstants.KEY_FREE_TEXT).isEqualTo("Virtual.Shop_Fritext");
		assertThat(CaseMapperConstants.KEY_ID).isEqualTo("Id");
		assertThat(CaseMapperConstants.KEY_JOIN_CONTACT).isEqualTo("Virtual.Shop_JoinContact");
		assertThat(CaseMapperConstants.KEY_MANAGEMENT_COMPANY).isEqualTo("Virtual.Shop_ForvaltningBolag");
		assertThat(CaseMapperConstants.KEY_OBJECT_NUMBER).isEqualTo("Virtual.Shop_Kst_Objektnummer");
		assertThat(CaseMapperConstants.KEY_OFFICE).isEqualTo("Virtual.Shop_Office");
		assertThat(CaseMapperConstants.KEY_PERSONAL).isEqualTo("Virtual.Shop_PersonligGemensam");
		assertThat(CaseMapperConstants.KEY_PHONE_NUMBER).isEqualTo("Virtual.Shop_Telefonnummer");
		assertThat(CaseMapperConstants.KEY_POSTAL_CODE).isEqualTo("Virtual.Shop_Adr_Postnr");
		assertThat(CaseMapperConstants.KEY_PRIORITY).isEqualTo("PriorityInfo.Priority");
		assertThat(CaseMapperConstants.KEY_PROJECT_NUMBER).isEqualTo("Virtual.Shop_Kst_Projektnummer");
		assertThat(CaseMapperConstants.KEY_RESPONSIBLE).isEqualTo("Responsible");
		assertThat(CaseMapperConstants.KEY_RESPONSIBLE_GROUP).isEqualTo("ResponsibleGroup");
		assertThat(CaseMapperConstants.KEY_RESPONSIBLE_NUMBER).isEqualTo("Virtual.Shop_Kst_Ansvarsnummer");
		assertThat(CaseMapperConstants.KEY_SHOP_CI_NAME).isEqualTo("Virtual.Shop_CI_Name");
		assertThat(CaseMapperConstants.KEY_STREET).isEqualTo("Virtual.Shop_Adr_Gatuadress");
		assertThat(CaseMapperConstants.KEY_SUBACCOUNT).isEqualTo("Virtual.Shop_Kst_Underkonto");
		assertThat(CaseMapperConstants.KEY_SUSPENSION).isEqualTo("Suspension");
	}

	@Test
	void testDefaultTypes() {
		assertThat(CaseMapperConstants.DEFAULT_TYPE).isEqualTo("Case");
	}

	@Test
	void testNoteValues() {
		assertThat(CaseMapperConstants.NOTE_STATUS_PART).isEqualTo("Status: '%s'");
		assertThat(CaseMapperConstants.NOTE_DELIVERED).isEqualTo("Beställning levererad");
	}

	@Test
	void testStatuses() {
		assertThat(CaseMapperConstants.STATUS_SOLVED).isEqualTo("Solved");
		assertThat(CaseMapperConstants.STATUS_IN_PROCESS).isEqualTo("In Process");
		assertThat(CaseMapperConstants.STATUS_CLOSED).isEqualTo("Closed");
		assertThat(CaseMapperConstants.STATUS_DELIVERED).isEqualTo("Delivered");
	}

	@Test
	void testClosureCodes() {
		assertThat(CaseMapperConstants.CLOSURE_CODE_CHANGE_OF_HARDWARE).isEqualTo("Byte av hårdvara");
		assertThat(CaseMapperConstants.CLOSURE_CODE_DELIVERED_HARDWARE).isEqualTo("Levererat Hårdvara - Service request");
		assertThat(CaseMapperConstants.CLOSURE_CODE_ADVANIA_DEFAULT_SOLUTION_TEXT).isEqualTo("Advania - Övriga lösningar Incident");
	}

	@Test
	void testCustomStatusMapKeys() {
		assertThat(CaseMapperConstants.CUSTOM_STATUS_MAP.keySet()).containsExactlyInAnyOrder(
			ASSIGN_BACK.getValue(),
			CANCELLED.getValue(),
			DELIVERED.getValue(),
			DELIVERED_ACTION_NEEDED.getValue(),
			DESPATCHED.getValue(),
			OPEN.getValue(),
			PICKING.getValue(),
			PROCESSED.getValue(),
			RESERVED.getValue(),
			RESOLVED.getValue(),
			ORDER_UPDATED.getValue(),
			SCHEDULE_CHANGED.getValue(),
			ENGINEER_START_WORK.getValue(),
			ORDER_NOT_COMPLETED.getValue());
	}

	@ParameterizedTest
	@MethodSource("customStatusMapValuesArgumentProvider")
	void testCustomStatusMapValues(SupportCenterStatus status, List<CustomStatusMapping> value) {
		assertThat(CaseMapperConstants.CUSTOM_STATUS_MAP).containsEntry(status.getValue(), value);
	}

	private static Stream<Arguments> customStatusMapValuesArgumentProvider() {
		return Stream.of(
			Arguments.of(ASSIGN_BACK, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_ACTION_NEEDED, true,
					CaseMapperConstants.KEY_ACTION_NEEDED_DESCRIPTION, "Åtgärdsbeskrivning i interna anteckningar",
					CaseMapperConstants.KEY_RESPONSIBLE_GROUP, "IT Support"),
					List.of(CaseMapperConstants.KEY_RESPONSIBLE))))),

			Arguments.of(CANCELLED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS,
					CaseMapperConstants.KEY_RESPONSIBLE_GROUP, "IT Support"),
					List.of(CaseMapperConstants.KEY_EXTERNAL_CASE_ID)))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(DELIVERED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_SOLVED,
					CaseMapperConstants.KEY_CLOSURE_CODE, CaseMapperConstants.CLOSURE_CODE_DELIVERED_HARDWARE)))
						.withStatusNoteType(NoteType.SOLUTION),
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_CLOSED))))),

			Arguments.of(DELIVERED_ACTION_NEEDED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS,
					CaseMapperConstants.KEY_RESPONSIBLE_GROUP, "IT Support")))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(DESPATCHED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS)))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(OPEN, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS)))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(PICKING, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS)))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(PROCESSED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS)))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(RESERVED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_IN_PROCESS)))
						.withStatusNoteType(NoteType.WORKNOTE))),

			Arguments.of(RESOLVED, List.of(
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_SOLVED,
					CaseMapperConstants.KEY_CLOSURE_CODE, CaseMapperConstants.CLOSURE_CODE_ADVANIA_DEFAULT_SOLUTION_TEXT,
					CaseMapperConstants.KEY_EXTERNAL_CASE_ID, "")))
						.withStatusNoteType(NoteType.WORKNOTE),
				createCustomStatusMapping(from(Map.of(
					CaseMapperConstants.KEY_CASE_STATUS, CaseMapperConstants.STATUS_CLOSED,
					CaseMapperConstants.KEY_EXTERNAL_CASE_ID, "")))))
		);

	}

	private static CustomStatusMapping createCustomStatusMapping(Map<String, Object> values) {
		return CustomStatusMapping.create()
			.withAttributes(values);
	}

	private static Map<String, Object> from(Map<String, Object> map) {
		return from(map, null);
	}

	private static Map<String, Object> from(Map<String, Object> map, List<String> nullValues) {
		Map<String, Object> hashMap = new HashMap<>();

		hashMap.putAll(map);
		ofNullable(nullValues).orElse(Collections.emptyList())
			.stream()
			.forEach(key -> hashMap.put(key, null));

		return hashMap;
	}
}
