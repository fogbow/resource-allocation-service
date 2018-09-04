package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;

public class PublicIpOrder extends Order {

    public String ip;

    @Override
    public ResourceType getType() {
        return ResourceType.PUBLIC_IP;
    }

    @Override
    public String getSpec() {
        return "";
    }
}
