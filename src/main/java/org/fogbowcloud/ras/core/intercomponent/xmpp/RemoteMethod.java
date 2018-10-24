package org.fogbowcloud.ras.core.intercomponent.xmpp;

public enum RemoteMethod {
    REMOTE_CREATE_ORDER("remoteCreateOrder"),
    REMOTE_DELETE_ORDER("remoteDeleteOrder"),
    REMOTE_GET_ALL_IMAGES("remoteGetAllImages"),
    REMOTE_GET_IMAGE("remoteGetImage"),
    REMOTE_GET_ORDER("remoteGetOrder"),
    REMOTE_GET_USER_QUOTA("remoteGetUserQuota"),
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
