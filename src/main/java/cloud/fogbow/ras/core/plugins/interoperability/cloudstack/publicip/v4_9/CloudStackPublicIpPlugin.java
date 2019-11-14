package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.*;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CloudStackPublicIpPlugin implements PublicIpPlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackPublicIpPlugin.class);

    public static final String DEFAULT_SSH_PORT = "22";
    public static final String DEFAULT_PROTOCOL = "TCP";

    // Since the ip creation and association involves multiple asynchronous requests instance,
    // we need to keep track of where we are in the process in order to fulfill the operation.
    private static Map<String, AsyncRequestInstanceState> asyncRequestInstanceStateMap = new HashMap<>();

    private final String defaultNetworkId;
    private CloudStackHttpClient client;
    private String cloudStackUrl;

    public CloudStackPublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.defaultNetworkId = properties.getProperty(CloudStackCloudUtils.DEFAULT_NETWORK_ID_KEY);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.PUBLIC_IP, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.PUBLIC_IP, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(@NotNull PublicIpOrder publicIpOrder,
                                  @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);
        return doRequestInstance(publicIpOrder, cloudStackUser);
    }

    @Override
    public PublicIpInstance getInstance(@NotNull PublicIpOrder publicIpOrder,
                                        @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, publicIpOrder.getInstanceId()));
        return doGetInstance(publicIpOrder, cloudStackUser);
    }

    @Override
    public void deleteInstance(@NotNull PublicIpOrder publicIpOrder, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, publicIpOrder.getInstanceId()));
        doDeleteInstance(publicIpOrder, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    public String doRequestInstance(@NotNull PublicIpOrder publicIpOrder,
                                    @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        String jobId = requestIpAddressAssociation(this.defaultNetworkId, cloudStackUser);
        setAsyncRequestInstanceFirstStep(jobId, publicIpOrder);
        return getInstanceId(publicIpOrder);
    }

    @VisibleForTesting
    void doDeleteInstance(@NotNull PublicIpOrder publicIpOrder, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        String instanceId = getInstanceId(publicIpOrder);
        AsyncRequestInstanceState asyncRequestInstanceState = this.asyncRequestInstanceStateMap.get(instanceId);
        if (asyncRequestInstanceState == null) {
            throw new InstanceNotFoundException();
        }
        String ipAddressId = asyncRequestInstanceState.getIpInstanceId();

        DisassociateIpAddressRequest disassociateIpAddressRequest = new DisassociateIpAddressRequest.Builder()
                .id(ipAddressId)
                .build(this.cloudStackUrl);

        URIBuilder uriRequest = disassociateIpAddressRequest.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    PublicIpInstance doGetInstance(@NotNull PublicIpOrder publicIpOrder,
                                   @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        String instanceId = getInstanceId(publicIpOrder);
        AsyncRequestInstanceState asyncRequestInstanceState = this.asyncRequestInstanceStateMap.get(instanceId);

        boolean isAOperationalFailure = asyncRequestInstanceState == null;
        if (isAOperationalFailure) {
            // This may happen due to a failure in the RAS while this operation was being carried out; since the
            // order was still spawning, the spawning processor will start monitoring this order after the RAS
            // is restarted. Unfortunately, even if the operation succeeded, we cannot retrieve this information
            // and will have to signal that the order has failed.
            return buildFailedPublicIpInstance();
        }

        if (asyncRequestInstanceState.isReady()) {
            return buildReadyPublicIpInstance(asyncRequestInstanceState);
        } else {
            return buildCurrentPublicIpInstance(publicIpOrder, cloudStackUser);
        }
    }


    /**
     * We don't have the id of the ip address yet, but since the instance id is only used
     * by the plugin, we can return an orderId as an instanceId in the plugin
     */
    @NotNull
    @VisibleForTesting
    String getInstanceId(@NotNull PublicIpOrder publicIpOrder) {
        return publicIpOrder.getId();
    }

    /**
     * Retrieve the current Cloudstack asynchronous job and treat the next operation of the
     * asynchronous request instance flow.
     */
    @NotNull
    @VisibleForTesting
    PublicIpInstance buildCurrentPublicIpInstance(@NotNull PublicIpOrder publicIpOrder,
                                                  @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        String temporaryInstanceId = getInstanceId(publicIpOrder);
        AsyncRequestInstanceState asyncRequestInstanceState =
                this.asyncRequestInstanceStateMap.get(temporaryInstanceId);

        String currentJobId = asyncRequestInstanceState.getCurrentJobId();
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(
                this.client, this.cloudStackUrl, currentJobId, cloudStackUser);
        CloudStackQueryAsyncJobResponse response = CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);

        switch (response.getJobStatus()) {
            case CloudStackQueryJobResult.PROCESSING:
                return buildProcessingPublicIpInstance();
            case CloudStackQueryJobResult.SUCCESS:
                return buildNextOperationPublicIpInstance(
                        asyncRequestInstanceState, cloudStackUser, jsonResponse);
            case CloudStackQueryJobResult.FAILURE:
                // any failure should lead to a disassociation of the ip address
                deleteInstance(publicIpOrder, cloudStackUser);
                return buildFailedPublicIpInstance();
            default:
                LOGGER.error(Messages.Error.UNEXPECTED_JOB_STATUS);
                return null;
        }
    }

    /**
     * Execute the next operation of the asynchronous request instance flow.
     */
    @NotNull
    @VisibleForTesting
    PublicIpInstance buildNextOperationPublicIpInstance(@NotNull AsyncRequestInstanceState asyncRequestInstanceState,
                                                        @NotNull CloudStackUser cloudStackUser,
                                                        String jsonResponse)
            throws FogbowException {

        AsyncRequestInstanceState.StateType currentInstanceState = asyncRequestInstanceState.getState();
        switch (currentInstanceState) {
            case ASSOCIATING_IP_ADDRESS:
                doCreatingFirewallOperation(asyncRequestInstanceState, cloudStackUser, jsonResponse);
                return buildCreatingFirewallPublicIpInstance(asyncRequestInstanceState);
            case CREATING_FIREWALL_RULE:
                finishAsyncRequestInstanceSteps(asyncRequestInstanceState);
                return buildReadyPublicIpInstance(asyncRequestInstanceState);
            default:
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR);
                return null;
        }
    }

    @VisibleForTesting
    void doCreatingFirewallOperation(@NotNull AsyncRequestInstanceState asyncRequestInstanceState,
                                     @NotNull CloudStackUser cloudStackUser,
                                     String jsonResponse)
            throws FogbowException {

        SuccessfulAssociateIpAddressResponse response =
                SuccessfulAssociateIpAddressResponse.fromJson(jsonResponse);

        doEnableStaticNat(response, asyncRequestInstanceState, cloudStackUser);

        String createFirewallRuleJobId = doCreateFirewallRule(response, cloudStackUser);

        setAsyncRequestInstanceSecondStep(response, asyncRequestInstanceState, createFirewallRuleJobId);
    }

    /**
     * Set the asynchronous request instance to the first stage; This stage consist of
     * wait the asynchronous Associating Ip Address Operation finish in the Cloudstack.
     */
    @VisibleForTesting
    void setAsyncRequestInstanceFirstStep(String jobId, @NotNull PublicIpOrder publicIpOrder) {
        String computeId = publicIpOrder.getComputeId();
        String instanceId = getInstanceId(publicIpOrder);
        AsyncRequestInstanceState asyncRequestInstanceState = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS, jobId, computeId);
        this.asyncRequestInstanceStateMap.put(instanceId, asyncRequestInstanceState);
    }

    /**
     * Set the asynchronous request instance to the second stage; This stage consist of
     * wait the asynchronous Create Firewall Operation finish in the Cloudstack.
     */
    @VisibleForTesting
    void setAsyncRequestInstanceSecondStep(@NotNull SuccessfulAssociateIpAddressResponse response,
                                           @NotNull AsyncRequestInstanceState asyncRequestInstanceState,
                                           String createFirewallRuleJobId) {

        String ipAddressId = response.getIpAddress().getId();
        String ipAddress = response.getIpAddress().getIpAddress();

        asyncRequestInstanceState.setIpInstanceId(ipAddressId);
        asyncRequestInstanceState.setIp(ipAddress);
        asyncRequestInstanceState.setCurrentJobId(createFirewallRuleJobId);
        asyncRequestInstanceState.setState(AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE);
    }

    @VisibleForTesting
    void finishAsyncRequestInstanceSteps(@NotNull AsyncRequestInstanceState asyncRequestInstanceState) {
        asyncRequestInstanceState.setState(AsyncRequestInstanceState.StateType.READY);
    }

    @NotNull
    @VisibleForTesting
    String doCreateFirewallRule(@NotNull SuccessfulAssociateIpAddressResponse response,
                                @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        String ipAddressId = response.getIpAddress().getId();
        return requestCreateFirewallRule(ipAddressId, cloudStackUser);
    }

    @VisibleForTesting
    void doEnableStaticNat(@NotNull SuccessfulAssociateIpAddressResponse response,
                      @NotNull AsyncRequestInstanceState asyncRequestInstanceState,
                      @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        String ipAddressId = response.getIpAddress().getId();
        String computeInstanceId = asyncRequestInstanceState.getComputeInstanceId();

        requestEnableStaticNat(computeInstanceId, ipAddressId, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    PublicIpInstance buildCreatingFirewallPublicIpInstance(
            @NotNull AsyncRequestInstanceState asyncRequestInstanceState) {
        return buildPublicIpInstance(asyncRequestInstanceState, CloudStackStateMapper.CREATING_FIREWALL_RULE_STATUS);
    }

    @NotNull
    @VisibleForTesting
    PublicIpInstance buildReadyPublicIpInstance(@NotNull AsyncRequestInstanceState asyncRequestInstanceState) {
        return buildPublicIpInstance(asyncRequestInstanceState, CloudStackStateMapper.READY_STATUS);
    }

    @NotNull
    @VisibleForTesting
    PublicIpInstance buildFailedPublicIpInstance() {
        return buildPublicIpInstance(null, CloudStackStateMapper.FAILURE_STATUS);
    }

    @NotNull
    @VisibleForTesting
    PublicIpInstance buildProcessingPublicIpInstance() {
        return buildPublicIpInstance(null, CloudStackStateMapper.PROCESSING_STATUS);
    }

    @NotNull
    @VisibleForTesting
    PublicIpInstance buildPublicIpInstance(@NotNull AsyncRequestInstanceState asyncRequestInstanceState,
                                           String state) {
        String id = null;
        String ip = null;
        if (asyncRequestInstanceState != null) {
            id = asyncRequestInstanceState.getIpInstanceId();
            ip = asyncRequestInstanceState.getIp();
        }
        return new PublicIpInstance(id, state, ip);
    }

    @NotNull
    @VisibleForTesting
    String requestIpAddressAssociation(String networkId, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        AssociateIpAddressRequest request = new AssociateIpAddressRequest.Builder()
                .networkId(networkId)
                .build(this.cloudStackUrl);

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            AssociateIpAddressAsyncJobIdResponse response = AssociateIpAddressAsyncJobIdResponse
                    .fromJson(jsonResponse);
            return response.getJobId();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    void requestEnableStaticNat(String computeInstanceId,
                                String ipAdressId,
                                @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        EnableStaticNatRequest enableStaticNatRequest = new EnableStaticNatRequest.Builder()
                .ipAddressId(ipAdressId)
                .virtualMachineId(computeInstanceId)
                .build(this.cloudStackUrl);

        URIBuilder uriRequest = enableStaticNatRequest.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    String requestCreateFirewallRule(String ipAdressId, @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                .protocol(DEFAULT_PROTOCOL)
                .startPort(DEFAULT_SSH_PORT)
                .endPort(DEFAULT_SSH_PORT)
                .ipAddressId(ipAdressId)
                .build(this.cloudStackUrl);

        URIBuilder uriRequest = createFirewallRuleRequest.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudUser);
            CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);
            return response.getJobId();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

    // TODO(chico) - This method will be removed after the Cloudstack Security Rule PR is accepted.

    public static String getPublicIpId(String orderId) {
        return asyncRequestInstanceStateMap.get(orderId).getIpInstanceId();
    }
    // TODO(chico) - This method will be removed after the Cloudstack Security Rule PR is accepted.

    public static void setOrderidToInstanceIdMapping(String orderId, String instanceId) {
        AsyncRequestInstanceState currentAsyncRequest = new AsyncRequestInstanceState(AsyncRequestInstanceState.StateType.READY, null, instanceId);
        asyncRequestInstanceStateMap.put(orderId, currentAsyncRequest);
    }
}
