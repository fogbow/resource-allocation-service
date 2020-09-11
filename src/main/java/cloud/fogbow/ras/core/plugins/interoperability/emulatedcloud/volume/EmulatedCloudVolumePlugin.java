package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.volume;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.EmulatedCloudVolumeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.models.EmulatedVolume;
import org.apache.log4j.Logger;

import java.util.Optional;
import java.util.Properties;

public class EmulatedCloudVolumePlugin implements VolumePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudVolumePlugin.class);

    private Properties properties;

    public EmulatedCloudVolumePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        EmulatedCloudVolumeManager volumeManager = EmulatedCloudVolumeManager.getInstance();
        EmulatedVolume volume = createEmulatedVolume(volumeOrder);
        String instanceId = volumeManager.create(volume);
        updateInstanceAllocation(volumeOrder);
        return instanceId;
    }

    private EmulatedVolume createEmulatedVolume(VolumeOrder volumeOrder) {
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String name = volumeOrder.getName();
        String size = String.valueOf(volumeOrder.getVolumeSize());
        EmulatedVolume emulatedVolume =
                new EmulatedVolume.Builder()
                        .instanceId(instanceId)
                        .name(name)
                        .size(size)
                        .status(EmulatedCloudStateMapper.ACTIVE_STATUS)
                        .build();

        return emulatedVolume;
    }

    private void updateInstanceAllocation(VolumeOrder volumeOrder) {
        synchronized (volumeOrder) {
            int size = volumeOrder.getVolumeSize();
            VolumeAllocation allocation = new VolumeAllocation(size);
            volumeOrder.setActualAllocation(allocation);
        }
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = volumeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        EmulatedCloudVolumeManager volumeManager = EmulatedCloudVolumeManager.getInstance();
        Optional<EmulatedVolume> volumeOptional = volumeManager.find(instanceId);

        if (volumeOptional.isPresent()) {
            return buildVolumeInstance(volumeOptional.get());
        } else {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    private VolumeInstance buildVolumeInstance(EmulatedVolume volume) {
        String instanceId = volume.getInstanceId();
        String name = volume.getName();
        String size = volume.getSize();
        String status = volume.getStatus();

        return new VolumeInstance(instanceId, status, name, Integer.parseInt(size));
    }

    @Override
    public boolean isReady(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = volumeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        EmulatedCloudVolumeManager volumeManager = EmulatedCloudVolumeManager.getInstance();
        volumeManager.delete(instanceId);
    }
}
