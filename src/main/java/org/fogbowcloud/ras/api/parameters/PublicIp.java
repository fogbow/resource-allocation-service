package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;

public class PublicIp {
    private String provider;
    private String computeOrderId;

    public PublicIpOrder getOrder() {
        PublicIpOrder order = new PublicIpOrder(provider, computeOrderId);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getComputeOrderId() {
        return computeOrderId;
    }
}
