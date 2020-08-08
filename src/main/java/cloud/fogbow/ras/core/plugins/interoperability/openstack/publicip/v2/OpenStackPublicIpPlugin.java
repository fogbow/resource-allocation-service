package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.*;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.*;
import com.google.common.annotations.VisibleForTesting;
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
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
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
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String endpoint = getFloatingIpEndpoint()
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + instanceId;
        
        return doGetInstance(endpoint, cloudUser);
    }
    
    @Override
    public void deleteInstance(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        String securityGroupName = getSecurityGroupName(instanceId);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);
        disassociateSecurityGroup(securityGroupName, order, cloudUser);
        deleteSecurityGroup(securityGroupId, cloudUser);
        doDeleteInstance(instanceId, cloudUser);
    }
    
    @VisibleForTesting
    void doDeleteInstance(String instanceId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getFloatingIpEndpoint() 
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + instanceId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }
    
    @VisibleForTesting
    void disassociateSecurityGroup(String name, PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String computeId = order.getComputeId();
        String endpoint = getComputeAssociationEndpoint(projectId, computeId);
        
        RemoveSecurityGroupFromServerRequest request = new RemoveSecurityGroupFromServerRequest.Builder()
                .name(name)
                .build();
        this.client.doPostRequest(endpoint, request.toJson(), cloudUser);
    }
    
    @VisibleForTesting
    String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
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

    @VisibleForTesting
    GetSecurityGroupsResponse doGetSecurityGroupsResponseFrom(String json) throws FogbowException {
        try {
            return GetSecurityGroupsResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.SECURITY_GROUP_RESOURCE));
        }
    }
    
    @VisibleForTesting
    PublicIpInstance doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = doGetResponseFromCloud(endpoint, cloudUser);
        GetFloatingIpResponse response = doGetFloatingIpResponseFrom(jsonResponse);
        return buildPublicIpInstance(response);
    }
    
    @VisibleForTesting
    PublicIpInstance buildPublicIpInstance(GetFloatingIpResponse response) {
        FloatingIp floatingIp = response.getFloatingIp();
        String id = floatingIp.getId();
        String cloudStatus = floatingIp.getStatus();
        String ip = floatingIp.getFloatingIpAddress();
        return new PublicIpInstance(id, cloudStatus, ip);
    }

    @VisibleForTesting
    GetFloatingIpResponse doGetFloatingIpResponseFrom(String json) throws FogbowException {
        try {
            return GetFloatingIpResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.PUBLIC_IP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.PUBLIC_IP_RESOURCE));
        }
    }
    
    @VisibleForTesting
    String getFloatingIpEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.FLOATINGIPS_ENDPOINT;
    }

    @VisibleForTesting
    void associateSecurityGroup(String securityGroupId, String floatingIpId, PublicIpOrder order,
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

    @VisibleForTesting
    String getSecurityGroupName(String publicIpId) {
        return SystemConstants.PIP_SECURITY_GROUP_PREFIX + publicIpId;
    }

    @VisibleForTesting
    String getComputeAssociationEndpoint(String projectId, String computeId) {
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

    @VisibleForTesting
    void deleteSecurityGroup(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getSecurityGroupsEndpoint() 
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + securityGroupId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    void allowAllIngressSecurityRules(String securityGroupId, OpenStackV3User cloudUser)
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

    @VisibleForTesting
    void doPostRequestFromCloud(CreateSecurityGroupRuleRequest request, OpenStackV3User cloudUser)
            throws FogbowException {
        this.client.doPostRequest(getSecurityGroupRulesEndpoint(), request.toJson(), cloudUser);
    }

    @VisibleForTesting
    String getSecurityGroupRulesEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
    }

    @VisibleForTesting
    String doCreateSecurityGroup(String floatingIpId, OpenStackV3User cloudUser) throws FogbowException {
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

    @VisibleForTesting
    CreateSecurityGroupResponse doCreateSecurityGroupResponseFrom(String json) throws FogbowException {
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

    @VisibleForTesting
    String getSecurityGroupsEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUPS_ENDPOINT;
    }

    @VisibleForTesting
    String doRequestInstance(CreateFloatingIpRequest request, OpenStackV3User cloudUser)
            throws FogbowException {
        
        String jsonResponse = this.client.doPostRequest(getFloatingIpEndpoint(), request.toJson(), cloudUser);

        CreateFloatingIpResponse response = doCreateFloatingIpResponseFrom(jsonResponse);
        return response.getFloatingIp().getId();
    }

    @VisibleForTesting
    CreateFloatingIpResponse doCreateFloatingIpResponseFrom(String json) throws FogbowException {
        try {
            return CreateFloatingIpResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.PUBLIC_IP_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_CREATING_RESOURCE_S,
                    OpenStackConstants.PUBLIC_IP_RESOURCE));
        }
    }

    @VisibleForTesting
    String getNetworkPortId(PublicIpOrder order, OpenStackV3User cloudUser) throws FogbowException {
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

    @VisibleForTesting
    GetNetworkPortsResponse doGetNetworkPortsResponseFrom(String json) throws FogbowException {
        try {
            return GetNetworkPortsResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.NETWORK_PORTS_RESOURCE), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                    OpenStackConstants.NETWORK_PORTS_RESOURCE));
        }
    }

    @VisibleForTesting
    String doGetResponseFromCloud(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String json = this.client.doGetRequest(endpoint, cloudUser);
        return json;
    }

    @VisibleForTesting
    String buildNetworkPortsEndpoint(String deviceId, String networkId, String endpoint)
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
    
    @VisibleForTesting
    String getNetworkPortsEndpoint() {
        return getNeutronPrefixEndpoint() 
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.PORTS_ENDPOINT;
    }
    
    @VisibleForTesting
    void checkProperties() {
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
    
    @VisibleForTesting
    String getNeutronPrefixEndpoint() {
        return this.properties.getProperty(OpenStackPluginUtils.NETWORK_NEUTRON_URL_KEY);
    }
    
    @VisibleForTesting
    String getExternalNetworkId() {
        return this.properties.getProperty(OpenStackPluginUtils.EXTERNAL_NETWORK_ID_KEY);
    }
    
    @VisibleForTesting
    String getDefaultNetworkId() {
        return this.properties.getProperty(OpenStackPluginUtils.DEFAULT_NETWORK_ID_KEY);
    }

    @VisibleForTesting
    void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
    
    private void initClient() {
        this.client = new OpenStackHttpClient();
    }
    
}
