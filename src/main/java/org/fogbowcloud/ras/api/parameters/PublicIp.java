package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;

public class PublicIp extends OrderApiParameter<PublicIpOrder> {

    private String computeOrderId;

    @Override
    public PublicIpOrder getOrder() {
        return new PublicIpOrder(null, null, null, null, computeOrderId);
    }

    public String getComputeOrderId() {
        return computeOrderId;
    }
}
