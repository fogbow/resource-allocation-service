package org.fogbowcloud.manager.core.models.orders;

public enum OrderState {
	
	OPEN("open"), PENDING("pending"), SPAWNING("spawning"), FULFILLED("fulfilled"), FAILED("failed"), CLOSED("closed");

	private String value;

	private OrderState(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
