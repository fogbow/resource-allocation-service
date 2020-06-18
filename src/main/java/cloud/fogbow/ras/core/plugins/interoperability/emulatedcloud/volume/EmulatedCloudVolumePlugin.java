package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.volume;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedVolume;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2.GetVolumeResponse;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class EmulatedCloudVolumePlugin implements VolumePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudVolumePlugin.class);

    private Properties properties;

    public EmulatedCloudVolumePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        Map<String, String> requirements = volumeOrder.getRequirements();

        EmulatedVolume volume;

        String size = String.valueOf(volumeOrder.getVolumeSize());
        String instanceName = volumeOrder.getName();
        String name = EmulatedCloudUtils.getName(instanceName);

        volume = generateJsonEntityToCreateInstance(size, name);

        String jsonVolume = volume.toJson();

        String newVolumePath = EmulatedCloudUtils.getResourcePath(this.properties, volume.getId());

        try {

            EmulatedCloudUtils.saveFileContent(newVolumePath, jsonVolume);

        } catch (IOException e) {

            throw new FogbowException(e.getMessage());
        }

        return volume.getId();
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        String volumeId = volumeOrder.getInstanceId();

        String volumeJson;

        try {
            volumeJson = EmulatedCloudUtils.getFileContentById(this.properties, volumeId);

        } catch (IOException e) {

            throw new InstanceNotFoundException(e.getMessage());
        }

        EmulatedVolume volume = EmulatedVolume.fromJson(volumeJson);

        String name = volume.getName();
        String size = volume.getSize();
        String status = volume.getStatus();

        return new VolumeInstance(volumeId, name, status, Integer.parseInt(size));
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        String volumeId = volumeOrder.getId();
        String volumePath = EmulatedCloudUtils.getResourcePath(this.properties, volumeId);

        EmulatedCloudUtils.deleteFile(volumePath);
    }

    protected EmulatedVolume generateJsonEntityToCreateInstance(String size, String name) throws JSONException {
        EmulatedVolume emulatedVolume =
                new EmulatedVolume.Builder()
                        .id(EmulatedCloudUtils.getRandomUUID())
                        .name(name)
                        .size(size)
                        .build();

        return emulatedVolume;
    }

    protected VolumeInstance getInstanceFromJson(String json) throws InternalServerErrorException {
        try {
            GetVolumeResponse getVolumeResponse = GetVolumeResponse.fromJson(json);
            String id = getVolumeResponse.getId();
            String name = getVolumeResponse.getName();
            int size = getVolumeResponse.getSize();
            String status = getVolumeResponse.getStatus();
            return new VolumeInstance(id, status, name, size);
        } catch (Exception e) {
            LOGGER.error(Messages.Log.ERROR_WHILE_GETTING_VOLUME_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_GETTING_VOLUME_INSTANCE);
        }
    }
}
