package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void setUserData(ArrayList<UserData> userData) {
        this.userData = userData;
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
