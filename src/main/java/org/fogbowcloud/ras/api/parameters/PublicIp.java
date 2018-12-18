package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;

public class PublicIp implements OrderApiParameter{
    private String provider;
    private String cloudName;
    private String computeId;

    @Override
    public PublicIpOrder getOrder() {
        PublicIpOrder order = new PublicIpOrder(provider, cloudName, computeId);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getComputeId() {
        return computeId;
    }
}
