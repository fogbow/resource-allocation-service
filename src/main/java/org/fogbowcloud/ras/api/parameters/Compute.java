package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.api.parameters.OrderApiParameter;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;

public class Compute implements OrderApiParameter<ComputeOrder> {

    private String name;
    private int vCPU;
    private int memory;
    private int disk;
    private String imageId;
    private String publicKey;

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

    @Override
    public ComputeOrder getOrder() {
        ComputeOrder computeOrder = new ComputeOrder(null, null,
            null, name, vCPU, memory, disk, imageId, null, publicKey, null);
        return computeOrder;
    }

}
