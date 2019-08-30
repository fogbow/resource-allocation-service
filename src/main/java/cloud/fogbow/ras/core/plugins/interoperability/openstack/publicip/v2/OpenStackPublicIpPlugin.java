package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

// FIXME removed this imports static...
import static cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin.ACTION;
import static cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin.COMPUTE_NOVAV2_URL_KEY;
import static cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin.COMPUTE_V2_API_ENDPOINT;
import static cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin.SERVERS;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.AddSecurityGroupToServerRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.RemoveSecurityGroupFromServerRequest;

public class OpenStackPublicIpPlugin implements PublicIpPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackPublicIpPlugin.class);

    protected static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
    protected static final String EXTERNAL_NETWORK_ID_KEY = "external_gateway_info";
    protected static final String FLOATINGIPS = "/floatingips";
    protected static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    protected static final String PORTS = "/ports";
    protected static final String SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String SECURITY_GROUPS = "/security-groups";
    protected static final String V2_API_ENDPOINT = "/v2.0";

    protected static final String IPV4_ETHER_TYPE = "IPv4";
    protected static final String IPV6_ETHER_TYPE = "IPv6";
    protected static final String QUERY_SECURITY_GROUP_NAME = "name";
    protected static final String SECURITY_GROUP_INGRESS_DIRECTION = "ingress";

    private static final int MAXIMUM_PORTS_SIZE = 1;

    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackPublicIpPlugin(String confFilePath) {
        this(confFilePath, true);
    }

    public OpenStackPublicIpPlugin(String confFilePath, boolean checkProperties) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        checkProperties(checkProperties);
        initClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.PUBLIC_IP, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.PUBLIC_IP, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        // Network port id is the connection between the virtual machine and the network
        String networkPortId = getNetworkPortIp(order, cloudUser);
        String floatingNetworkId = getExternalNetworkId();
        
        String jsonRequest = doCreateFloatingIpRequestToJson(floatingNetworkId, networkPortId, projectId);
        String endpoint = getFloatingIpEndpoint();

        String jsonResponse = doRequestInstance(endpoint, jsonRequest, cloudUser);
        CreateFloatingIpResponse floatingIpResponse = doCreateFloatingIpResponseFrom(jsonResponse);
        String floatingIpId = floatingIpResponse.getFloatingIp().getId(); // FIXME verify this after...
        
        doAssociateSecurityGroup(floatingIpId, order, cloudUser);
        
        return floatingIpId;
    }

    private void doAssociateSecurityGroup(String floatingIpId, PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String securityGroupName = defineSecurityGroupName(floatingIpId);
        String computeInstanceId = order.getComputeId();
        String securityGroupId = doCreateSecurityGroup(securityGroupName, cloudUser);
        addAllowAllRules(securityGroupId, cloudUser);
        associateSecurityGroupToCompute(securityGroupName, computeInstanceId, cloudUser);
    }

    private String doCreateSecurityGroup(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        
        CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest.Builder()
                .name(securityGroupName)
                .projectId(projectId)
                .build();
        
        String jsonRequest = createSecurityGroupRequest.toJson();
        
        String endpoint = getSecurityGroupsApiEndpoint();
        String jsonResponse = doRequestInstance(endpoint, jsonRequest, cloudUser);
        
        CreateSecurityGroupResponse securityGroupResponse = doCreateSecurityGroupResponseFrom(jsonResponse);
        return securityGroupResponse.getId();
    }

    private CreateSecurityGroupResponse doCreateSecurityGroupResponseFrom(String json) throws UnexpectedException {
        try {
            return CreateSecurityGroupResponse.fromJson(json);
        } catch (Exception e) {
            String message = Messages.Error.ERROR_WHILE_CREATING_PUBLIC_IP_INSTANCE; // FIXME this message...
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    public String defineSecurityGroupName(String publicIpId) {
        return SystemConstants.PIP_SECURITY_GROUP_PREFIX + publicIpId;
    }

    private CreateFloatingIpResponse doCreateFloatingIpResponseFrom(String json) throws UnexpectedException {
        try {
            return CreateFloatingIpResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = Messages.Error.ERROR_WHILE_CREATING_PUBLIC_IP_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    private String doCreateFloatingIpRequestToJson(String floatingNetworkId, String networkPortId, String projectId) {
        CreateFloatingIpRequest createFloatingIpRequest = new CreateFloatingIpRequest.Builder()
                .floatingNetworkId(floatingNetworkId)
                .portId(networkPortId)
                .projectId(projectId)
                .build();
        
        return createFloatingIpRequest.toJson();
    }

    private String doRequestInstance(String endpoint, String jsonRequest, OpenStackV3User cloudUser)
            throws FogbowException {
        
        String responsePostFloatingIp = null;
        try {
            responsePostFloatingIp = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return responsePostFloatingIp;
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String responseGetFloatingIp = null;
        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
        String floatingIpInstanceId = order.getInstanceId();

        try {
            String floatingIpEndpointPrefix = getFloatingIpEndpoint();
            String endpoint = String.format("%s/%s", floatingIpEndpointPrefix, floatingIpInstanceId);
            responseGetFloatingIp = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        GetFloatingIpResponse getFloatingIpResponse = GetFloatingIpResponse.fromJson(responseGetFloatingIp);

        String floatingIpStatus = getFloatingIpResponse.getFloatingIp().getStatus();
        String ipAddressId = getFloatingIpResponse.getFloatingIp().getId();
        String floatingIpAddress = getFloatingIpResponse.getFloatingIp().getFloatingIpAddress();
        PublicIpInstance publicIpInstance = new PublicIpInstance(ipAddressId, floatingIpStatus, floatingIpAddress);
        return publicIpInstance;
    }

    @Override
    public void deleteInstance(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        try {
            if (order == null) {
                throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
            }
            String computeInstanceId = order.getComputeId();
            String floatingIpId = order.getInstanceId();
            String securityGroupName = defineSecurityGroupName(floatingIpId);

            if (computeInstanceId != null) {
                disassociateSecurityGroupFromCompute(securityGroupName, order, cloudUser);
            }

            String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);
            removeSecurityGroup(securityGroupId, cloudUser);

            String floatingIpEndpointPrefix = getFloatingIpEndpoint();
            String endpoint = String.format("%s/%s", floatingIpEndpointPrefix, floatingIpId);
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getSecurityGroupsApiEndpoint() + "?" + QUERY_SECURITY_GROUP_NAME + "=" + securityGroupName;

        String response = null;
        try {
            response = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        List<ListSecurityGroups.SecurityGroup> securityGroups = ListSecurityGroups.fromJson(response).getSecurityGroups();
        if (securityGroups.size() > 0) {
            return securityGroups.get(0).getId();
        } else {
            return null;
        }
    }

    private void associateSecurityGroupToCompute(String securityGroupName, String computeInstanceId, OpenStackV3User cloudUser) throws FogbowException {
        AddSecurityGroupToServerRequest request = new AddSecurityGroupToServerRequest.Builder()
                .name(securityGroupName)
                .build();

        try {
            String computeEndpoint = getComputeEndpoint(cloudUser.getProjectId());
            computeEndpoint = String.format("%s/%s/%s", computeEndpoint, computeInstanceId, ACTION);
            this.client.doPostRequest(computeEndpoint, request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private void disassociateSecurityGroupFromCompute(String securityGroupName, PublicIpOrder publicIpOrder, OpenStackV3User cloudUser) throws FogbowException {
        String computeInstanceId = publicIpOrder.getComputeId();
        RemoveSecurityGroupFromServerRequest request = new RemoveSecurityGroupFromServerRequest.Builder()
                .name(securityGroupName)
                .build();

        try {
            String computeEndpoint = getComputeEndpoint(cloudUser.getProjectId());
            computeEndpoint = String.format("%s/%s/%s", computeEndpoint, computeInstanceId, ACTION);
            this.client.doPostRequest(computeEndpoint, request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private void addAllowAllRules(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException {
        CreateSecurityGroupRuleRequest ipv4Request = new CreateSecurityGroupRuleRequest.Builder()
                .securityGroupId(securityGroupId)
                .direction(SECURITY_GROUP_INGRESS_DIRECTION)
                .etherType(IPV4_ETHER_TYPE)
                .build();

        try {
            this.client.doPostRequest(getSecurityGroupRulesApiEndpoint(), ipv4Request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        CreateSecurityGroupRuleRequest ipv6Request = new CreateSecurityGroupRuleRequest.Builder()
                .securityGroupId(securityGroupId)
                .direction(SECURITY_GROUP_INGRESS_DIRECTION)
                .etherType(IPV6_ETHER_TYPE)
                .build();

        try {
            this.client.doPostRequest(getSecurityGroupRulesApiEndpoint(), ipv6Request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private void removeSecurityGroup(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException {
        try {
            String endpoint = String.format("%s/%s", getSecurityGroupsApiEndpoint(), securityGroupId);
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getNetworkPortIp(PublicIpOrder publicIpOrder, OpenStackV3User cloudUser) throws FogbowException {
        String computeInstanceId = publicIpOrder.getComputeId();
        String defaulNetworkId = getDefaultNetworkId();
        String networkPortsEndpointBase = getNetworkPortsEndpoint();

        GetNetworkPortsResquest getNetworkPortsResquest = null;
        try {
            getNetworkPortsResquest = new GetNetworkPortsResquest.Builder()
                    .url(networkPortsEndpointBase)
                    .deviceId(computeInstanceId)
                    .networkId(defaulNetworkId)
                    .build();
            
        } catch (URISyntaxException e) {
            String errorMsg = String.format(Messages.Exception.WRONG_URI_SYNTAX, networkPortsEndpointBase);
            throw new FogbowException(errorMsg, e);
        }

        String responseGetPorts = null;
        try {
            String networkPortsEndpoint = getNetworkPortsResquest.getUrl();
            responseGetPorts = this.client.doGetRequest(networkPortsEndpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        GetNetworkPortsResponse networkPortsResponse = GetNetworkPortsResponse.fromJson(responseGetPorts);

        String networkPortId = null;
        List<GetNetworkPortsResponse.Port> ports = networkPortsResponse.getPorts();
        // One is the maximum threshold of ports in the fogbow for default network
        if (isValidPorts(ports)) {
            return networkPortId = ports.get(0).getId();
        }

        throwPortsException(ports, computeInstanceId, defaulNetworkId);
        return networkPortId;
    }

    protected void throwPortsException(List<GetNetworkPortsResponse.Port> ports, String computeInstanceId, String defaulNetworkId) throws FogbowException {
        String errorMsg = null;
        if (ports == null || ports.size() == 0) {
            errorMsg = String.format(Messages.Exception.PORT_NOT_FOUND, computeInstanceId, defaulNetworkId);
        } else {
            errorMsg = String.format(Messages.Exception.INVALID_PORT_SIZE, String.valueOf(ports.size()), computeInstanceId, defaulNetworkId);
        }
        throw new FogbowException(errorMsg);
    }

    protected void checkProperties(boolean checkProperties) {
        if (!checkProperties) {
            return;
        }

        String defaultNetworkId = getDefaultNetworkId();
        if (defaultNetworkId == null || defaultNetworkId.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.DEFAULT_NETWORK_NOT_FOUND);
        }
        String externalNetworkId = getExternalNetworkId();
        if (externalNetworkId == null || externalNetworkId.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.EXTERNAL_NETWORK_NOT_FOUND);
        }
        String neutroApiEndpoint = getNeutronApiEndpoint();
        if (neutroApiEndpoint == null || neutroApiEndpoint.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.NEUTRON_ENDPOINT_NOT_FOUND);
        }
    }

    protected String getDefaultNetworkId() {
        return this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
    }

    protected String getExternalNetworkId() {
        return this.properties.getProperty(EXTERNAL_NETWORK_ID_KEY);
    }

    protected String getSecurityGroupsApiEndpoint() {
        return getNeutronApiEndpoint() + V2_API_ENDPOINT + SECURITY_GROUPS;
    }

    protected String getSecurityGroupRulesApiEndpoint() {
        return getNeutronApiEndpoint() + V2_API_ENDPOINT + SECURITY_GROUP_RULES;
    }

    protected String getNeutronApiEndpoint() {
        return this.properties.getProperty(NETWORK_NEUTRONV2_URL_KEY);
    }

    protected String getNetworkPortsEndpoint() {
        return getNeutronApiEndpoint() + V2_API_ENDPOINT + PORTS;
    }

    protected String getFloatingIpEndpoint() {
        return getNeutronApiEndpoint() + V2_API_ENDPOINT + FLOATINGIPS;
    }

    private String getComputeEndpoint(String projectId) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + projectId + SERVERS;
    }

    protected boolean isValidPorts(List<GetNetworkPortsResponse.Port> ports) {
        return ports != null && ports.size() == MAXIMUM_PORTS_SIZE;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}
