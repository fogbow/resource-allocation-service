package org.fogbowcloud.manager.api.remote.xmpp;

public enum RemoteMethod {

    GET_REMOTE_INSTANCE("get_remote_instance"),
    GET_REMOTE_USER_QUOTA("get_remote_user_quota"),
    CREATE_REMOTE_ORDER("create_remote_order"),
    DELETE_REMOTE_ORDER("delete_remote_order"),
    REMOTE_ORDER_FULFILLED("remote_order_fulfilled"),
    REMOTE_ORDER_FAILED("remote_order_failed");

    private final String method;

    RemoteMethod(final String methodName) {
        this.method = methodName;
    }

    @Override
    public String toString() {
        return method;
    }

}
