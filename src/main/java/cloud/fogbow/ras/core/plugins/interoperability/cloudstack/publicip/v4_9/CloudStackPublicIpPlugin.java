package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.*;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CloudStackPublicIpPlugin implements PublicIpPlugin {

    private static final Logger LOGGER = Logger.getLogger(CloudStackPublicIpPlugin.class);

    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";

    public static final String DEFAULT_SSH_PORT = "22";
    public static final String DEFAULT_PROTOCOL = "TCP";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private String cloudStackUrl;
    private final String defaultNetworkId;

    private CloudStackHttpClient client;
    private Properties properties;

    // since the ip creation and association involves multiple synchronous and asynchronous requests,
    // we need to keep track of where we are in the process in order to fulfill the operation.
    private static Map<String, CurrentAsyncRequest> publicIpSubState = new HashMap<>();

    public CloudStackPublicIpPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);

        this.defaultNetworkId = properties.getProperty(DEFAULT_NETWORK_ID_KEY);

        Integer timeout = new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.XMPP_TIMEOUT));
        HttpRequestClientUtil client = new HttpRequestClientUtil(timeout);
        this.client = new CloudStackHttpClient(client);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId,
                                  CloudToken token) throws FogbowException {
        String jobId = requestIpAddressAssociation(defaultNetworkId, token);

        CurrentAsyncRequest currentAsyncRequest = new CurrentAsyncRequest(PublicIpSubState.ASSOCIATING_IP_ADDRESS,
                jobId, computeInstanceId);
        publicIpSubState.put(publicIpOrder.getId(), currentAsyncRequest);

        // we don't have the id of the ip address yet, but since the instance id is only used
        // by the plugin, we can map an orderId to an instanceId in the plugin
        return publicIpOrder.getId();
    }

    @Override
    public PublicIpInstance getInstance(String publicIpInstanceId, CloudToken token) throws FogbowException {
        // since we returned the id of the order on requestInstance, publicIpInstanceId
        // should be the id of the order
        String publicIpOrderId = publicIpInstanceId;

        CurrentAsyncRequest currentAsyncRequest = publicIpSubState.get(publicIpOrderId);

        PublicIpInstance result;
        if (currentAsyncRequest == null) {
            result = new PublicIpInstance(null, InstanceState.FAILED, null);
        } else if (currentAsyncRequest.getState().equals(PublicIpSubState.READY)) {
            result = instanceFromCurrentAsyncRequest(currentAsyncRequest, InstanceState.READY);
        } else {
            result = getCurrentInstance(publicIpOrderId, token);
        }

        return result;
    }

    @Override
    public void deleteInstance(String publicIpInstanceId, String computeInstanceId, CloudToken cloudToken)
            throws FogbowException {
        // since we returned the id of the order on requestInstance, publicIpInstanceId
        // should be the id of the order
        String orderId = publicIpInstanceId;
        String ipAddressId = publicIpSubState.get(orderId).getIpInstanceId();

        DisassociateIpAddressRequest disassociateIpAddressRequest = new DisassociateIpAddressRequest.Builder()
                .id(ipAddressId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(disassociateIpAddressRequest.getUriBuilder(), cloudToken.getTokenValue());

        try {
            this.client.doGetRequest(disassociateIpAddressRequest.getUriBuilder().toString(),
                    cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    public static String getPublicIpId(String orderId) {
        return publicIpSubState.get(orderId).getIpInstanceId();
    }

    // testing purposes only
    public static void setOrderidToInstanceIdMapping(String orderId, String instanceId) {
        CurrentAsyncRequest currentAsyncRequest = new CurrentAsyncRequest(PublicIpSubState.READY, null, instanceId);
        publicIpSubState.put(orderId, currentAsyncRequest);
    }

    private PublicIpInstance getCurrentInstance(String orderId, CloudToken token) throws FogbowException {
        CurrentAsyncRequest currentAsyncRequest = publicIpSubState.get(orderId);
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(this.client,
                currentAsyncRequest.getCurrentJobId(), token);
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);

        PublicIpInstance result;
        switch (queryAsyncJobResult.getJobStatus()) {
            case CloudStackQueryJobResult.PROCESSING:
                result = new PublicIpInstance(null, InstanceState.CREATING, null);
                break;
            case CloudStackQueryJobResult.SUCCESS:
                switch (currentAsyncRequest.getState()) {
                    case ASSOCIATING_IP_ADDRESS:
                        SuccessfulAssociateIpAddressResponse response = SuccessfulAssociateIpAddressResponse.fromJson(jsonResponse);
                        String ipAddressId = response.getIpAddress().getId();
                        currentAsyncRequest.setIpInstanceId(ipAddressId);
                        currentAsyncRequest.setIp(response.getIpAddress().getIpAddress());

                        enableStaticNat(currentAsyncRequest.getComputeInstanceId(), ipAddressId, token);

                        String createFirewallRuleJobId = createFirewallRule(ipAddressId, token);
                        currentAsyncRequest.setCurrentJobId(createFirewallRuleJobId);
                        currentAsyncRequest.setState(PublicIpSubState.CREATING_FIREWALL_RULE);
                        result = instanceFromCurrentAsyncRequest(currentAsyncRequest, InstanceState.CREATING);
                        break;
                    case CREATING_FIREWALL_RULE:
                        currentAsyncRequest.setState(PublicIpSubState.READY);
                        result = instanceFromCurrentAsyncRequest(currentAsyncRequest, InstanceState.READY);
                        break;
                    default:
                        result = null;
                        break;
                }
                break;
            case CloudStackQueryJobResult.FAILURE:
                // any failure should lead to a disassociation of the ip address
                deleteInstance(orderId, null, token);
                result = new PublicIpInstance(null, InstanceState.FAILED, null);
                break;
            default:
                LOGGER.error(Messages.Error.UNEXPECTED_JOB_STATUS);
                result = null;
                break;
        }
        return result;
    }

    private PublicIpInstance instanceFromCurrentAsyncRequest(CurrentAsyncRequest currentAsyncRequest, InstanceState state) {
        String ipInstanceId = currentAsyncRequest == null ? null : currentAsyncRequest.getIpInstanceId();
        String ip = currentAsyncRequest == null ? null : currentAsyncRequest.getIp();
        return new PublicIpInstance(ipInstanceId, state, ip);
    }

    protected String requestIpAddressAssociation(String networkId, CloudToken cloudToken)
            throws FogbowException {
        AssociateIpAddressRequest associateIpAddressRequest = new AssociateIpAddressRequest.Builder()
                .networkId(networkId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(associateIpAddressRequest.getUriBuilder(), cloudToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client
                    .doGetRequest(associateIpAddressRequest.getUriBuilder().toString(),
                            cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        AssociateIpAddressAsyncJobIdResponse associateIpAddressAsyncJobIdResponse = AssociateIpAddressAsyncJobIdResponse
                .fromJson(jsonResponse);

        return associateIpAddressAsyncJobIdResponse.getJobId();
    }

    protected void enableStaticNat(String computeInstanceId, String ipAdressId,
                                   CloudToken cloudToken)
            throws FogbowException {

        EnableStaticNatRequest enableStaticNatRequest = new EnableStaticNatRequest.Builder()
                .ipAddressId(ipAdressId)
                .virtualMachineId(computeInstanceId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil
                .sign(enableStaticNatRequest.getUriBuilder(), cloudToken.getTokenValue());

        try {
            this.client.doGetRequest(enableStaticNatRequest.getUriBuilder().toString(), cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String createFirewallRule(String ipAdressId, CloudToken cloudToken)
            throws FogbowException {
        CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                .protocol(DEFAULT_PROTOCOL)
                .startPort(DEFAULT_SSH_PORT)
                .endPort(DEFAULT_SSH_PORT)
                .ipAddressId(ipAdressId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(createFirewallRuleRequest.getUriBuilder(), cloudToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(createFirewallRuleRequest.getUriBuilder().toString(), cloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);
        return response.getJobId();
    }

    private enum PublicIpSubState {
        ASSOCIATING_IP_ADDRESS, CREATING_FIREWALL_RULE, READY
    }

    private static class CurrentAsyncRequest {

        private PublicIpSubState state;

        private String currentJobId;

        private String ipInstanceId;
        private String ip;

        private String computeInstanceId;

        public CurrentAsyncRequest(PublicIpSubState state, String currentJobId, String computeInstanceId) {
            this.state = state;
            this.currentJobId = currentJobId;
            this.computeInstanceId = computeInstanceId;
        }

        public PublicIpSubState getState() {
            return state;
        }

        public String getCurrentJobId() {
            return currentJobId;
        }

        public String getComputeInstanceId() {
            return computeInstanceId;
        }

        public void setState(PublicIpSubState state) {
            this.state = state;
        }

        public void setCurrentJobId(String currentJobId) {
            this.currentJobId = currentJobId;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getIpInstanceId() {
            return ipInstanceId;
        }

        public void setIpInstanceId(String ipInstanceId) {
            this.ipInstanceId = ipInstanceId;
        }
    }

}
