package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse.DiskOffering;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetAllVolumesResponse.Volume;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

public class CloudStackVolumePlugin implements VolumePlugin<Token>{

    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);
    
    private static final String CLOUDSTACK_ZONE_ID_KEY = "cloudstack_zone_id";

    private HttpRequestClientUtil client;
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
    public String requestInstance(VolumeOrder volumeOrder, Token localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Requesting volume instance with token " + localUserAttributes);
        
        String name = volumeOrder.getVolumeName();
        int volumeSize = volumeOrder.getVolumeSize();
        
        String diskOfferingsResponse = null;
        try {
            diskOfferingsResponse = doGetRequestToListDiskOfferings(volumeOrder, localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        
        String diskOfferingId = getDiskOfferingIdCompatible(volumeSize, diskOfferingsResponse);

        CreateVolumeRequest request;
        if (diskOfferingId != null) {
            request = new CreateVolumeRequest.Builder()
                    .zoneId(this.zoneId)
                    .name(name)
                    .diskOfferingId(diskOfferingId)
                    .build();
        } else {
            diskOfferingId = getDiskOfferingIdCustomized(volumeSize, diskOfferingsResponse);

            String size = String.valueOf(volumeSize);
            request = new CreateVolumeRequest.Builder()
                    .zoneId(this.zoneId)
                    .name(name)
                    .diskOfferingId(diskOfferingId)
                    .size(size)
                    .build();
        }
        // TODO throw exception for diskOfferingId null...
                
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        
        LOGGER.debug("Getting instance from json: " + jsonResponse);
        CreateVolumeResponse volumeResponse = CreateVolumeResponse.fromJson(jsonResponse);

        return volumeResponse.getId();
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Getting storage instance " + volumeInstanceId + ", with token " + localUserAttributes);
        
        List<VolumeInstance> volumeInstances = getAllInstances(localUserAttributes);
        for (VolumeInstance volumeInstance : volumeInstances) {
            if (volumeInstance.getId().equals(volumeInstanceId)) {
                return volumeInstance;
            }
        }
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Removing storage instance " + volumeInstanceId + ". With token: " + localUserAttributes);
        
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
            LOGGER.debug("Could not remove storage instance " + volumeInstanceId + ". " + message);
        }
    }

    
    private String doGetRequestToListDiskOfferings(VolumeOrder volumeOrder, Token localUserAttributes)
            throws InvalidParameterException, UnauthorizedRequestException, HttpResponseException,
            UnavailableProviderException {

        LOGGER.debug("Getting disk offerings available in cloudstack with token: "
                + localUserAttributes + " and volume size: " + volumeOrder.getVolumeSize());

        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder().build();
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        return jsonResponse;
        
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
        return null;
    }

    private String getDiskOfferingIdCompatible(int volumeSize, String jsonResponse) {
        GetAllDiskOfferingsResponse response = GetAllDiskOfferingsResponse.fromJson(jsonResponse);
        int size;
        for (DiskOffering diskOffering : response.getDiskOfferings()) {
            size = diskOffering.getDiskSize();
            if (size == volumeSize) {
                return diskOffering.getId();
            }
        }
        return null;
    }
    
    private List<VolumeInstance> getAllInstances(Token localUserAttributes)
            throws FogbowManagerException {

        LOGGER.debug("Listing cloudstack volumes with this token value: "
                + localUserAttributes.getTokenValue());

        GetVolumeRequest request = new GetVolumeRequest.Builder().build();
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse =
                    this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetAllVolumesResponse response = GetAllVolumesResponse.fromJson(jsonResponse);

        List<VolumeInstance> volumeInstances = listInstances(response);
        return volumeInstances;
    }

    private List<VolumeInstance> listInstances(GetAllVolumesResponse response) {
        List<VolumeInstance> volumeInstances = new LinkedList<VolumeInstance>();
        
        for (Volume volume : response.getVolumes()) {
            if (volume != null) {
                volumeInstances.add(loadInstance(volume));
            }
        }
        return volumeInstances;
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
    
}
