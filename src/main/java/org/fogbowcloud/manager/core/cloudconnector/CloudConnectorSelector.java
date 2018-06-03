package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;

public class CloudConnectorSelector {

    private static Logger LOGGER = Logger.getLogger(CloudConnectorSelector.class);

    private static CloudConnectorSelector instance;

    private String localMemberId;
    private LocalCloudConnector localInstanceProvider;
    private RemoteCloudConnector remoteInstanceProvider;

    public static synchronized CloudConnectorSelector getInstance() {
        if (instance == null) {
            return new CloudConnectorSelector();
        }
        return instance;
    }

    public CloudConnector getInstanceProvider(Order order) {
        CloudConnector cloudConnector;
        // Check if localInstanceProvider, remoteInstanceProvider null

        synchronized (order) {
            if (order.isProviderLocal(this.localMemberId)) {
                cloudConnector = this.localInstanceProvider;
            } else {
                cloudConnector = this.remoteInstanceProvider;
            }
        }

        return cloudConnector;
    }

    public CloudConnector getInstanceProvider(String memberId) {
        CloudConnector cloudConnector;
        // Check if localInstanceProvider, remoteInstanceProvider null

        if (memberId.equals(this.localMemberId)) {
            cloudConnector = this.localInstanceProvider;
        } else {
            cloudConnector = this.remoteInstanceProvider;
        }

        return cloudConnector;
    }

    public void setLocalInstanceProvider(LocalCloudConnector localInstanceProvider) {
        this.localInstanceProvider = localInstanceProvider;
    }

    public void setRemoteInstanceProvider(RemoteCloudConnector remoteInstanceProvider) {
        this.remoteInstanceProvider = remoteInstanceProvider;
    }

    public void setLocalMemberId(String localMemberId) {
        this.localMemberId = localMemberId;
    }

    public String getLocalMemberId() {
        return localMemberId;
    }

    public LocalCloudConnector getLocalInstanceProvider() {
        return this.localInstanceProvider;
    }
}
