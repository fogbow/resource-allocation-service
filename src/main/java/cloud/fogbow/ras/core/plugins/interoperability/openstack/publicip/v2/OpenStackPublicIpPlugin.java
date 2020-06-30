package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.*;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.*;
import org.apache.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetFloatingIpResponse.FloatingIp;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetNetworkPortsResponse.Port;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetSecurityGroupsResponse.SecurityGroup;

public class OpenStackPublicIpPlugin implements PublicIpPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackPublicIpPlugin.class);
    private static final int SSH_DEFAULT_PORT = 22;

    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackPublicIpPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        checkProperties();
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
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        // Network port id is the connection between the virtual machine and the network
        String networkPortId = getNetworkPortId(order, cloudUser);
        String floatingNetworkId = getExternalNetworkId();

        CreateFloatingIpRequest request = new CreateFloatingIpRequest.Builder()
                .floatingNetworkId(floatingNetworkId)
                .portId(networkPortId)
                .projectId(projectId)
                .build();

        String instanceId = doRequestInstance(request, cloudUser);
        String securityGroupId = doCreateSecurityGroup(instanceId, cloudUser);
        allowAllIngressSecurityRules(securityGroupId, cloudUser);
        associateSecurityGroup(securityGroupId, instanceId, order, cloudUser);
        return instanceId;
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        String endpoint = getFloatingIpEndpoint() 
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + instanceId;
        
        return doGetInstance(endpoint, cloudUser);
    }
    
    @Override
    public void deleteInstance(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        String securityGroupName = getSecurityGroupName(instanceId);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);
        disassociateSecurityGroup(securityGroupName, order, cloudUser);
        deleteSecurityGroup(securityGroupId, cloudUser);
        doDeleteInstance(instanceId, cloudUser);
    }
    
    protected void doDeleteInstance(String instanceId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getFloatingIpEndpoint() 
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + instanceId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }
    
    protected void disassociateSecurityGroup(String name, PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String computeId = order.getComputeId();
        String endpoint = getComputeAssociationEndpoint(projectId, computeId);
        
        RemoveSecurityGroupFromServerRequest request = new RemoveSecurityGroupFromServerRequest.Builder()
                .name(name)
                .build();
        this.client.doPostRequest(endpoint, request.toJson(), cloudUser);
    }
    
    protected String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getSecurityGroupsEndpoint() + OpenStackConstants.QUERY_NAME + securityGroupName;
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetSecurityGroupsResponse response = doGetSecurityGroupsResponseFrom(json);
        
        List<SecurityGroup> securityGroups = response.getSecurityGroups();
        if (securityGroups != null && !securityGroups.isEmpty()) {
            return securityGroups.listIterator().next().getId();
        } else {
            throw new InternalServerErrorException(String.format(Messages.Exception.NO_SECURITY_GROUP_FOUND_S, json));
        }
    }

    protected GetSecurityGroupsResponse doGetSecurityGroupsResponseFrom(String json) throws FogbowException {
        try {
            return GetSecurityGroupsResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE));
        }
    }
    
    protected PublicIpInstance doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = doGetResponseFromCloud(endpoint, cloudUser);
        GetFloatingIpResponse response = doGetFloatingIpResponseFrom(jsonResponse);
        return buildPublicIpInstance(response);
    }
    
    protected PublicIpInstance buildPublicIpInstance(GetFloatingIpResponse response) {
        FloatingIp floatingIp = response.getFloatingIp();
        String id = floatingIp.getId();
        String cloudStatus = floatingIp.getStatus();
        String ip = floatingIp.getFloatingIpAddress();
        return new PublicIpInstance(id, cloudStatus, ip);
    }

    protected GetFloatingIpResponse doGetFloatingIpResponseFrom(String json) throws FogbowException {
        try {
            return GetFloatingIpResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.PUBLIC_IP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.PUBLIC_IP_RESOURCE));
        }
    }
    
    protected String getFloatingIpEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.FLOATINGIPS_ENDPOINT;
    }

    protected void associateSecurityGroup(String securityGroupId, String floatingIpId, PublicIpOrder order,
            OpenStackV3User cloudUser) throws FogbowException {
        
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String computeId = order.getComputeId();
        String endpoint = getComputeAssociationEndpoint(projectId, computeId);
        String securityGroupName = getSecurityGroupName(floatingIpId);
        
        AddSecurityGroupToServerRequest request = new AddSecurityGroupToServerRequest.Builder()
                .name(securityGroupName)
                .build();
        try {
            this.client.doPostRequest(endpoint, request.toJson(), cloudUser);
        } catch (FogbowException e) {
            deleteSecurityGroup(securityGroupId, cloudUser);
            throw e;
        }
    }

    protected String getSecurityGroupName(String publicIpId) {
        return SystemConstants.PIP_SECURITY_GROUP_PREFIX + publicIpId;
    }

    protected String getComputeAssociationEndpoint(String projectId, String computeId) {
        return this.properties.getProperty(OpenStackPluginUtils.COMPUTE_NOVA_URL_KEY)
                + OpenStackConstants.NOVA_V2_API_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + projectId 
                + OpenStackConstants.SERVERS_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + computeId
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + OpenStackConstants.ACTION;
    }

    protected void deleteSecurityGroup(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getSecurityGroupsEndpoint() 
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + securityGroupId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    protected void allowAllIngressSecurityRules(String securityGroupId, OpenStackV3User cloudUser)
            throws FogbowException {

        String[] etherTypes = { OpenStackConstants.IPV4_ETHER_TYPE, OpenStackConstants.IPV6_ETHER_TYPE };

        for (String etherType : etherTypes) {
            CreateSecurityGroupRuleRequest request = new CreateSecurityGroupRuleRequest.Builder()
                    .securityGroupId(securityGroupId)
                    .direction(OpenStackConstants.INGRESS_DIRECTION)
                    .etherType(etherType)
                    .minPort(SSH_DEFAULT_PORT)
                    .maxPort(SSH_DEFAULT_PORT)
                    .protocol(OpenStackConstants.TCP_PROTOCOL)
                    .build();

            doPostRequestFromCloud(request, cloudUser);
        }
    }

    protected void doPostRequestFromCloud(CreateSecurityGroupRuleRequest request, OpenStackV3User cloudUser)
            throws FogbowException {
        this.client.doPostRequest(getSecurityGroupRulesEndpoint(), request.toJson(), cloudUser);
    }

    protected String getSecurityGroupRulesEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
    }

    protected String doCreateSecurityGroup(String floatingIpId, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String securityGroupName = getSecurityGroupName(floatingIpId);
        
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
                .name(securityGroupName)
                .projectId(projectId)
                .build();
        
        String jsonResponse = this.client.doPostRequest(getSecurityGroupsEndpoint(), request.toJson(), cloudUser);
        CreateSecurityGroupResponse securityGroupResponse = doCreateSecurityGroupResponseFrom(jsonResponse);
        return securityGroupResponse.getId();
    }

    protected CreateSecurityGroupResponse doCreateSecurityGroupResponseFrom(String json) throws FogbowException {
        try {
            return CreateSecurityGroupResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = String.format(Messages.Log.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE);
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE));
        }
    }

    protected String getSecurityGroupsEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUPS_ENDPOINT;
    }

    protected String doRequestInstance(CreateFloatingIpRequest request, OpenStackV3User cloudUser)
            throws FogbowException {
        
        String jsonResponse = this.client.doPostRequest(getFloatingIpEndpoint(), request.toJson(), cloudUser);

        CreateFloatingIpResponse response = doCreateFloatingIpResponseFrom(jsonResponse);
        return response.getFloatingIp().getId();
    }

    protected CreateFloatingIpResponse doCreateFloatingIpResponseFrom(String json) throws FogbowException {
        try {
            return CreateFloatingIpResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.PUBLIC_IP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.PUBLIC_IP_RESOURCE));
        }
    }

    protected String getNetworkPortId(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String computeId = order.getComputeId();
        String defaulNetworkId = getDefaultNetworkId();
        String endpointBase = getNetworkPortsEndpoint();
        String endpoint = buildNetworkPortsEndpoint(computeId, defaulNetworkId, endpointBase);
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetNetworkPortsResponse response = doGetNetworkPortsResponseFrom(json);

        List<Port> ports = response.getPorts();
        if (ports != null && !ports.isEmpty()) {
            return ports.listIterator().next().getId();
        } else {
            throw new InternalServerErrorException(String.format(Messages.Exception.PORT_NOT_FOUND_S, computeId, defaulNetworkId));
        }
    }

    protected GetNetworkPortsResponse doGetNetworkPortsResponseFrom(String json) throws FogbowException {
        try {
            return GetNetworkPortsResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.NETWORK_PORTS_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.NETWORK_PORTS_RESOURCE));
        }
    }

    protected String doGetResponseFromCloud(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String json = this.client.doGetRequest(endpoint, cloudUser);
        return json;
    }

    protected String buildNetworkPortsEndpoint(String deviceId, String networkId, String endpoint)
            throws InternalServerErrorException {
        
        GetNetworkPortsResquest resquest = null;
        try {
            resquest = new GetNetworkPortsResquest.Builder()
                    .deviceId(deviceId)
                    .networkId(networkId)
                    .url(endpoint)
                    .build();

        } catch (URISyntaxException e) {
            throw new InternalServerErrorException(String.format(Messages.Exception.WRONG_URI_SYNTAX_S, endpoint));
        }
        return resquest.getUrl();
    }
    
    protected String getNetworkPortsEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.PORTS_ENDPOINT;
    }
    
    protected void checkProperties() {
        String defaultNetworkId = getDefaultNetworkId();
        if (defaultNetworkId == null || defaultNetworkId.isEmpty()) {
            throw new FatalErrorException(Messages.Exception.DEFAULT_NETWORK_NOT_FOUND);
        }
        String externalNetworkId = getExternalNetworkId();
        if (externalNetworkId == null || externalNetworkId.isEmpty()) {
            throw new FatalErrorException(Messages.Exception.EXTERNAL_NETWORK_NOT_FOUND);
        }
        String neutroApiEndpoint = getNeutronPrefixEndpoint();
        if (neutroApiEndpoint == null || neutroApiEndpoint.isEmpty()) {
            throw new FatalErrorException(Messages.Exception.NEUTRON_ENDPOINT_NOT_FOUND);
        }
    }
    
    protected String getNeutronPrefixEndpoint() {
        return this.properties.getProperty(OpenStackPluginUtils.NETWORK_NEUTRON_URL_KEY);
    }
    
    protected String getExternalNetworkId() {
        return this.properties.getProperty(OpenStackPluginUtils.EXTERNAL_NETWORK_ID_KEY);
    }
    
    protected String getDefaultNetworkId() {
        return this.properties.getProperty(OpenStackPluginUtils.DEFAULT_NETWORK_ID_KEY);
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
    
    private void initClient() {
        this.client = new OpenStackHttpClient();
    }
    
}
