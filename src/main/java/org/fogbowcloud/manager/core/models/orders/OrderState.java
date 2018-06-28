package org.fogbowcloud.manager.core.models.orders;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderState {
    OPEN("open"),
    PENDING("pending"),
    SPAWNING("spawning"),
    FULFILLED("fulfilled"),
    FAILED("failed"),
    CLOSED("closed");

    private String value;

    OrderState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static OrderState fromValue(String value) {
        for (OrderState orderState : OrderState.values()) {
            if (orderState.getValue().equalsIgnoreCase(value)) {
                return orderState;
            }
        }

        return null;
    }
}
