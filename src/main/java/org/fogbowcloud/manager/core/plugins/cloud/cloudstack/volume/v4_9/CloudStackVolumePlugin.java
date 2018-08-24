package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse.DiskOffering;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetVolumeResponse.Volume;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

public class CloudStackVolumePlugin implements VolumePlugin<CloudStackToken>{

    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);
    
    private static final String CLOUDSTACK_ZONE_ID_KEY = "cloudstack_zone_id";
    
    private static final int FIRST_ELEMENT_POSITION = 0;

    private HttpRequestClientUtil client;
    private boolean diskOfferingCompatible;
    private String zoneId;

    public CloudStackVolumePlugin() {
        HomeDir homeDir = HomeDir.getInstance();
        String filePath = homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        this.zoneId = properties.getProperty(CLOUDSTACK_ZONE_ID_KEY);
        this.client = new HttpRequestClientUtil();
    }
    
    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudStackToken localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Requesting volume instance with token " + localUserAttributes);
        String diskOfferingId = getDiskOfferingId(volumeOrder, localUserAttributes);

        CreateVolumeRequest request;
        if (isDiskOfferingCompatible()) {
            request = createVolumeCompatible(volumeOrder, diskOfferingId);
        } else {
            request = createVolumeCustomized(volumeOrder, diskOfferingId);
        }
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        
        LOGGER.debug("Getting instance from json: " + jsonResponse);
        CreateVolumeResponse volumeResponse = CreateVolumeResponse.fromJson(jsonResponse);
        String volumeId = volumeResponse.getId();
        return volumeId;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, CloudStackToken localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Getting volume instance " + volumeInstanceId + ", with token " + localUserAttributes);
        GetVolumeRequest request = new GetVolumeRequest.Builder()
                .id(volumeInstanceId)
                .build();
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        
        GetVolumeResponse response = GetVolumeResponse.fromJson(jsonResponse);
        List<GetVolumeResponse.Volume> volumes = response.getVolumes();
        
        if (volumes != null && volumes.size() > 0) {
            // since an id were specified, there should be no more than one volume in the response
            return loadInstance(volumes.get(FIRST_ELEMENT_POSITION));
        } else {
            throw new InstanceNotFoundException();
        }

    }

    @Override
    public void deleteInstance(String volumeInstanceId, CloudStackToken localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Removing volume instance " + volumeInstanceId + ". With token: " + localUserAttributes);
        
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().id(volumeInstanceId).build();
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        
        DeleteVolumeResponse volumeResponse = DeleteVolumeResponse.fromJson(jsonResponse);
        boolean success = volumeResponse.isSuccess();
        
        if (!success) {
            String message = volumeResponse.getDisplayText();
            LOGGER.debug("Could not remove volume instance " + volumeInstanceId + ". " + message);
        }
    }

    private String getDiskOfferingId(VolumeOrder volumeOrder, Token localUserAttributes)
            throws FogbowManagerException {

        int volumeSize = volumeOrder.getVolumeSize();
        LOGGER.debug("Getting disk offerings available in cloudstack with token: " 
                + localUserAttributes + " and volume size: " + volumeSize);

        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder().build();
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        String diskOfferingId = getDiskOfferingIdCompatible(volumeSize, jsonResponse);
        
        if (!isDiskOfferingCompatible()) {
            diskOfferingId = getDiskOfferingIdCustomized(volumeSize, jsonResponse);
        }

        return diskOfferingId;
    }
    
    private String getDiskOfferingIdCustomized(int volumeSize, String jsonResponse) {
        GetAllDiskOfferingsResponse response = GetAllDiskOfferingsResponse.fromJson(jsonResponse);
        boolean customized;
        int size;
        for (DiskOffering diskOffering : response.getDiskOfferings()) {
            customized = diskOffering.isCustomized();
            size = diskOffering.getDiskSize();
            if (customized && size == 0) {
                return diskOffering.getId();
            }
        }
        LOGGER.debug("There is not disk offering customized in cloudstack");
        return null;
    }

    private String getDiskOfferingIdCompatible(int volumeSize, String jsonResponse) {
        GetAllDiskOfferingsResponse response = GetAllDiskOfferingsResponse.fromJson(jsonResponse);
        int size;
        for (DiskOffering diskOffering : response.getDiskOfferings()) {
            size = diskOffering.getDiskSize();
            if (size == volumeSize) {
                this.diskOfferingCompatible = true;
                return diskOffering.getId();
            }
        }
        LOGGER.debug("There is not compatible disk offering in cloudstack");
        this.diskOfferingCompatible = false;
        return null;
    }

    private CreateVolumeRequest createVolumeCustomized(VolumeOrder volumeOrder,
            String diskOfferingId) throws InvalidParameterException {
        
        String name = volumeOrder.getVolumeName();
        String size = String.valueOf(volumeOrder.getVolumeSize());
        return new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(name)
                .diskOfferingId(diskOfferingId)
                .size(size)
                .build();
    }

    private CreateVolumeRequest createVolumeCompatible(VolumeOrder volumeOrder, String diskOfferingId)
            throws InvalidParameterException {
        
        String name = volumeOrder.getVolumeName();
        return new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(name)
                .diskOfferingId(diskOfferingId)
                .build();
    }
    
    private VolumeInstance loadInstance(Volume volume) {
        String id = volume.getId();
        String state = volume.getState();
        String name = volume.getName();
        int size = volume.getSize();
        
        InstanceState instanceState = CloudStackStateMapper.map(ResourceType.VOLUME, state);
        
        VolumeInstance volumeInstance = new VolumeInstance(id, instanceState, name, size);
        return volumeInstance;
    }

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }

    protected boolean isDiskOfferingCompatible() {
        return diskOfferingCompatible;
    }
    
    public String getZoneId() {
        return this.zoneId;
    }

}
