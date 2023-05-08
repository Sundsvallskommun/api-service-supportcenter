package se.sundsvall.supportcenter.service;

public enum SupportCenterStatus {

	// NETSET statuses (order flow)
	PROCESSED("Processed"),
	RESERVED("Reserved"),
	PICKING("Picking"),
	PARTIALLY_DESPATCHED("Partially Despatched"),
	DESPATCHED("Despatched"),
	DELIVERED("Delivered"),
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
}
