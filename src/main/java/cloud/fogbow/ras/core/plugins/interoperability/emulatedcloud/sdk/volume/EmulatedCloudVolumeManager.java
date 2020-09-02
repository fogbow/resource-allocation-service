package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.models.EmulatedVolume;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudVolumeManager implements ResourceManager<EmulatedVolume> {
    private Map<String, EmulatedVolume> volumes;
    private static EmulatedCloudVolumeManager instance;

    private EmulatedCloudVolumeManager() {
        this.volumes = new HashMap<>();
    }

    public static EmulatedCloudVolumeManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudVolumeManager();
        }
        return instance;
    }

    @Override
    public Optional<EmulatedVolume> find(String instanceId) {
        return Optional.ofNullable(this.volumes.get(instanceId));
    }

    @Override
    public List<EmulatedVolume> list() {
        return new ArrayList<>(volumes.values());
    }

    @Override
    public String create(EmulatedVolume volume) {
        if (volume == null || !EmulatedCloudUtils.validateInstanceId(volume.getInstanceId())) {
            String message = String.format(
                    EmulatedCloudConstants.Exception.UNABLE_TO_CREATE_RESOURCE_INVALID_INSTANCE_ID_S, volume.getInstanceId());
            throw new InvalidParameterException(message);
        }

        this.volumes.put(volume.getInstanceId(), volume);
        return volume.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (this.volumes.containsKey(instanceId)) {
            this.volumes.remove(instanceId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }
}
