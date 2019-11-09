package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class CloudStackVolumePlugin implements VolumePlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);

    protected static final int CUSTOMIZED_DISK_SIZE_EXPECTED = 0;

    private CloudStackHttpClient client;
    private String zoneId;
    private String cloudStackUrl;

    public CloudStackVolumePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.zoneId = properties.getProperty(CloudStackCloudUtils.ZONE_ID_CONFIG);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(@NotNull VolumeOrder volumeOrder, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);
        CreateVolumeRequest request = buildCreateVolumeRequest(volumeOrder, cloudStackUser);
        return doRequestInstance(request, cloudStackUser);
    }

    @Override
    public VolumeInstance getInstance(@NotNull VolumeOrder volumeOrder, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, volumeOrder.getInstanceId()));
        GetVolumeRequest request = new GetVolumeRequest.Builder()
                .id(volumeOrder.getInstanceId())
                .build(this.cloudStackUrl);

        return doGetInstance(request, cloudStackUser);
    }

    @Override
    public void deleteInstance(@NotNull VolumeOrder volumeOrder, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, volumeOrder.getInstanceId()));
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder()
                .id(volumeOrder.getInstanceId())
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    void doDeleteInstance(@NotNull DeleteVolumeRequest request, @NotNull CloudStackUser cloudStackUser)
        throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            DeleteVolumeResponse volumeResponse = DeleteVolumeResponse.fromJson(jsonResponse);
            boolean success = volumeResponse.isSuccess();
            if (!success) {
                String message = volumeResponse.getDisplayText();
                throw new UnexpectedException(message);
            }
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    VolumeInstance doGetInstance(@NotNull GetVolumeRequest request, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            GetVolumeResponse response = GetVolumeResponse.fromJson(jsonResponse);
            List<GetVolumeResponse.Volume> volumes = response.getVolumes();
            if (volumes.isEmpty()) {
                throw new UnexpectedException();
            }
            // since an id were specified, there should be no more than one volume in the response
            GetVolumeResponse.Volume volume = volumes.listIterator().next();
            return buildVolumeInstance(volume);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    CreateVolumeRequest buildCreateVolumeRequest(@NotNull VolumeOrder volumeOrder,
                                                 @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = getDisksOffering(cloudStackUser);
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOfferingFiltered =
                filterDisksOfferingByRequirements(disksOffering, volumeOrder);

        String diskOfferingCompatibleId = getDiskOfferingIdCompatible(volumeOrder, disksOfferingFiltered);
        if (diskOfferingCompatibleId != null) {
            return buildVolumeCompatible(volumeOrder, diskOfferingCompatibleId);
        } else {
            LOGGER.warn(Messages.Warn.DISK_OFFERING_COMPATIBLE_NOT_FOUND);
        }

        String diskOfferingCustomizedId = getDiskOfferingIdCustomized(disksOfferingFiltered);
        if (diskOfferingCustomizedId != null) {
            return buildVolumeCustomized(volumeOrder, diskOfferingCustomizedId);
        } else {
            LOGGER.warn(Messages.Warn.DISK_OFFERING_CUSTOMIZED_NOT_FOUND);
        }

        throw new NoAvailableResourcesException();
    }

    @NotNull
    @VisibleForTesting
    String doRequestInstance(@NotNull CreateVolumeRequest request, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            CreateVolumeResponse volumeResponse = CreateVolumeResponse.fromJson(jsonResponse);
            return volumeResponse.getId();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    List<GetAllDiskOfferingsResponse.DiskOffering> getDisksOffering(
            @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder()
                .build(this.cloudStackUrl);

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            GetAllDiskOfferingsResponse response = GetAllDiskOfferingsResponse.fromJson(jsonResponse);
            return response.getDiskOfferings();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    List<GetAllDiskOfferingsResponse.DiskOffering> filterDisksOfferingByRequirements(
            @NotNull List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering,
            @NotNull VolumeOrder volumeOrder) {

        List<GetAllDiskOfferingsResponse.DiskOffering> disksOfferingFilted = disksOffering;
        Map<String, String> requirements = volumeOrder.getRequirements();
        if (requirements == null || requirements.size() == 0) {
            return disksOffering;
        }

        for (Map.Entry<String, String> tag : requirements.entrySet()) {
            String tagFromRequirements = tag.getKey() +
                                         CloudStackCloudUtils.FOGBOW_TAG_SEPARATOR +
                                         tag.getValue();
            disksOfferingFilted = disksOfferingFilted.stream().filter(diskOffering -> {
                String tagsDiskOffering = diskOffering.getTags();
                boolean isMatchingWithRequirements = tagsDiskOffering != null &&
                        !tagsDiskOffering.isEmpty() &&
                        tagsDiskOffering.contains(tagFromRequirements);
                return isMatchingWithRequirements;
            }).collect(Collectors.toList());
        }

        return disksOfferingFilted;
    }

    @Nullable
    @VisibleForTesting
    String getDiskOfferingIdCustomized(
            @NotNull List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings) {

        for (GetAllDiskOfferingsResponse.DiskOffering diskOffering : diskOfferings) {
            boolean customized = diskOffering.isCustomized();
            int diskSize = diskOffering.getDiskSize();
            if (customized && diskSize == CUSTOMIZED_DISK_SIZE_EXPECTED) {
                return diskOffering.getId();
            }
        }
        return null;
    }

    @Nullable
    @VisibleForTesting
    String getDiskOfferingIdCompatible(@NotNull VolumeOrder volumeOrder,
                                       @NotNull List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings) {

        int orderVolumeSize = volumeOrder.getVolumeSize();
        for (GetAllDiskOfferingsResponse.DiskOffering diskOffering : diskOfferings) {
            int diskSize = diskOffering.getDiskSize();
            if (diskSize == orderVolumeSize) {
                return diskOffering.getId();
            }
        }
        return null;
    }

    @NotNull
    @VisibleForTesting
    CreateVolumeRequest buildVolumeCustomized(@NotNull VolumeOrder volumeOrder, String diskOfferingId)
            throws InvalidParameterException {

        String name = normalizeInstanceName(volumeOrder.getName());
        String size = String.valueOf(volumeOrder.getVolumeSize());
        return new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(name)
                .diskOfferingId(diskOfferingId)
                .size(size)
                .build(this.cloudStackUrl);
    }

    @NotNull
    @VisibleForTesting
    CreateVolumeRequest buildVolumeCompatible(@NotNull VolumeOrder volumeOrder, String diskOfferingId)
            throws InvalidParameterException {

        String name = normalizeInstanceName(volumeOrder.getName());
        return new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(name)
                .diskOfferingId(diskOfferingId)
                .build(this.cloudStackUrl);
    }

    @NotNull
    @VisibleForTesting
    VolumeInstance buildVolumeInstance(GetVolumeResponse.Volume volume) {
        String id = volume.getId();
        String state = volume.getState();
        String name = volume.getName();
        long sizeInBytes = volume.getSize();
        int sizeInGigabytes = convertToGigaByte(sizeInBytes);

        return new VolumeInstance(id, state, name, sizeInGigabytes);
    }

    // TODO(chico) - Move to the Utils class and implement tests
    @VisibleForTesting
    int convertToGigaByte(long sizeInBytes) {
        return (int) (sizeInBytes / CloudStackCloudUtils.ONE_GB_IN_BYTES);
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

    @VisibleForTesting
    String getZoneId() {
        return this.zoneId;
    }

    @VisibleForTesting
    String normalizeInstanceName(String instanceName) {
        if (instanceName == null) {
            return CloudStackCloudUtils.generateInstanceName();
        }
        return instanceName;
    }
}
