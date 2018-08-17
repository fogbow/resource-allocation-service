package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.GetVolumeResponse;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;

import java.io.File;
import java.util.Properties;

public class CloudStackComputePlugin implements ComputePlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackComputePlugin.class);

    private static final String CLOUDSTACK_URL_KEY = "cloudstack_api_url";
    protected static final String LIST_VMS_COMMAND = "listVirtualMachines";
    protected static final String LIST_VOLUMES_COMMAND = "listVolumes";
    protected static final String VIRTUAL_MACHINE_ID = "id";

    private Properties properties;
    private HttpRequestClientUtil client;
    private String endpoint;

    public CloudStackComputePlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        this.endpoint = properties.getProperty(CLOUDSTACK_URL_KEY);
        initClient();
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudStackToken localToken)
            throws FogbowManagerException, UnexpectedException {
        return "";

    }

    @Override
    public ComputeInstance getInstance(String computeInstanceId, CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        LOGGER.info("Getting instance " + computeInstanceId + " with token " + cloudStackToken);

        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(this.endpoint, LIST_VMS_COMMAND);
        uriBuilder.addParameter(VIRTUAL_MACHINE_ID, computeInstanceId);
        CloudStackUrlUtil.sign(uriBuilder, cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(uriBuilder.toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        LOGGER.debug("Getting instance from json: " + jsonResponse);
        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse, cloudStackToken);

        return computeInstance;

    }

    private ComputeInstance getInstanceFromJson(String jsonResponse, CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        GetComputeResponse computeResponse = GetComputeResponse.fromJson(jsonResponse);
        GetComputeResponse.VirtualMachine vm = computeResponse.getVirtualMachines().get(0);

        String instanceId = vm.getId();
        String hostName = vm.getName();
        int vcpusCount = vm.getCpuNumber();
        int memory = vm.getMemory();

        String volumeJsonResponse = getInstanceVolume(instanceId, cloudStackToken);
        int disk = getDiskSizeFromJson(volumeJsonResponse);

        String cloudStackState = vm.getState();
        InstanceState fogbowState = CloudStackStateMapper.map(ResourceType.COMPUTE, cloudStackState);

        GetComputeResponse.Nic[] addresses = vm.getNic();
        String address = "";
        if (addresses != null) {
            boolean firstAddressEmpty = addresses == null || addresses.length == 0 || addresses[0].getIpAddress() == null;
            address = firstAddressEmpty ? "" : addresses[0].getIpAddress();
        }

        ComputeInstance computeInstance = new ComputeInstance(instanceId,
                fogbowState, hostName, vcpusCount, memory, disk, address);

        return computeInstance;
    }

    private String getInstanceVolume(String computeInstanceId, CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        LOGGER.info("Getting volume for instance " + computeInstanceId + " with token " + cloudStackToken);

        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(this.endpoint, LIST_VOLUMES_COMMAND);
        uriBuilder.addParameter(VIRTUAL_MACHINE_ID, computeInstanceId);
        CloudStackUrlUtil.sign(uriBuilder, cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(uriBuilder.toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        return jsonResponse;
    }

    private int getDiskSizeFromJson(String volumeJsonResponse) {
        GetVolumeResponse volumeResponse = GetVolumeResponse.fromJson(volumeJsonResponse);
        GetVolumeResponse.Volume volume = volumeResponse.getVolumes().get(0);

        // TODO(pauloewerton): Check case when there's no volume attached to instance
        int diskSize = volume.getDiskSize();

        return diskSize;
    }

    @Override
    public void deleteInstance(String computeInstanceId, CloudStackToken localToken)
            throws FogbowManagerException, UnexpectedException {

    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }
}
