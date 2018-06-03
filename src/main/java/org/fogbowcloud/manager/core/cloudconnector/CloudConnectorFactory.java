package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.services.AaController;
import org.jamppa.component.PacketSender;

public class CloudConnectorFactory {

    private static Logger LOGGER = Logger.getLogger(CloudConnectorFactory.class);

    private static CloudConnectorFactory instance;

    private String localMemberId;
    private PacketSender packetSender;
    private AaController aaController;
    private OrderController orderController;
    private CloudPluginsHolder cloudPluginsHolder;

    public static synchronized CloudConnectorFactory getInstance() {
        if (instance == null) {
            return new CloudConnectorFactory();
        }
        return instance;
    }

    public CloudConnector getCloudConnector(String memberId) {
        CloudConnector cloudConnector;

        if (memberId.equals(this.localMemberId)) {
                cloudConnector = new LocalCloudConnector(this.localMemberId, this.aaController, this.orderController,
                        this.cloudPluginsHolder);
        } else {
                cloudConnector = new RemoteCloudConnector(memberId, this.packetSender);
        }

        return cloudConnector;
    }

    public void setPacketSender(PacketSender packetSender) {
        this.packetSender = packetSender;
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
