package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.network.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.parameters.Network;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.*;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.enums.RoutingMode;

import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models.CreateFirewallRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.securityrule.v1.GoogleCloudSecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.util.List;
import java.util.Properties;

public class GoogleCloudNetworkPlugin implements NetworkPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudNetworkPlugin.class);

    @VisibleForTesting
    static final boolean DEFAULT_AUTO_CREATE_SUBNETWORKS = false;
    @VisibleForTesting
    static final RoutingMode DEFAULT_ROUTING_MODE = RoutingMode.GLOBAL;
    @VisibleForTesting
    static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    @VisibleForTesting
    static final String DEFAULT_NETWORK_CIDR = "10.158.0.0/20";
    @VisibleForTesting
    static final String DEFAULT_SUBNETWORK_REGION = "southamerica-east1";
    @VisibleForTesting
    static final String DEFAULT_INSTANCE_STATE = "active";

    private static final String SUBNET_PREFIX = "-subnet";

    private String networkV1ApiEndpoint;
    private GoogleCloudHttpClient client;
    private String[] dnsList;

    public GoogleCloudNetworkPlugin(String confFilePath) throws FatalErrorException{
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV1ApiEndpoint = properties.getProperty(GoogleCloudPluginUtils.NETWORK_URL_KEY) +
        GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT + GoogleCloudConstants.PROJECT_ENDPOINT;
        setDNSList(properties);
        initClient();
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        InsertNetworkResponse insertNetworkResponse = insertNetwork(networkOrder.getName(), cloudUser, projectId);

        String insertedNetworkUrl = insertNetworkResponse.getTargetLink();
        String insertedNetworkId = insertNetworkResponse.getId();

        insertSubnetwork(cloudUser, networkOrder, insertedNetworkUrl, projectId);
        //Todo: Make a POST for firewall ssh default rule
        return insertedNetworkId;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = networkOrder.getInstanceId();

        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        
        String endPoint = this.networkV1ApiEndpoint
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + GoogleCloudPluginUtils.getProjectIdFrom(cloudUser)
                + GoogleCloudConstants.GLOBAL_NETWORKS_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + networkOrder.getName();
        
        String responseStr = doGetInstance(endPoint, cloudUser);

        return buildNetworkInstance(responseStr, cloudUser);
    }

    @VisibleForTesting
    public NetworkInstance buildNetworkInstance(String json, GoogleCloudUser cloudUser) throws FogbowException{
        GetNetworkResponse getNetworkResponse = GetNetworkResponse.fromJson(json);

        String networkId = getNetworkResponse.getId();
        String name = getNetworkResponse.getName();
        String selfLink = getNetworkResponse.getSelfLink();
        List<String> subnetworks = getNetworkResponse.getSubnetworks();

        String subnetLink = subnetworks == null || subnetworks.size() == 0 ? null : subnetworks.get(0);

        String cidr = null;
        String gateway = null;
        NetworkAllocationMode allocationMode = null;

        try {
            GetSubnetworkResponse subnetworkInfo = getSubnetworkInfo(cloudUser, subnetLink);

            cidr = subnetworkInfo.getIpCidrRange();
            gateway = subnetworkInfo.getGatewayAddress();

            //TODO: this step is uncertain, as I don't have dhcp. But I will initially assume that it is STATIC
            allocationMode = NetworkAllocationMode.STATIC;
        }catch (JSONException je){
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_GET_NETWORK_S, json), je);
            throw new InternalServerErrorException(String.format(Messages.Exception.UNABLE_TO_GET_NETWORK_S, json));
        }

        NetworkInstance instance = null;
        //TODO: Here i don't have any information about instance state,
        // allocation mode (as previously mentioned) or vlan

        if(networkId != null){
            instance = new NetworkInstance(networkId, DEFAULT_INSTANCE_STATE, name, cidr, gateway,
                    null, allocationMode, null, null, null);
        }
        return instance;
    }
    @VisibleForTesting
    public GetSubnetworkResponse getSubnetworkInfo(GoogleCloudUser cloudUser, String subnetLink) throws FogbowException{
        String response = this.client.doGetRequest(subnetLink, cloudUser);
        GetSubnetworkResponse getSubnetworkResponse = GetSubnetworkResponse.fromJson(response);
        return  getSubnetworkResponse;
    }

    @VisibleForTesting
    public String doGetInstance(String endPoint, GoogleCloudUser cloudUser) throws FogbowException{
        String responseStr = doGetRequest(cloudUser, endPoint);
        return responseStr;
    }
    @VisibleForTesting
    public String doGetRequest(GoogleCloudUser cloudUser, String endPoint) throws FogbowException{
        String responseStr = this.client.doGetRequest(endPoint, cloudUser);
        return responseStr;
    }

    @Override
    public boolean isReady(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceName = networkOrder.getName();
        String instanceId = networkOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceName));
        doDeleteInstance(instanceName, cloudUser, instanceId);
    }
    @VisibleForTesting
    public void doDeleteInstance(String instanceName, GoogleCloudUser cloudUser, String instanceId) throws InternalServerErrorException, FogbowException{
        try{
            String endPoint = this.networkV1ApiEndpoint
                    + GoogleCloudConstants.ENDPOINT_SEPARATOR
                    + GoogleCloudPluginUtils.getProjectIdFrom(cloudUser)
                    + GoogleCloudConstants.GLOBAL_NETWORKS_ENDPOINT
                    + GoogleCloudConstants.ENDPOINT_SEPARATOR
                    + instanceName;
            this.client.doDeleteRequest(endPoint, cloudUser);
        }catch (InstanceNotFoundException infe){
            LOGGER.warn(String.format(Messages.Log.NETWORK_NOT_FOUND_S, instanceId), infe);
        }catch (FogbowException fe){
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_DELETE_NETWORK_WITH_ID_S, instanceId), fe);
            throw fe;
        }
        //TODO: If needed, delete the firewall rules related to this network
    }

    public static CreateFirewallRuleRequest createDefaultIngressSecurityRule(Order majorOrder, String confFilePath){
        SecurityRule.Protocol protocol = SecurityRule.Protocol.ANY;
        SecurityRule.EtherType etherType = SecurityRule.EtherType.IPv4;
        SecurityRule.Direction direction = SecurityRule.Direction.IN;

        int portFrom = GoogleCloudConstants.Network.Firewall.LOWEST_PORT_LIMIT;
        int portTo = GoogleCloudConstants.Network.Firewall.HIGHEST_PORT_LIMIT;
        String cidr = GoogleCloudConstants.Network.Firewall.INTERNAL_VPC_CIDR;

        SecurityRule securityRule = new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);

        GoogleCloudSecurityRulePlugin googleCloudSecurityRulePluginTemp = new GoogleCloudSecurityRulePlugin(confFilePath);

        return googleCloudSecurityRulePluginTemp.buildCreateSecurityRuleRequest(securityRule, majorOrder, GoogleCloudConstants
        .Network.Firewall.LOWEST_PORT_LIMIT);
    }
    @VisibleForTesting
    public InsertNetworkResponse insertNetwork(String networkName, GoogleCloudUser cloudUser, String projectId) throws FogbowException{
        InsertNetworkResponse insertNetworkResponse = null;
        InsertNetworkRequest insertNetworkRequest = new InsertNetworkRequest.Builder()
                .name(networkName)
                .autoCreateSubnetworks(DEFAULT_AUTO_CREATE_SUBNETWORKS)
                .routingMode(DEFAULT_ROUTING_MODE)
                .build();

        String endPoint = this.networkV1ApiEndpoint + GoogleCloudConstants.ENDPOINT_SEPARATOR +
                projectId + GoogleCloudConstants.GLOBAL_NETWORKS_ENDPOINT;


        try {
            String response = this.client.doPostRequest(endPoint, insertNetworkRequest.toJson(), cloudUser);
            insertNetworkResponse = InsertNetworkResponse.fromJson(response);
        }catch (JsonSyntaxException e){
            LOGGER.error(Messages.Log.UNABLE_TO_GENERATE_JSON, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_GENERATE_JSON);
        }
        return  insertNetworkResponse;

    }
    @VisibleForTesting
    public void insertSubnetwork(GoogleCloudUser cloudUser, NetworkOrder networkOrder, String insertedNetworkUrl, String projectId) throws FogbowException{

        try {
            String jsonRequest = generateJsonEntityToCreateSubnetwork(insertedNetworkUrl, projectId, networkOrder);
            String endPoint = this.networkV1ApiEndpoint + GoogleCloudConstants.ENDPOINT_SEPARATOR
                    + projectId + cloud.fogbow.common.constants.GoogleCloudConstants.REGIONS_ENDPOINT
                    + GoogleCloudConstants.ENDPOINT_SEPARATOR + DEFAULT_SUBNETWORK_REGION + GoogleCloudConstants.SUBNETS_ENDPOINT;
            this.client.doPostRequest(endPoint, jsonRequest, cloudUser);
        }catch (FogbowException fe){
            removeNetwork(insertedNetworkUrl, cloudUser);
            throw fe;
        }
    }
    @VisibleForTesting
    public void removeNetwork(String networkUrl, GoogleCloudUser cloudUser) throws FogbowException{
        this.client.doDeleteRequest(networkUrl, cloudUser);
    }

    private String generateJsonEntityToCreateSubnetwork(String insertedNetworkUrl, String projectId, NetworkOrder networkOrder) {
        String subnetName = networkOrder.getName() + SUBNET_PREFIX;

        String subNetworkCidr = networkOrder.getCidr();
        subNetworkCidr = subNetworkCidr == null ? DEFAULT_NETWORK_CIDR : subNetworkCidr;

        InsertSubnetworkRequest insertSubnetworkRequest = new InsertSubnetworkRequest.Builder()
                .name(subnetName)
                .network(insertedNetworkUrl)
                .ipCidrRange(subNetworkCidr)
                .build();
        return insertSubnetworkRequest.toJson();
    }

    @VisibleForTesting
    public void setDNSList(Properties properties) {
        String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
        if (dnsProperty != null) {
            this.dnsList = dnsProperty.split(",");
        }
    }

    private void initClient(){
        this.client = new GoogleCloudHttpClient();
    }

    @VisibleForTesting
    public void setClient(GoogleCloudHttpClient client){
        this.client = client;
    }

    @VisibleForTesting
    public String[] getDnsList(){
        return this.dnsList;
    }
}
