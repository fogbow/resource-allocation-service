package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;

public class PublicIpOrder extends Order {

    public String ip;
    private String computeInstanceId;
    
    public PublicIpOrder(String computeInstanceId) {
    	this.computeInstanceId = computeInstanceId;
	}

    @Override
    public ResourceType getType() {
        return ResourceType.PUBLIC_IP;
    }

    public String getComputeInstanceId() {
		return computeInstanceId;
	}
    
    @Override
    public String getSpec() {
        return "";
    }
}
