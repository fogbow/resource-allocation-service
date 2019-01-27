package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class CloudStackNetworkPlugin implements NetworkPlugin {
    private static final Logger LOGGER = Logger.getLogger(CloudStackNetworkPlugin.class);

    public static final String NETWORK_OFFERING_ID = "network_offering_id";
    public static final String ZONE_ID = "zone_id";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    protected String networkOfferingId = null;
    protected String zoneId = null;
    protected String cloudStackUrl;

    private AuditableHttpRequestClient client;
    private Properties properties;

    public CloudStackNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);

        this.networkOfferingId = properties.getProperty(NETWORK_OFFERING_ID);
        this.zoneId = properties.getProperty(ZONE_ID);

        this.client = new AuditableHttpRequestClient(new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY)));
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudToken cloudToken) throws FogbowException {
        SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(networkOrder.getCidr());
        if (subnetInfo == null) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_CIDR, networkOrder.getCidr()));
        }

        String name = networkOrder.getName();
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
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        CreateNetworkResponse response = CreateNetworkResponse.fromJson(jsonResponse);
        return response.getId();
    }

    @Override
    public NetworkInstance getInstance(String networkInstanceId, CloudToken cloudToken) throws FogbowException {
        GetNetworkRequest request = new GetNetworkRequest.Builder()
                .id(networkInstanceId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        GetNetworkResponse response = GetNetworkResponse.fromJson(jsonResponse);
        List<GetNetworkResponse.Network> networks = response.getNetworks();

        if (networks != null && networks.size() > 0) {
            // since an id was specified, there should be no more than one network in the response
            return getNetworkInstance(networks.get(0));
        } else {
            throw new InstanceNotFoundException();
        }
    }

    @Override
    public void deleteInstance(String networkInstanceId, CloudToken cloudToken) throws FogbowException {
        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder()
                .id(networkInstanceId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudToken.getTokenValue());

        try {
            this.client.doGetRequest(request.getUriBuilder().toString(), cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
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

    protected void setClient(AuditableHttpRequestClient client) {
        this.client = client;
    }

}
