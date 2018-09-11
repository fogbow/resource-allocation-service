package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryAsyncJobResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CloudStackPublicIpPlugin implements PublicIpPlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackPublicIpPlugin.class);

    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";

    public static final String DEFAULT_START_PORT = "22";
    public static final String DEFAULT_END_PORT = "60000";
    public static final String DEFAULT_PROTOCOL = "TCP";

    public static final int PROCESSING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILURE = 2;

    private final String defaultNetworkId;

    private HttpRequestClientUtil client;

    // since the ip creation and association involves multiple synchronous and asynchronous requests,
    // we need to keep track of where we are in the process in order to fulfill the operation.
    Map<String, CurrentAsyncRequest> publicIpSubState;

    public CloudStackPublicIpPlugin() {
        String cloudStackConfFilePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.defaultNetworkId = properties.getProperty(DEFAULT_NETWORK_ID_KEY);

        this.client = new HttpRequestClientUtil();
        this.publicIpSubState = new HashMap<>();
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId,
                                  CloudStackToken token) throws FogbowRasException, UnexpectedException {
        String jobId = requestIpAddressAssociation(defaultNetworkId, token);

        CurrentAsyncRequest currentAsyncRequest = new CurrentAsyncRequest(PublicIpSubState.ASSOCIATING_IP_ADDRESS,
                jobId, null, computeInstanceId);
        publicIpSubState.put(publicIpOrder.getId(), currentAsyncRequest);

        // TODO doAssociateRequest

        // we dont have the id of the ip address yet, but since the instance id is only used
        // by the plugin, we can map an orderId to an instanceId in the plugin
        return publicIpOrder.getId();
    }

    @Override
    public PublicIpInstance getInstance(String publicIpInstanceId, CloudStackToken token)
            throws FogbowRasException, UnexpectedException {
        // since we returned the id of the order on requestInstance, publicIpInstanceId
        // should be the id of the order
        String publicIpOrderId = publicIpInstanceId;

        CurrentAsyncRequest currentAsyncRequest = publicIpSubState.get(publicIpOrderId);

        PublicIpInstance result;
        if (currentAsyncRequest == null) {
            result = new PublicIpInstance(null, InstanceState.FAILED, null);
        } else if (currentAsyncRequest.equals(PublicIpSubState.READY)) {
            result = instanceFromCurrentAsyncRequest(currentAsyncRequest, InstanceState.READY);
        } else {
            result = getCurrentInstance(token, currentAsyncRequest);
        }

        return result;
    }

    private PublicIpInstance getCurrentInstance(CloudStackToken token, CurrentAsyncRequest currentAsyncRequest) throws FogbowRasException {
        PublicIpInstance result;
        String jsonResponse = getQueryJobResult(currentAsyncRequest.getCurrentJobId(), token);
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);

        switch (queryAsyncJobResult.getJobStatus()) {
            case PROCESSING:
                result = new PublicIpInstance(null, InstanceState.SPAWNING, null);
                break;
            case SUCCESS:
                AssociateIpAddressResponse response = AssociateIpAddressResponse.fromJson(jsonResponse);
                String ipAddressId = response.getIpAddress().getId();
                switch (currentAsyncRequest.getState()) {
                    case ASSOCIATING_IP_ADDRESS:
                        enableStaticNat(currentAsyncRequest.getComputeInstanceId(), ipAddressId, token);

                        String createFirewallRuleJobId = createFirewallRule(ipAddressId, token);
                        currentAsyncRequest.setCurrentJobId(createFirewallRuleJobId);
                        currentAsyncRequest.setState(PublicIpSubState.CREATING_FIREWALL_RULE);
                        result = instanceFromCurrentAsyncRequest(currentAsyncRequest, InstanceState.SPAWNING);
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
            case FAILURE:
                // any failure will lead to a disassociation of the ip address
                rollback(currentAsyncRequest.getIpInstanceId());
                result = new PublicIpInstance(null, InstanceState.FAILED, null);
                break;
            default:
                LOGGER.error("Job status must be one of {0, 1, 2}");
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

    private void rollback(String publicIpInstanceId) {

    }

    @Override
    public void deleteInstance(String ipAddressId, CloudStackToken cloudStackToken)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info("");

        DisassociateIpAddressRequest disassociateIpAddressRequest = new DisassociateIpAddressRequest.Builder()
                .id(ipAddressId)
                .build();

        CloudStackUrlUtil
                .sign(disassociateIpAddressRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        try {
            this.client.doGetRequest(disassociateIpAddressRequest.getUriBuilder().toString(),
                    cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        // TODO wait asynchronous operation and check the success
    }

    protected String requestIpAddressAssociation(String networkId, CloudStackToken cloudStackToken)
            throws FogbowRasException {
        AssociateIpAddressRequest associateIpAddressRequest = new AssociateIpAddressRequest.Builder()
                .networkId(networkId)
                .build();

        CloudStackUrlUtil.sign(associateIpAddressRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client
                    .doGetRequest(associateIpAddressRequest.getUriBuilder().toString(),
                            cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        AssociateIpAddressSyncJobIdResponse associateIpAddressSyncJobIdResponse = AssociateIpAddressSyncJobIdResponse
                .fromJson(jsonResponse);

        return associateIpAddressSyncJobIdResponse.getJobId();
//        checkOperation(associateIpAddressSyncJobIdResponse.getJobId(), cloudStackToken);
//
//        jsonResponse = getQueryJobResult(jobId, cloudStackToken);
//        AssociateIpAddressResponse associateIpAddressResponse = AssociateIpAddressResponse
//            .fromJson(jsonResponse);
//
//        IpAddress ipAddress = associateIpAddressResponse.getIpAddress();
//        return ipAddress.getId();
    }

    protected void enableStaticNat(String computeInstanceId, String ipAdressId,
                                   CloudStackToken cloudStackToken)
            throws FogbowRasException {

        EnableStaticNatRequest enableStaticNatRequest = new EnableStaticNatRequest.Builder()
                .ipAddressId(ipAdressId)
                .virtualMachineId(computeInstanceId)
                .build();

        CloudStackUrlUtil
                .sign(enableStaticNatRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        try {
            this.client.doGetRequest(enableStaticNatRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    protected String createFirewallRule(String ipAdressId, CloudStackToken cloudStackToken)
            throws FogbowRasException {
        CreateFirewallRequest createFirewallRequest = new CreateFirewallRequest.Builder()
                .protocol(DEFAULT_PROTOCOL)
                .startPort(DEFAULT_START_PORT)
                .endPort(DEFAULT_END_PORT)
                .ipAddress(ipAdressId)
                .build();

        CloudStackUrlUtil.sign(createFirewallRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(createFirewallRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        CreateFirewallResponse response = CreateFirewallResponse.fromJson(jsonResponse);
        return response.getJobId();
    }

    private String getQueryJobResult(String jobId, CloudStackToken cloudStackToken)
            throws FogbowRasException {
        QueryAsyncJobResultRequest queryAsyncJobResultRequest = new QueryAsyncJobResultRequest.Builder()
                .jobId(jobId)
                .build();

        CloudStackUrlUtil
                .sign(queryAsyncJobResultRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client
                    .doGetRequest(queryAsyncJobResultRequest.getUriBuilder().toString(),
                            cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return jsonResponse;
    }

    // TODO complete the implementation
    private void checkOperation(String jobId, CloudStackToken cloudStackToken)
            throws FogbowRasException {
        // TODO implement the codition
        boolean condition = true;
        do {

            getQueryJobResult(jobId, cloudStackToken);

            // TODO
            // jobStatus: 1 success
            // jobStatus: 2 failed
        } while (condition);

    }

    private enum PublicIpSubState {
        ASSOCIATING_IP_ADDRESS, CREATING_FIREWALL_RULE, READY
    }

    private class CurrentAsyncRequest {

        private PublicIpSubState state;

        private String currentJobId;

        private String ipInstanceId;
        private String ip;

        private String computeInstanceId;

        public CurrentAsyncRequest(PublicIpSubState state, String currentJobId, String ipInstanceId, String computeInstanceId) {
            this.state = state;
            this.currentJobId = currentJobId;
            this.ipInstanceId = ipInstanceId;
            this.computeInstanceId = computeInstanceId;
        }

        public PublicIpSubState getState() {
            return state;
        }

        public String getCurrentJobId() {
            return currentJobId;
        }

        public String getIpInstanceId() {
            return ipInstanceId;
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
    }

}
