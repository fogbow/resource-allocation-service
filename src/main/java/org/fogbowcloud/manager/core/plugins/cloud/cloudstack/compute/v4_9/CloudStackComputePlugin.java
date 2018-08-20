package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
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
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class CloudStackComputePlugin implements ComputePlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackComputePlugin.class);

    private static final String CLOUDSTACK_URL_KEY = "cloudstack_api_url";
    protected static final String LIST_VOLUMES_COMMAND = "listVolumes";

    private Properties properties;
    private HttpRequestClientUtil client;

    public CloudStackComputePlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        // TODO read attributes from file
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

        GetComputeRequest request = new GetComputeRequest.Builder()
                .id(computeInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        LOGGER.debug("Getting instance from json: " + jsonResponse);

        GetComputeResponse computeResponse = GetComputeResponse.fromJson(jsonResponse);
        List<GetComputeResponse.VirtualMachine> vms = computeResponse.getVirtualMachines();

        if (vms.size() > 0) {
            return getComputeInstance(vms.get(0), cloudStackToken);
        } else {
            throw new InstanceNotFoundException();
        }
    }

    private ComputeInstance getComputeInstance(GetComputeResponse.VirtualMachine vm, CloudStackToken cloudStackToken)
            throws FogbowManagerException {
        String instanceId = vm.getId();
        String hostName = vm.getName();
        int vcpusCount = vm.getCpuNumber();
        int memory = vm.getMemory();
        // TODO(pauloewerton): use volume plugin to request disk size
        int disk = 0;

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

    @Override
    public void deleteInstance(String computeInstanceId, CloudStackToken localToken)
            throws FogbowManagerException, UnexpectedException {

    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }
}
