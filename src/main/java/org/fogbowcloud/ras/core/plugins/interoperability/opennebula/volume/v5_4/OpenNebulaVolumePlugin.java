package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import org.apache.log4j.Logger;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.VolumePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4.CreateVolumeRequest;

public class OpenNebulaVolumePlugin implements VolumePlugin<Token> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);

    public static final String OPENNEBULA_DATABLOCK_IMAGE_TYPE = "DATABLOCK";
    public static final String OPENNEBULA_RAW_FSTYPE = "raw";
    public static final String OPENNEBULA_BLOCK_DISK_TYPE = "BLOCK";
    public static final String OPENNEBULA_DATASTORE_DEFAULT_DEVICE_PREFIX = "vd";
    public static final String OPENNEBULA_PERSISTENT_DISK_YES = "YES";

    public OpenNebulaVolumePlugin() {
        super();
        // TODO
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String volumeName = volumeOrder.getName();
        int volumeSize = volumeOrder.getVolumeSize();

        CreateVolumeRequest request = new CreateVolumeRequest.Builder()
                .name(volumeName)
                .size(volumeSize)
                .persistent(OPENNEBULA_PERSISTENT_DISK_YES)
                .type(OPENNEBULA_DATABLOCK_IMAGE_TYPE)
                .fstype(OPENNEBULA_RAW_FSTYPE)
                .diskType(OPENNEBULA_BLOCK_DISK_TYPE)
                .devPrefix(OPENNEBULA_DATASTORE_DEFAULT_DEVICE_PREFIX)
                .build();

        String template  = request.getVolumeImageRequestTemplate().generateTemplate();

        return template;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {

    }
}