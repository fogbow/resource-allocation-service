package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;

public class CloudConnectorSelector {

    private static Logger LOGGER = Logger.getLogger(CloudConnectorSelector.class);

    private static CloudConnectorSelector instance;

    private String localMemberId;
    private LocalCloudConnector localCloudConnector;
    private RemoteCloudConnector remoteCloudConnector;

    public static synchronized CloudConnectorSelector getInstance() {
        if (instance == null) {
            return new CloudConnectorSelector();
        }
        return instance;
    }

    public CloudConnector getCloudConnector(Order order) {
        CloudConnector cloudConnector;
        // Check if localCloudConnector, remoteCloudConnector null

        synchronized (order) {
            if (order.isProviderLocal(this.localMemberId)) {
                cloudConnector = this.localCloudConnector;
            } else {
                cloudConnector = this.remoteCloudConnector;
            }
        }

        return cloudConnector;
    }

    public CloudConnector getCloudConnector(String memberId) {
        CloudConnector cloudConnector;
        // Check if localCloudConnector, remoteCloudConnector null

        if (memberId.equals(this.localMemberId)) {
            cloudConnector = this.localCloudConnector;
        } else {
            cloudConnector = this.remoteCloudConnector;
        }

        return cloudConnector;
    }

    public void setLocalCloudConnector(LocalCloudConnector localCloudConnector) {
        this.localCloudConnector = localCloudConnector;
    }

    public void setRemoteCloudConnector(RemoteCloudConnector remoteCloudConnector) {
        this.remoteCloudConnector = remoteCloudConnector;
    }

    public void setLocalMemberId(String localMemberId) {
        this.localMemberId = localMemberId;
    }

    public String getLocalMemberId() {
        return localMemberId;
    }

    public LocalCloudConnector getLocalCloudConnector() {
        return this.localCloudConnector;
    }
}
