package se.sundsvall.supportcenter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ASSIGN_BACK;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.AWAITING_INFO;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.CANCELLED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED_ACCESSORIES;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED_ACTION_NEEDED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED_IT;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DESPATCHED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ENGINEER_START_WORK;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.OPEN;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ORDER_NOT_COMPLETED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ORDER_UPDATED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.PARTIALLY_DESPATCHED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.PICKING;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.PROCESSED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESERVED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESOLVED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.SCHEDULE_CHANGED;

class SupportCenterStatusTest {

	@Test
	void testEnums() {
		assertThat(SupportCenterStatus.values()).containsExactlyInAnyOrder(
			ASSIGN_BACK,
			AWAITING_INFO,
			CANCELLED,
			DELIVERED,
			DELIVERED_ACCESSORIES,
			DELIVERED_ACTION_NEEDED,
			DELIVERED_IT,
			DESPATCHED,
			OPEN,
			PARTIALLY_DESPATCHED,
			PICKING,
			PROCESSED,
			RESERVED,
			RESOLVED,
			ORDER_UPDATED,
			SCHEDULE_CHANGED,
			ENGINEER_START_WORK,
			ORDER_NOT_COMPLETED);
	}

	@Test
	void testEnumValues() {
		assertThat(ASSIGN_BACK.getValue()).isEqualTo("AssignBack");
		assertThat(AWAITING_INFO.getValue()).isEqualTo("Awaiting info");
		assertThat(CANCELLED.getValue()).isEqualTo("Cancelled");
		assertThat(DELIVERED.getValue()).isEqualTo("Delivered");
		assertThat(DELIVERED_ACCESSORIES.getValue()).isEqualTo("DeliveredAccessories");
		assertThat(DELIVERED_IT.getValue()).isEqualTo("DeliveredIT");
		assertThat(DELIVERED_ACTION_NEEDED.getValue()).isEqualTo("Delivered - Action Needed");
		assertThat(DESPATCHED.getValue()).isEqualTo("Despatched");
		assertThat(OPEN.getValue()).isEqualTo("Open");
		assertThat(PARTIALLY_DESPATCHED.getValue()).isEqualTo("Partially Despatched");
		assertThat(PICKING.getValue()).isEqualTo("Picking");
		assertThat(PROCESSED.getValue()).isEqualTo("Processed");
		assertThat(RESERVED.getValue()).isEqualTo("Reserved");
		assertThat(RESOLVED.getValue()).isEqualTo("Resolved");
		assertThat(ORDER_UPDATED.getValue()).isEqualTo("OrderUpdated");
		assertThat(SCHEDULE_CHANGED.getValue()).isEqualTo("ScheduleChanged");
		assertThat(ENGINEER_START_WORK.getValue()).isEqualTo("EngineerStartWork");
		assertThat(ORDER_NOT_COMPLETED.getValue()).isEqualTo("OrderNotCompleted");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"DeliveredIT", "deliveredit", "DELIVEREDIT", "dElIvErEdIt"
	})
	void fromValueIgnoresCase(final String value) {
		assertThat(SupportCenterStatus.fromValue(value)).contains(DELIVERED_IT);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"DeliveredIT ", " DeliveredIT", "  deliveredit  ", "\tDeliveredIT\n"
	})
	void fromValueIgnoresSurroundingWhitespace(final String value) {
		assertThat(SupportCenterStatus.fromValue(value)).contains(DELIVERED_IT);
	}

	@Test
	void fromValueMatchesValuesWithSpaces() {
		assertThat(SupportCenterStatus.fromValue("awaiting INFO")).contains(AWAITING_INFO);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		"   ", "In Process", "Delivered IT", "DeliveredITs", "awaiting  info"
	})
	void fromValueWithoutMatch(final String value) {
		assertThat(SupportCenterStatus.fromValue(value)).isEmpty();
	}
}
