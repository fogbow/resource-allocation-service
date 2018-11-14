package org.fogbowcloud.ras.core.intercomponent.xmpp;

public enum RemoteMethod {
    REMOTE_CREATE_ORDER("remoteCreateOrder"),
    REMOTE_DELETE_ORDER("remoteDeleteOrder"),
    REMOTE_GET_ALL_IMAGES("remoteGetAllImages"),
    REMOTE_GET_IMAGE("remoteGetImage"),
    REMOTE_GET_ORDER("remoteGetOrder"),
    REMOTE_GET_USER_QUOTA("remoteGetUserQuota"),
    REMOTE_NOTIFY_EVENT("remoteNotifyEvent"),
    REMOTE_CREATE_SECURITY_GROUP("remoteCreateSecurityGroup"),
    REMOTE_GET_SECURITY_GROUP("remoteGetSecurityGroup"),
    REMOTE_DELETE_SECURITY_GROUP("remoteDeleteSecurityGroup");

    private final String method;

    RemoteMethod(final String methodName) {
        this.method = methodName;
    }

    @Override
    public String toString() {
        return method;
    }
}
