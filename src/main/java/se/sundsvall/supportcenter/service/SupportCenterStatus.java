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
	RESOLVED("Resolved");
	
	private final String value;
	
	private SupportCenterStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
