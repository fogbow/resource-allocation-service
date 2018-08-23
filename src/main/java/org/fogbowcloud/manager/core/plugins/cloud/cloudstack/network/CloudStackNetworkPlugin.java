package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

import java.io.File;
import java.util.*;

import static org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator.CLOUDSTACK_URL;

public class CloudStackNetworkPlugin implements NetworkPlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackNetworkPlugin.class);

    public static final java.lang.String NETWORK_OFFERING_ID = "network_offering_id";
    public static final java.lang.String ZONE_ID = "zone_id";

    protected String networkOfferingId = null;
    protected String zoneId = null;

    private HttpRequestClientUtil client;

    public CloudStackNetworkPlugin() {
        String cloudStackConfFilePath = HomeDir.getInstance().getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.networkOfferingId = properties.getProperty(NETWORK_OFFERING_ID);
        this.zoneId = properties.getProperty(ZONE_ID);

        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudStackToken cloudStackToken) throws FogbowManagerException, UnexpectedException {
        SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(networkOrder.getAddress());
        if (subnetInfo == null) {
            throw new InvalidParameterException("cidr <" + networkOrder.getAddress() + "> is not valid");
        }

        String name = generateRandomNetworkName();
        String startingIp = subnetInfo.getLowAddress();
        String endingIp = subnetInfo.getHighAddress();
        String gateway = networkOrder.getGateway();

        CreateNetworkRequest request = new CreateNetworkRequest.Builder()
                .name(name)
                .displayText(name)
                .networkOfferingId(this.networkOfferingId)
                .zoneId(this.zoneId)
                .startIp(startingIp)
                .endingIp(endingIp)
                .gateway(gateway)
                .netmask(subnetInfo.getNetmask())
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        CreateNetworkResponse response = CreateNetworkResponse.fromJson(jsonResponse);
        return response.getId();
    }

    @Override
    public NetworkInstance getInstance(String networkInstanceId, CloudStackToken cloudStackToken) throws FogbowManagerException, UnexpectedException {
        GetNetworkRequest request = new GetNetworkRequest.Builder()
                .id(networkInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetNetworkResponse response = GetNetworkResponse.fromJson(jsonResponse);
        List<GetNetworkResponse.Network> networks = response.getNetworks();

        if (networks.size() > 0) {
            // since an id were specified, there should be no more than one network in the response
            return getNetworkInstance(networks.get(0));
        } else {
            throw new InstanceNotFoundException();
        }
    }

    @Override
    public void deleteInstance(String networkInstanceId, CloudStackToken cloudStackToken) throws FogbowManagerException, UnexpectedException {
        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder()
                .id(networkInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        try {
            this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }

    private String generateRandomNetworkName() {
        return "fogbow_network_" + UUID.randomUUID();
    }

    private SubnetUtils.SubnetInfo getSubnetInfo(String cidrNotation) {
        try {
            return new SubnetUtils(cidrNotation).getInfo();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private NetworkInstance getNetworkInstance(GetNetworkResponse.Network network) {
        String state = network.getState();
        InstanceState fogbowState = CloudStackStateMapper.map(ResourceType.NETWORK, state);

        String networkId = network.getId();
        String label = network.getName();
        String address = network.getCidr();
        String gateway = network.getGateway();
        NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;

        return new NetworkInstance(networkId, fogbowState, label, address, gateway, null, allocationMode,
                null, null, null);
    }

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }

}