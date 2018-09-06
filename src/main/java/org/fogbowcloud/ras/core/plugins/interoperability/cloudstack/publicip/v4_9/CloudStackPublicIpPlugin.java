package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.QueryAsyncJobResultResponse.QueryAsyncJobResult;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackPublicIpPlugin implements PublicIpPlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackPublicIpPlugin.class);

    private static final String DEFAULT_START_PORT = "22";
    private static final String DEFAULT_END_PORT = "60000";
    private static final String DEFAULT_PROTOCOL = "TCP";

    private HttpRequestClientUtil client;

    Map<String, CurrentState> publicIpSubState;

    public CloudStackPublicIpPlugin() {
        this.client = new HttpRequestClientUtil();
        this.publicIpSubState = new HashMap<>();
    }

    //    @Override
//    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId,
//        CloudStackToken token) throws FogbowRasException, UnexpectedException {
//        // TODO use getInstance of the CloudStackComputePlugin
//        String networkId = "";
//
//        // asynchronous operation
//        String ipAdressId = requestIpAddressAssociation(networkId, token);
//
//        try {
//            enableStaticNat(computeInstanceId, ipAdressId, token);
//        } catch (Exception e) {
//            deleteInstance(computeInstanceId, token);
//        }
//
//        try {
//            // asynchronous operation
//            createFirewallRule(ipAdressId, token);
//        } catch (Exception e) {
//            deleteInstance(computeInstanceId, token);
//        }
//
//        // TODO wait asynchronous operation and check the success
//  		checkOperation();
//
//        return ipAdressId;
//    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId,
        CloudStackToken token) throws FogbowRasException, UnexpectedException {

        // TODO use getInstance of the CloudStackComputePlugin
        String networkId = "";

        String jobId = requestIpAddressAssociation(networkId, token);

        CurrentState currentState = new CurrentState(null,
            PublicIpSubState.ASSOCIATING_IP_ADDRESS, jobId);
        publicIpSubState.put(publicIpOrder.getId(), currentState);

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

        CurrentState currentState = publicIpSubState.get(publicIpOrderId);

        String jsonResponse = getQueryJobResult(currentState.getCurrentJobId(), token);
        QueryAsyncJobResult queryAsyncJobResult = QueryAsyncJobResultResponse.fromJson(jsonResponse);

        switch (queryAsyncJobResult.getJobStatus()) {
            case "0":
                // cloud is still processing request, do nothing yet
                break;
            case "1":
                advanceState(publicIpOrderId, currentState);
                break;
            case "2":
                rollback(currentState.getInstanceId());
                break;
            default:
                LOGGER.error("Job status must be one of {0, 1, 2}");
                break;
        }
        return null;
    }

    private void rollback(String publicIpInstanceId) {

    }

    private void advanceState(String publicIpInstanceId, CurrentState currentState) {
        // request went ok, advance to next state
        //   if state is ASSOCIATING_IP_ADDRESS
        //     return spawning instance
        //   elif state is ENABLING_STATIC_NAT
        //     return spawning instance
        //   elif state is CREATING_FIREWALL_RULE
        //     return fulfilled instance
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

        CloudStackUrlUtil
            .sign(associateIpAddressRequest.getUriBuilder(), cloudStackToken.getTokenValue());

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
            this.client
                .doGetRequest(enableStaticNatRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    protected void createFirewallRule(String ipAdressId, CloudStackToken cloudStackToken)
        throws FogbowRasException {
        CreateFirewallRequest createFirewallRequest = new CreateFirewallRequest.Builder()
            .protocol(DEFAULT_PROTOCOL)
            .startPort(DEFAULT_START_PORT)
            .endPort(DEFAULT_END_PORT)
            .ipAddress(ipAdressId)
            .build();

        CloudStackUrlUtil
            .sign(createFirewallRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        try {
            this.client
                .doGetRequest(createFirewallRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }
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
        ASSOCIATING_IP_ADDRESS, ENABLING_STATIC_NAT, CREATING_FIREWALL_RULE, READY
    }

    private class CurrentState {

        private String instanceId;
        private PublicIpSubState state;
        private String currentJobId;

        public CurrentState(String instanceId,
            PublicIpSubState state, String currentJobId) {
            this.instanceId = instanceId;
            this.state = state;
            this.currentJobId = currentJobId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public PublicIpSubState getState() {
            return state;
        }

        public String getCurrentJobId() {
            return currentJobId;
        }
    }

}
