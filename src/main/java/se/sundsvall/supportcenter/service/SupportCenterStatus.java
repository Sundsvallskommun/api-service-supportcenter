package se.sundsvall.supportcenter.service;

import java.util.Arrays;
import java.util.Optional;

public enum SupportCenterStatus {

	// NETSET statuses (order flow)
	PROCESSED("Processed"),
	RESERVED("Reserved"),
	PICKING("Picking"),
	PARTIALLY_DESPATCHED("Partially Despatched"),
	DESPATCHED("Despatched"),
	DELIVERED("Delivered"),
	DELIVERED_IT("DeliveredIT"),
	DELIVERED_ACCESSORIES("DeliveredAccessories"),
	DELIVERED_ACTION_NEEDED("Delivered - Action Needed"),

	// CUBE statuses (support flow)
	ASSIGN_BACK("AssignBack"),
	AWAITING_INFO("Awaiting info"),
	CANCELLED("Cancelled"),
	OPEN("Open"),
	RESOLVED("Resolved"),
	ORDER_UPDATED("OrderUpdated"),
	SCHEDULE_CHANGED("ScheduleChanged"),
	ENGINEER_START_WORK("EngineerStartWork"),
	ORDER_NOT_COMPLETED("OrderNotCompleted");

	private final String value;

	SupportCenterStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Finds the status with the provided value, without regard to case and surrounding whitespace.
	 *
	 * @param  value the value to look for
	 * @return       the matching status, or an empty optional if there is none
	 */
	public static Optional<SupportCenterStatus> fromValue(final String value) {
		return Optional.ofNullable(value)
			.map(String::strip)
			.flatMap(strippedValue -> Arrays.stream(values())
				.filter(status -> status.value.equalsIgnoreCase(strippedValue))
				.findFirst());
	}
}
