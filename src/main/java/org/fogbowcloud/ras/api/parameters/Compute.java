package org.fogbowcloud.ras.api.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.UserData;

public class Compute implements OrderApiParameter {
    private String provider;
    private String cloudName;
    private String name;
    private int vCPU;
    private int memory;
    private int disk;
    private String imageId;
    private String publicKey;
    private ArrayList<UserData> userData;
    private List<String> networkIds;
    private Map<String, String> requirements;

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
    }

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

    public List<UserData> getUserData() {
        return userData;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }

    @Override
    public ComputeOrder getOrder() {
        ComputeOrder order = new ComputeOrder(provider, cloudName, name, vCPU, memory, disk, imageId, userData,
                publicKey, networkIds
        );
        order.setRequirements(requirements);
        return order;
    }
}
