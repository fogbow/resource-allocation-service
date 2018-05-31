package org.fogbowcloud.manager.api.remote.xmpp;

public enum RemoteMethod {

    GET_REMOTE_INSTANCE("getRemoteInstance"),
    GET_REMOTE_USER_QUOTA("getRemoteUserQuota"),
    CREATE_REMOTE_ORDER("createRemoteOrder"),
    DELETE_REMOTE_ORDER("deleteRemoteOrder"),
    REMOTE_ORDER_FULFILLED("remoteOrderFulfilled"),
    REMOTE_ORDER_FAILED("remoteOrderFailed");

    private final String method;

    RemoteMethod(final String methodName) {
        this.method = methodName;
    }

    @Override
    public String toString() {
        return method;
    }

}
