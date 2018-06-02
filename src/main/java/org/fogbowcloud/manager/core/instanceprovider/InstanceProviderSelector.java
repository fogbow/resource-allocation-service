package org.fogbowcloud.manager.core.instanceprovider;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;

public class InstanceProviderSelector {

    private static Logger LOGGER = Logger.getLogger(InstanceProviderSelector.class);

    private static InstanceProviderSelector instance;

    private LocalInstanceProvider localInstanceProvider;
    private RemoteInstanceProvider remoteInstanceProvider;
    private String localMemberId;

    public static synchronized InstanceProviderSelector getInstance() {
        if (instance == null) {
            return new InstanceProviderSelector();
        }
        return instance;
    }

    // FIXME: Remove duplicates of this code
    public InstanceProvider getInstanceProvider(Order order) {
        InstanceProvider instanceProvider;
        // Check if localInstanceProvider, remoteInstanceProvider null

        synchronized (order) {
            if (order.isProviderLocal(this.localMemberId)) {
                instanceProvider = this.localInstanceProvider;
            } else {
                instanceProvider = this.remoteInstanceProvider;
            }
        }

        return instanceProvider;
    }

    public void setLocalInstanceProvider(LocalInstanceProvider localInstanceProvider) {
        this.localInstanceProvider = localInstanceProvider;
    }

    public void setRemoteInstanceProvider(RemoteInstanceProvider remoteInstanceProvider) {
        this.remoteInstanceProvider = remoteInstanceProvider;
    }

    public LocalInstanceProvider getLocalInstanceProvider() {
        return this.localInstanceProvider;
    }

    public void setLocalMemberId(String localMemberId) {
        this.localMemberId = localMemberId;
    }
}
