package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

import java.io.File;
import java.util.*;

public class CloudStackComputePlugin implements ComputePlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackComputePlugin.class);

    private static final String ZONE_ID_KEY = "zone_id";
    private static final String EXPUNGE_ON_DESTROY_KEY = "compute_cloudstack_expunge_on_destroy";

    private HttpRequestClientUtil client;

    private String zoneId;
    private String expungeOnDestroy;

    public CloudStackComputePlugin() throws FatalErrorException {
        String cloudStackConfFilePath = HomeDir.getInstance().getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.zoneId = properties.getProperty(ZONE_ID_KEY);
        this.expungeOnDestroy = properties.getProperty(EXPUNGE_ON_DESTROY_KEY, "true");

        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudStackToken cloudStackToken)
            throws FogbowManagerException, UnexpectedException {
        String templateId = computeOrder.getImageId();
        if (templateId == null || this.zoneId == null) {
            throw new InvalidParameterException();
        }

        // FIXME(pauloewerton): should this be creating a cloud-init script for cloudstack?
        String userData = Base64.getEncoder().encodeToString(computeOrder.getUserData().getExtraUserDataFileContent().getBytes());
        String networksId = StringUtils.join(computeOrder.getNetworksId(), ",");

        String serviceOfferingId = getServiceOfferingId(computeOrder.getvCPU(), computeOrder.getMemory(), cloudStackToken);
        if (serviceOfferingId == null) {
            throw new NoAvailableResourcesException();
        }

        int disk = computeOrder.getDisk();
        String diskOfferingId = disk > 0 ? getDiskOfferingId(disk, cloudStackToken) : null;

        // NOTE(pauloewerton): diskofferingid and hypervisor are required in case of ISO image. I haven't
        // found any clue pointing that ISO images were being used in mono though.
        DeployVirtualMachineRequest request = new DeployVirtualMachineRequest.Builder()
                .serviceOfferingId(serviceOfferingId)
                .templateId(templateId)
                .zoneId(this.zoneId)
                .diskOfferingId(diskOfferingId)
                .userData(userData)
                .networksId(networksId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        DeployVirtualMachineResponse response = DeployVirtualMachineResponse.fromJson(jsonResponse);

        return response.getId();
    }

    public String getServiceOfferingId(int vcpusRequirement, int memoryRequirement, CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        GetAllServiceOfferingsResponse serviceOfferingsResponse = getServiceOfferings(cloudStackToken);
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings = serviceOfferingsResponse.getServiceOfferings();

        if (serviceOfferings != null) {
            for (GetAllServiceOfferingsResponse.ServiceOffering serviceOffering : serviceOfferings) {
                if (serviceOffering.getCpuNumber() >= vcpusRequirement &&
                        serviceOffering.getMemory() >= memoryRequirement) {
                    return serviceOffering.getId();
                }
            }
        } else {
            throw new NoAvailableResourcesException();
        }

        return null;
    }

    public GetAllServiceOfferingsResponse getServiceOfferings(CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        GetAllServiceOfferingsRequest request = new GetAllServiceOfferingsRequest.Builder().build();
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetAllServiceOfferingsResponse serviceOfferingsResponse = GetAllServiceOfferingsResponse.fromJson(jsonResponse);

        return serviceOfferingsResponse;
    }

    public String getDiskOfferingId(int diskSize, CloudStackToken cloudStackToken) throws FogbowManagerException {
        GetAllDiskOfferingsResponse diskOfferingsResponse = getDiskOfferings(cloudStackToken);
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = diskOfferingsResponse.getDiskOfferings();

        if (diskOfferings != null) {
            for (GetAllDiskOfferingsResponse.DiskOffering diskOffering : diskOfferings) {
                if (diskOffering.getDiskSize() >= diskSize) {
                    return diskOffering.getId();
                }
            }
        }

        return null;
    }

    public GetAllDiskOfferingsResponse getDiskOfferings(CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder().build();
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetAllDiskOfferingsResponse diskOfferingsResponse = GetAllDiskOfferingsResponse.fromJson(jsonResponse);

        return diskOfferingsResponse;
    }

    @Override
    public ComputeInstance getInstance(String computeInstanceId, CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .id(computeInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetVirtualMachineResponse computeResponse = GetVirtualMachineResponse.fromJson(jsonResponse);
        List<GetVirtualMachineResponse.VirtualMachine> vms = computeResponse.getVirtualMachines();
        if (vms != null) {
            return getComputeInstance(vms.get(0));
        } else {
            throw new InstanceNotFoundException();
        }
    }

    private ComputeInstance getComputeInstance(GetVirtualMachineResponse.VirtualMachine vm) throws FogbowManagerException {
        String instanceId = vm.getId();
        String hostName = vm.getName();
        int vcpusCount = vm.getCpuNumber();
        int memory = vm.getMemory();
        // TODO(pauloewerton): use volume plugin to request disk size
        int disk = 0;

        String cloudStackState = vm.getState();
        InstanceState fogbowState = CloudStackStateMapper.map(ResourceType.COMPUTE, cloudStackState);

        GetVirtualMachineResponse.Nic[] addresses = vm.getNic();
        String address = "";
        if (addresses != null) {
            boolean firstAddressEmpty = addresses == null || addresses.length == 0 || addresses[0].getIpAddress() == null;
            address = firstAddressEmpty ? "" : addresses[0].getIpAddress();
        }

        ComputeInstance computeInstance = new ComputeInstance(instanceId,
                fogbowState, hostName, vcpusCount, memory, disk, address);

        return computeInstance;
    }

    @Override
    public void deleteInstance(String computeInstanceId, CloudStackToken cloudStackToken)
            throws FogbowManagerException, UnexpectedException {
        DestroyVirtualMachineRequest request = new DestroyVirtualMachineRequest.Builder()
                .id(computeInstanceId)
                .expunge(this.expungeOnDestroy)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        try {
            this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }
}
