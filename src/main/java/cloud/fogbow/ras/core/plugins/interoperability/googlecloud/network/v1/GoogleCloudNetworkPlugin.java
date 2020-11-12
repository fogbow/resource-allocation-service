package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.network.v1;

import cloud.fogbow.as.core.models.GoogleCloudSystemUser;
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
import cloud.fogbow.ras.constants.SystemConstants;
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
import java.util.UUID;

public class GoogleCloudNetworkPlugin implements NetworkPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudNetworkPlugin.class);

    @VisibleForTesting
    static final boolean DEFAULT_AUTO_CREATE_SUBNETWORKS = true;
    @VisibleForTesting
    static final RoutingMode DEFAULT_ROUTING_MODE = RoutingMode.GLOBAL;
    @VisibleForTesting
    static final String DEFAULT_NETWORK_CIDR = "10.158.0.0/20";
    @VisibleForTesting
    static final String DEFAULT_SUBNETWORK_REGION = "southamerica-east1";
    @VisibleForTesting
    static final String SIMULATED_RUNNING_RESOURCE_KEY = "running";
    @VisibleForTesting
    static final String SIMULATED_ERROR_RESOURCE_KEY = "error";

    
    private static final String SUBNET_PREFIX = "-subnet";

    private String networkV1ApiEndpoint;
    private GoogleCloudHttpClient client;
    private String region;
    private String confFilePath;

    public GoogleCloudNetworkPlugin(String confFilePath) throws FatalErrorException{
        this.confFilePath = confFilePath;
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        //getProperties for the region field
        this.networkV1ApiEndpoint = GoogleCloudConstants.BASE_COMPUTE_API_URL +
        GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT + GoogleCloudConstants.PROJECT_ENDPOINT;
        this.region = DEFAULT_SUBNETWORK_REGION;
        initClient();
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        InsertNetworkResponse insertNetworkResponse = insertNetwork(networkOrder.getName(), cloudUser, projectId);
        String insertedNetworkId = insertNetworkResponse.getId();

        //TODO: Implement the default security rule creation
        //TODO: Utilize the same principle to create custom subnetworks

        //Url is used if we make custom mode subnets and if we create default rules
        //String insertedNetworkUrl = insertNetworkResponse.getTargetLink();

        //Insert subnetwork is used if we make custom mode subnets
        //insertSubnetwork(cloudUser, networkOrder, insertedNetworkUrl, projectId);

        //doCreateDefaultIngressSecurityRule(networkOrder, cloudUser, insertedNetworkUrl);

        return insertedNetworkId;
    }

    private void doCreateDefaultIngressSecurityRule(NetworkOrder networkOrder, GoogleCloudUser cloudUser, String insertedNetworkUrl) throws FogbowException {
        CreateFirewallRuleRequest request = createDefaultIngressSecurityRule(networkOrder, this.confFilePath);
        try{
            String endpoint = this.networkV1ApiEndpoint
                    + GoogleCloudConstants.ENDPOINT_SEPARATOR
                    + ((GoogleCloudSystemUser) networkOrder.getSystemUser()).getProjectId()
                    + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT;
            String requestJson = request.toJson();
            this.client.doPostRequest(endpoint, requestJson, cloudUser);
        }catch (FogbowException fe){
            removeNetwork(insertedNetworkUrl, cloudUser);
        }

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

        return buildNetworkInstance(responseStr, cloudUser, networkOrder);
    }

    @VisibleForTesting
    public NetworkInstance buildNetworkInstance(String json, GoogleCloudUser cloudUser, NetworkOrder order) throws FogbowException{
        GetNetworkResponse getNetworkResponse = GetNetworkResponse.fromJson(json);

        String networkId = getNetworkResponse.getId();
        String name = getNetworkResponse.getName();
        List<String> subnetworks = getNetworkResponse.getSubnetworks();

        String subnetLink = subnetworks == null || subnetworks.size() == 0 ? null : subnetworks.get(0);

        String cidr = null;
        String gateway = null;
        NetworkAllocationMode allocationMode = null;

        try {
            GetSubnetworkResponse subnetworkInfo = getSubnetworkInfo(cloudUser, subnetLink);

            cidr = subnetworkInfo.getIpCidrRange();
            gateway = subnetworkInfo.getGatewayAddress();

            allocationMode = order.getAllocationMode() == null ? NetworkAllocationMode.DYNAMIC : order.getAllocationMode();
        }catch (JSONException je){
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_GET_NETWORK_S, json), je);
            throw new InternalServerErrorException(String.format(Messages.Exception.UNABLE_TO_GET_NETWORK_S, json));
        }

        NetworkInstance instance = null;

        if(networkId != null){
            instance = new NetworkInstance(networkId, SIMULATED_RUNNING_RESOURCE_KEY, name, cidr, gateway,
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
    }

    public static CreateFirewallRuleRequest createDefaultIngressSecurityRule(Order majorOrder, String confFilePath){
        SecurityRule.Protocol protocol = SecurityRule.Protocol.ANY;
        SecurityRule.EtherType etherType = SecurityRule.EtherType.IPv4;
        SecurityRule.Direction direction = SecurityRule.Direction.IN;

        int portFrom = GoogleCloudConstants.Network.Firewall.LOWEST_PORT_LIMIT;
        int portTo = GoogleCloudConstants.Network.Firewall.HIGHEST_PORT_LIMIT;
        String cidr = GoogleCloudConstants.Network.Firewall.INTERNAL_VPC_CIDR;
        String securityRuleName = SystemConstants.PN_SECURITY_GROUP_PREFIX
                + ((NetworkOrder) majorOrder).getName()
                + GoogleCloudConstants.ELEMENT_SEPARATOR
                + UUID.randomUUID().toString();

        SecurityRule securityRule = new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);

        GoogleCloudSecurityRulePlugin googleCloudSecurityRulePluginTemp = new GoogleCloudSecurityRulePlugin(confFilePath);

        return googleCloudSecurityRulePluginTemp.buildCreateSecurityRuleRequest(
                securityRule,
                majorOrder,
                securityRuleName);
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
                    + projectId + GoogleCloudConstants.REGIONS_ENDPOINT
                    + GoogleCloudConstants.ENDPOINT_SEPARATOR + this.region + GoogleCloudConstants.SUBNETS_ENDPOINT;
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

    private void initClient(){
        this.client = new GoogleCloudHttpClient();
    }

    @VisibleForTesting
    public void setClient(GoogleCloudHttpClient client){
        this.client = client;
    }

}
