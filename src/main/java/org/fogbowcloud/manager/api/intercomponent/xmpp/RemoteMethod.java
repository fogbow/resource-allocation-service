package org.fogbowcloud.manager.api.intercomponent.xmpp;

public enum RemoteMethod {

    REMOTE_GET_INSTANCE("remoteGetInstance"),
    REMOTE_GET_USER_QUOTA("remoteGetUserQuota"),
    REMOTE_GET_USER_ALLOCATION("remoteGetUserAllocation"),
    REMOTE_CREATE_ORDER("remoteCreateOrder"),
    REMOTE_DELETE_ORDER("remoteDeleteOrder"),
    REMOTE_NOTIFY_EVENT("remoteNotifyEvent");

    private final String method;

    RemoteMethod(final String methodName) {
        this.method = methodName;
    }

    @Override
    public String toString() {
        return method;
    }

}
