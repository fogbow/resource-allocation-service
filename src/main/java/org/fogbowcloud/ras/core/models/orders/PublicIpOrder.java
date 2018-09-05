package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;

public class PublicIpOrder extends Order {

    private String computeOrderId;
    
    @Override
    public ResourceType getType() {
        return ResourceType.PUBLIC_IP;
    }

    public String getComputeOrderId() {
		return computeOrderId;
	}
    
    @Override
    public String getSpec() {
        return "";
    }
}
