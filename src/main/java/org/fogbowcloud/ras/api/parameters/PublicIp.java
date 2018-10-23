package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;

public class PublicIp {
    private String provider;
    private String computeId;

    public PublicIpOrder getOrder() {
        PublicIpOrder order = new PublicIpOrder(provider, computeId);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getComputeId() {
        return computeId;
    }
}
