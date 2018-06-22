package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.OrderController;

public class CloudConnectorFactory {

    private static CloudConnectorFactory instance;

    private String localMemberId;
    private AaController aaController;
    private OrderController orderController;
    private CloudPluginsHolder cloudPluginsHolder;

    public static synchronized CloudConnectorFactory getInstance() {
        if (instance == null) {
            instance = new CloudConnectorFactory();
        }
        return instance;
    }

    public CloudConnector getCloudConnector(String memberId) {
        CloudConnector cloudConnector;

        if (memberId.equals(this.localMemberId)) {
                cloudConnector = new LocalCloudConnector(this.localMemberId, this.aaController, this.orderController,
                        this.cloudPluginsHolder);
        } else {
                cloudConnector = new RemoteCloudConnector(memberId);
        }

        return cloudConnector;
    }

    public void setAaController(AaController aaController) {
        this.aaController = aaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public void setCloudPluginsHolder(CloudPluginsHolder cloudPluginsHolder) {
        this.cloudPluginsHolder = cloudPluginsHolder;
    }

    public void setLocalMemberId(String localMemberId) {
        this.localMemberId = localMemberId;
    }
}
