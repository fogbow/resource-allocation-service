package org.fogbowcloud.ras.api.parameters;

import java.util.List;
import org.fogbowcloud.ras.api.parameters.OrderApiParameter;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;

public class Compute implements OrderApiParameter<ComputeOrder> {

    private String name;
    private int vCPU;
    private int memory;
    private int disk;
    private String imageId;
    private String publicKey;
    private List<String> networkIds;

    public String getName() {
        return name;
    }

    public int getvCPU() {
        return vCPU;
    }

    public int getMemory() {
        return memory;
    }

    public int getDisk() {
        return disk;
    }

    public String getImageId() {
        return imageId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<String> getNetworkIds() {
        return networkIds;
    }

    @Override
    public ComputeOrder getOrder() {
        ComputeOrder computeOrder = new ComputeOrder(null, null,
            null, name, vCPU, memory, disk, imageId, null, publicKey, networkIds);
        return computeOrder;
    }

}
