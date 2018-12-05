
package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.VolumePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse.DiskOffering;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.io.File;
import java.util.*;

public class CloudStackVolumePlugin implements VolumePlugin<CloudStackToken> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);

    private static final String CLOUDSTACK_ZONE_ID_KEY = "zone_id";
    private static final int FIRST_ELEMENT_POSITION = 0;
    private static final String FOGBOW_INSTANCE_NAME = "ras-volume-";
    private static final String FOGBOW_TAG_SEPARATOR = ":";
    private HttpRequestClientUtil client;
    private boolean diskOfferingCompatible;
    private String zoneId;

    public CloudStackVolumePlugin() {
        String filePath = HomeDir.getPath() + File.separator + SystemConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        this.zoneId = properties.getProperty(CLOUDSTACK_ZONE_ID_KEY);
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudStackToken localUserAttributes)
            throws FogbowRasException {
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
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }
        CreateVolumeResponse volumeResponse = CreateVolumeResponse.fromJson(jsonResponse);
        String volumeId = volumeResponse.getId();
        return volumeId;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        GetVolumeRequest request = new GetVolumeRequest.Builder()
                .id(volumeInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        GetVolumeResponse response = GetVolumeResponse.fromJson(jsonResponse);
        List<GetVolumeResponse.Volume> volumes = response.getVolumes();

        if (volumes != null && volumes.size() > 0) {
            // since an id were specified, there should be no more than one volume in the response
            return loadInstance(volumes.get(FIRST_ELEMENT_POSITION));
        } else {
            throw new UnexpectedException();
        }
    }

    @Override
    public void deleteInstance(String volumeInstanceId, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {

        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder()
                .id(volumeInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        DeleteVolumeResponse volumeResponse = DeleteVolumeResponse.fromJson(jsonResponse);
        boolean success = volumeResponse.isSuccess();

        if (!success) {
            String message = volumeResponse.getDisplayText();
            throw new UnexpectedException(message);
        }
    }

    private String getDiskOfferingId(VolumeOrder volumeOrder, Token localUserAttributes)
            throws FogbowRasException {

        int volumeSize = volumeOrder.getVolumeSize();
        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder().build();
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        GetAllDiskOfferingsResponse response = GetAllDiskOfferingsResponse.fromJson(jsonResponse);
        List<DiskOffering> diskOfferings = response.getDiskOfferings();
        List<DiskOffering> toRemove = new ArrayList<>();

        if (volumeOrder.getRequirements() != null && volumeOrder.getRequirements().size() > 0) {
            for (Map.Entry<String, String> tag : volumeOrder.getRequirements().entrySet()) {
                String concatenatedTag = tag.getKey() + FOGBOW_TAG_SEPARATOR + tag.getValue();

                for (DiskOffering diskOffering : diskOfferings) {
                    if (diskOffering.getTags() == null) {
                        toRemove.add(diskOffering);
                        continue;
                    }

                    List<String> tags = new ArrayList<>(Arrays.asList(diskOffering.getTags().split(",")));
                    if (!tags.contains(concatenatedTag)) {
                        toRemove.add(diskOffering);
                    }
                }
            }
        }

        diskOfferings.removeAll(toRemove);

        String diskOfferingId = getDiskOfferingIdCompatible(volumeOrder.getVolumeSize(), diskOfferings);

        if (!isDiskOfferingCompatible()) {
            diskOfferingId = getDiskOfferingIdCustomized(diskOfferings);
        }

        return diskOfferingId;
    }

    private String getDiskOfferingIdCustomized(List<DiskOffering> diskOfferings) {
        boolean customized;
        int size;

        for (DiskOffering diskOffering : diskOfferings) {
            customized = diskOffering.isCustomized();
            size = diskOffering.getDiskSize();

            if (customized && size == 0) {
                return diskOffering.getId();
            }
        }

        return null;
    }

    private String getDiskOfferingIdCompatible(int volumeSize, List<DiskOffering> diskOfferings) {
        int size;

        for (DiskOffering diskOffering : diskOfferings) {
            size = diskOffering.getDiskSize();

            if (size == volumeSize) {
                this.diskOfferingCompatible = true;
                return diskOffering.getId();
            }
        }

        this.diskOfferingCompatible = false;

        return null;
    }

    private CreateVolumeRequest createVolumeCustomized(VolumeOrder volumeOrder, String diskOfferingId)
            throws InvalidParameterException {

        String instanceName = volumeOrder.getName();
        String name = instanceName == null ? FOGBOW_INSTANCE_NAME + getRandomUUID() : instanceName;
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

        String instanceName = volumeOrder.getName();
        String name = instanceName == null ? FOGBOW_INSTANCE_NAME + getRandomUUID() : instanceName;
        return new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(name)
                .diskOfferingId(diskOfferingId)
                .build();
    }

    private VolumeInstance loadInstance(GetVolumeResponse.Volume volume) {
        String id = volume.getId();
        String state = volume.getState();
        String name = volume.getName();
        long sizeInBytes = volume.getSize();
        int sizeInGigabytes = (int) (sizeInBytes / Math.pow(1024, 3));

        InstanceState instanceState = CloudStackStateMapper.map(ResourceType.VOLUME, state);

        VolumeInstance volumeInstance = new VolumeInstance(id, instanceState, name, sizeInGigabytes);
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

    protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }
}
