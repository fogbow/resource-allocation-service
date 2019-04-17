package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.*;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import static cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin.*;

public class OpenStackPublicIpPlugin implements PublicIpPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackPublicIpPlugin.class);

    protected static final String NETWORK_NEUTRONV2_URL_KEY = OpenStackNetworkPlugin.NETWORK_NEUTRONV2_URL_KEY;
    protected static final String DEFAULT_NETWORK_ID_KEY = OpenStackComputePlugin.DEFAULT_NETWORK_ID_KEY;
    protected static final String EXTERNAL_NETWORK_ID_KEY = OpenStackNetworkPlugin.KEY_EXTERNAL_GATEWAY_INFO;

    protected static final String SUFFIX_ENDPOINT_FLOATINGIPS = "/floatingips";
    protected static final String NETWORK_V2_API_ENDPOINT = "/v2.0";
    protected static final String SUFFIX_ENDPOINT_PORTS = "/ports";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUPS = "/security-groups";

    protected static final String QUERY_SECURITY_GROUP_NAME = "name";

    private static final int MAXIMUM_PORTS_SIZE = 1;
    public static final String SECURITY_GROUP_DIRECTION_INGRESS = "ingress";

    public static final String IPV6_ETHER_TYPE = "IPv6";
    public static final String IPV4_ETHER_TYPE = "IPv4";

    protected static final String SECURITY_GROUP_PREFIX = "ras-sg-pip-";

    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackPublicIpPlugin(String confFilePath) {
        this(confFilePath, true);
    }

    public OpenStackPublicIpPlugin(String confFilePath, boolean checkProperties) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
        checkProperties(checkProperties);
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
    public String requestInstance(PublicIpOrder publicIpOrder, OpenStackV3User cloudUser)
            throws FogbowException {
        boolean requestFailed = true;
        String securityGroupName = null;
        String securityGroupId = null;
        String floatingIpId = null;
        String computeInstanceId = null;

        try {
            computeInstanceId = publicIpOrder.getComputeId();
            // Network port id is the connection between the virtual machine and the network
            String networkPortId = getNetworkPortIp(computeInstanceId, cloudUser);
            String floatingNetworkId = getExternalNetworkId();
            String projectId = cloudUser.getProjectId();

            CreateFloatingIpRequest createFloatingIpRequest = new CreateFloatingIpRequest.Builder()
                    .floatingNetworkId(floatingNetworkId)
                    .projectId(projectId)
                    .portId(networkPortId)
                    .build();
            String body = createFloatingIpRequest.toJson();

            String responsePostFloatingIp = null;
            try {
                String floatingIpEndpoint = getFloatingIpEndpoint();
                responsePostFloatingIp = this.client.doPostRequest(floatingIpEndpoint, body, cloudUser);
            } catch (HttpResponseException e) {
                OpenStackHttpToFogbowExceptionMapper.map(e);
            }
            CreateFloatingIpResponse createFloatingIpResponse = CreateFloatingIpResponse.fromJson(responsePostFloatingIp);

            CreateFloatingIpResponse.FloatingIp floatingIp = createFloatingIpResponse.getFloatingIp();
            floatingIpId = floatingIp.getId();

            securityGroupName = getSecurityGroupName(floatingIpId);
            securityGroupId = createAndAssociateSecurityGroup(computeInstanceId, cloudUser, securityGroupName);

            // ensuring the floating ip and the security group were created properly
            if (floatingIpId != null && securityGroupId != null) {
                requestFailed = false;
            }

            return floatingIpId;
        } finally {
            if (requestFailed) {
                if (securityGroupName != null) {
                    disassociateSecurityGroupFromCompute(securityGroupName, computeInstanceId, cloudUser);
                }
                if (securityGroupId != null) {
                    removeSecurityGroup(securityGroupId, cloudUser);
                }
                if (floatingIpId != null) {
                    deleteInstance(floatingIpId, cloudUser);
                }
            }
        }
    }

    @Override
    public PublicIpInstance getInstance(String instanceId, OpenStackV3User cloudUser) throws FogbowException {
        String responseGetFloatingIp = null;
        PublicIpOrder order = (PublicIpOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(instanceId);
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
    public void deleteInstance(String instanceId, OpenStackV3User cloudUser) throws FogbowException {
        try {
            PublicIpOrder order = (PublicIpOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(instanceId);
            if (order == null) {
                throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
            }
            String computeInstanceId = order.getComputeId();
            String floatingIpId = order.getInstanceId();
            String securityGroupName = getSecurityGroupName(floatingIpId);

            if (computeInstanceId != null) {
                disassociateSecurityGroupFromCompute(securityGroupName, computeInstanceId, cloudUser);
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

    private String createAndAssociateSecurityGroup(String computeInstanceId, OpenStackV3User cloudUser, String securityGroupName) throws FogbowException {
        String securityGroupId = createSecurityGroup(cloudUser, securityGroupName);
        addAllowAllRules(securityGroupId, cloudUser);
        associateSecurityGroupToCompute(securityGroupName, computeInstanceId, cloudUser);
        return securityGroupId;
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

    private void disassociateSecurityGroupFromCompute(String securityGroupName, String computeInstanceId, OpenStackV3User cloudUser) throws FogbowException {
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
                .direction(SECURITY_GROUP_DIRECTION_INGRESS)
                .etherType(IPV4_ETHER_TYPE)
                .build();

        try {
            this.client.doPostRequest(getSecurityGroupRulesApiEndpoint(), ipv4Request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        CreateSecurityGroupRuleRequest ipv6Request = new CreateSecurityGroupRuleRequest.Builder()
                .securityGroupId(securityGroupId)
                .direction(SECURITY_GROUP_DIRECTION_INGRESS)
                .etherType(IPV6_ETHER_TYPE)
                .build();

        try {
            this.client.doPostRequest(getSecurityGroupRulesApiEndpoint(), ipv6Request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private String createSecurityGroup(OpenStackV3User cloudUser, String securityGroupName) throws FogbowException {
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
                .projectId(cloudUser.getProjectId())
                .name(securityGroupName)
                .build();

        String response = null;
        try {
            response = this.client.doPostRequest(getSecurityGroupsApiEndpoint(), request.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        CreateSecurityGroupResponse securityGroupResponse = CreateSecurityGroupResponse.fromJson(response);
        return securityGroupResponse.getId();
    }

    private void removeSecurityGroup(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException {
        try {
            String endpoint = String.format("%s/%s", getSecurityGroupsApiEndpoint(), securityGroupId);
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getNetworkPortIp(String computeInstanceId, OpenStackV3User cloudUser) throws FogbowException {
        String defaulNetworkId = getDefaultNetworkId();
        String networkPortsEndpointBase = getNetworkPortsEndpoint();

        GetNetworkPortsResquest getNetworkPortsResquest = null;
        try {
            getNetworkPortsResquest = new GetNetworkPortsResquest.Builder()
                    .url(networkPortsEndpointBase).deviceId(computeInstanceId).networkId(defaulNetworkId).build();
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
        return getNeutronApiEndpoint() + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_SECURITY_GROUPS;
    }

    protected String getSecurityGroupRulesApiEndpoint() {
        return getNeutronApiEndpoint() + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
    }

    protected String getNeutronApiEndpoint() {
        return this.properties.getProperty(NETWORK_NEUTRONV2_URL_KEY);
    }

    protected String getNetworkPortsEndpoint() {
        return getNeutronApiEndpoint() + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_PORTS;
    }

    protected String getFloatingIpEndpoint() {
        return getNeutronApiEndpoint() + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_FLOATINGIPS;
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

    public static String getSecurityGroupName(String publicIpId) {
        return SECURITY_GROUP_PREFIX + publicIpId;
    }

}
