package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
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
import cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2.GetFloatingIpResponse.FloatingIp;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2.GetNetworkPortsResponse.Port;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2.GetSecurityGroupsResponse.SecurityGroup;

public class OpenStackPublicIpPlugin implements PublicIpPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackPublicIpPlugin.class);

    protected static final String FLOATINGIPS = "/floatingips";
    protected static final String IPV4_ETHER_TYPE = "IPv4";
    protected static final String IPV6_ETHER_TYPE = "IPv6";
    protected static final String PORTS = "/ports";
    protected static final String PUBLIC_IP_RESOURCE = "Public IP";
    protected static final String QUERY_NAME = "?name=";

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
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
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
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
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
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + instanceId;
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }
    
    protected void disassociateSecurityGroup(String name, PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        String computeId = order.getComputeId();
        String endpoint = getComputeAssociationEndpoint(projectId, computeId);
        
        RemoveSecurityGroupFromServerRequest request = new RemoveSecurityGroupFromServerRequest.Builder()
                .name(name)
                .build();
        try {
            this.client.doPostRequest(endpoint, request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }
    
    protected String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getSecurityGroupsEndpoint() + QUERY_NAME + securityGroupName;
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetSecurityGroupsResponse response = doGetSecurityGroupsResponseFrom(json);
        
        List<SecurityGroup> securityGroups = response.getSecurityGroups();
        if (securityGroups != null && !securityGroups.isEmpty()) {
            return securityGroups.listIterator().next().getId();
        } else {
            throw new UnexpectedException(String.format(Messages.Exception.NO_SECURITY_GROUP_FOUND, json));
        }
    }

    protected GetSecurityGroupsResponse doGetSecurityGroupsResponseFrom(String json) throws FogbowException {
        try {
            return GetSecurityGroupsResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = String.format(Messages.Error.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackCloudUtils.SECURITY_GROUP_RESOURCE);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
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
            String message = String.format(Messages.Error.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD, PUBLIC_IP_RESOURCE);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    protected String getFloatingIpEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT 
                + FLOATINGIPS;
    }

    protected void associateSecurityGroup(String securityGroupId, String floatingIpId, PublicIpOrder order,
            OpenStackV3User cloudUser) throws FogbowException {
        
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        String computeId = order.getComputeId();
        String endpoint = getComputeAssociationEndpoint(projectId, computeId);
        String securityGroupName = getSecurityGroupName(floatingIpId);
        
        AddSecurityGroupToServerRequest request = new AddSecurityGroupToServerRequest.Builder()
                .name(securityGroupName)
                .build();
        try {
            this.client.doPostRequest(endpoint, request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            deleteSecurityGroup(securityGroupId, cloudUser);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getSecurityGroupName(String publicIpId) {
        return SystemConstants.PIP_SECURITY_GROUP_PREFIX + publicIpId;
    }

    protected String getComputeAssociationEndpoint(String projectId, String computeId) {
        return this.properties.getProperty(OpenStackCloudUtils.COMPUTE_NOVA_V2_URL_KEY) 
                + OpenStackCloudUtils.COMPUTE_V2_API_ENDPOINT 
                + projectId 
                + OpenStackCloudUtils.SERVERS
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + computeId
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + OpenStackCloudUtils.ACTION;
    }

    protected void deleteSecurityGroup(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getSecurityGroupsEndpoint() 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + securityGroupId;
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected void allowAllIngressSecurityRules(String securityGroupId, OpenStackV3User cloudUser)
            throws FogbowException {

        String[] etherTypes = { IPV4_ETHER_TYPE, IPV6_ETHER_TYPE };

        for (String etherType : etherTypes) {
            CreateSecurityGroupRuleRequest request = new CreateSecurityGroupRuleRequest.Builder()
                    .direction(OpenStackCloudUtils.INGRESS_DIRECTION)
                    .etherType(etherType)
                    .securityGroupId(securityGroupId)
                    .build();

            doPostRequestFromCloud(request, cloudUser);
        }
    }

    protected void doPostRequestFromCloud(CreateSecurityGroupRuleRequest request, OpenStackV3User cloudUser)
            throws FogbowException {
        try {
            this.client.doPostRequest(getSecurityGroupRulesEndpoint(), request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getSecurityGroupRulesEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT  
                + OpenStackCloudUtils.SECURITY_GROUP_RULES;
    }

    protected String doCreateSecurityGroup(String floatingIpId, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        String securityGroupName = getSecurityGroupName(floatingIpId);
        
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
                .name(securityGroupName)
                .projectId(projectId)
                .build();
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doPostRequest(getSecurityGroupsEndpoint(), request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        CreateSecurityGroupResponse securityGroupResponse = doCreateSecurityGroupResponseFrom(jsonResponse);
        return securityGroupResponse.getId();
    }

    protected CreateSecurityGroupResponse doCreateSecurityGroupResponseFrom(String json) throws FogbowException {
        try {
            return CreateSecurityGroupResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = String.format(Messages.Error.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackCloudUtils.SECURITY_GROUP_RESOURCE);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected String getSecurityGroupsEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT 
                + OpenStackCloudUtils.SECURITY_GROUPS;
    }

    protected String doRequestInstance(CreateFloatingIpRequest request, OpenStackV3User cloudUser)
            throws FogbowException {
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doPostRequest(getFloatingIpEndpoint(), request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        CreateFloatingIpResponse response = doCreateFloatingIpResponseFrom(jsonResponse);
        return response.getFloatingIp().getId();
    }

    protected CreateFloatingIpResponse doCreateFloatingIpResponseFrom(String json) throws FogbowException {
        try {
            return CreateFloatingIpResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = String.format(Messages.Error.ERROR_WHILE_CREATING_RESOURCE_S, PUBLIC_IP_RESOURCE);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
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
            throw new UnexpectedException(String.format(Messages.Exception.PORT_NOT_FOUND, computeId, defaulNetworkId));
        }
    }

    protected GetNetworkPortsResponse doGetNetworkPortsResponseFrom(String json) throws FogbowException {
        try {
            return GetNetworkPortsResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = String.format(Messages.Error.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackCloudUtils.NETWORK_PORTS_RESOURCE);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected String doGetResponseFromCloud(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String json = null;
        try {
            json = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return json;
    }

    protected String buildNetworkPortsEndpoint(String deviceId, String networkId, String endpoint)
            throws InvalidParameterException {
        
        GetNetworkPortsResquest resquest = null;
        try {
            resquest = new GetNetworkPortsResquest.Builder()
                    .deviceId(deviceId)
                    .networkId(networkId)
                    .url(endpoint)
                    .build();

        } catch (URISyntaxException e) {
            throw new InvalidParameterException(String.format(Messages.Exception.WRONG_URI_SYNTAX, endpoint), e);
        }
        return resquest.getUrl();
    }
    
    protected String getNetworkPortsEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT 
                + PORTS;
    }
    
    protected void checkProperties() {
        String defaultNetworkId = getDefaultNetworkId();
        if (defaultNetworkId == null || defaultNetworkId.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.DEFAULT_NETWORK_NOT_FOUND);
        }
        String externalNetworkId = getExternalNetworkId();
        if (externalNetworkId == null || externalNetworkId.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.EXTERNAL_NETWORK_NOT_FOUND);
        }
        String neutroApiEndpoint = getNeutronPrefixEndpoint();
        if (neutroApiEndpoint == null || neutroApiEndpoint.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.NEUTRON_ENDPOINT_NOT_FOUND);
        }
    }
    
    protected String getNeutronPrefixEndpoint() {
        return this.properties.getProperty(OpenStackCloudUtils.NETWORK_NEUTRON_V2_URL_KEY);
    }
    
    protected String getExternalNetworkId() {
        return this.properties.getProperty(OpenStackCloudUtils.EXTERNAL_NETWORK_ID_KEY);
    }
    
    protected String getDefaultNetworkId() {
        return this.properties.getProperty(OpenStackCloudUtils.DEFAULT_NETWORK_ID_KEY);
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
    
    private void initClient() {
        this.client = new OpenStackHttpClient();
    }
    
}
