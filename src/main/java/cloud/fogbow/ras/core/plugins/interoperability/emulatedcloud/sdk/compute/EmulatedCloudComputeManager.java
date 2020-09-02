package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.models.EmulatedCompute;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudComputeManager implements ResourceManager<EmulatedCompute> {
    private static EmulatedCloudComputeManager instance;
    private Map<String, EmulatedCompute> computes;

    private EmulatedCloudComputeManager() {
        this.computes = new HashMap<>();
    }

    public static EmulatedCloudComputeManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudComputeManager();
        }
        return instance;
    }

    @Override
    public Optional<EmulatedCompute> find(String instanceId) {
        return Optional.ofNullable(this.computes.get(instanceId));
    }

    @Override
    public List<EmulatedCompute> list() {
        return new ArrayList<>(this.computes.values());
    }

    @Override
    public String create(EmulatedCompute compute) {
        if (compute == null || !EmulatedCloudUtils.validateInstanceId(compute.getInstanceId())) {
            String message = String.format(
                    EmulatedCloudConstants.Exception.UNABLE_TO_CREATE_RESOURCE_INVALID_INSTANCE_ID_S, compute.getInstanceId());
            throw new InvalidParameterException(message);
        }

        this.computes.put(compute.getInstanceId(), compute);
        return compute.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (this.computes.containsKey(instanceId)) {
            this.computes.remove(instanceId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }
}
