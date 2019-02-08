package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.core.models.orders.VolumeOrder;

import java.util.Map;

public class Volume implements OrderApiParameter {
    private String provider;
    private String cloudName;
    private String name;
    private int volumeSize;
    private Map<String, String> requirements;

    @Override
    public VolumeOrder getOrder() {
        VolumeOrder order = new VolumeOrder(provider, cloudName, name, volumeSize);
        order.setRequirements(requirements);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getName() {
        return name;
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }
}
