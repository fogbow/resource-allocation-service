package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
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

    private String cloudStackUrl;
    private final String defaultNetworkId;
    private CloudStackHttpClient client;

    // since the ip creation and association involves multiple synchronous and asynchronous requests,
    // we need to keep track of where we are in the process in order to fulfill the operation.
    private static Map<String, AsyncRequestInstanceStage> asyncRequests = new HashMap<>();

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

    @NotNull
    @VisibleForTesting
    public String doRequestInstance(@NotNull PublicIpOrder publicIpOrder,
                                    @NotNull CloudStackUser cloudStackUser)
        throws FogbowException {

        String jobId = requestIpAddressAssociation(this.defaultNetworkId, cloudStackUser);
        String instanceId = getInstanceId(publicIpOrder);
        String computeId = publicIpOrder.getComputeId();
        initAsyncRequestInstanceFlow(jobId, instanceId, computeId);
        return instanceId;
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

    @VisibleForTesting
    void initAsyncRequestInstanceFlow(String jobId, String instanceId, String computeId) {
        AsyncRequestInstanceStage asyncRequestInstanceStage = new AsyncRequestInstanceStage(
                AsyncRequestInstanceStage.State.ASSOCIATING_IP_ADDRESS, jobId, computeId);
        this.asyncRequests.put(instanceId, asyncRequestInstanceStage);
    }

    @Override
    public PublicIpInstance getInstance(@NotNull PublicIpOrder publicIpOrder, @NotNull CloudStackUser cloudUser)
            throws FogbowException {
        AsyncRequestInstanceStage asyncRequestInstanceStage = this.asyncRequests.get(publicIpOrder.getId());

        PublicIpInstance result;
        if (asyncRequestInstanceStage == null) {
            // This may happen due to a failure in the RAS while this operation was being carried out; since the
            // order was still spawning, the spawning processor will start monitoring this order after the RAS
            // is restarted. Unfortunately, even if the operation succeeded, we cannot retrieve this information
            // and will have to signal that the order has failed.
            result = new PublicIpInstance(null, CloudStackStateMapper.FAILURE_STATUS, null);
        } else if (asyncRequestInstanceStage.getState().equals(AsyncRequestInstanceStage.State.READY)) {
            result = instanceFromCurrentAsyncRequest(asyncRequestInstanceStage, CloudStackStateMapper.READY_STATUS);
        } else {
            result = getCurrentInstance(publicIpOrder, cloudUser);
        }
        return result;
    }

    @Override
    public void deleteInstance(@NotNull PublicIpOrder publicIpOrder, @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        String ipAddressId = this.asyncRequests.get(publicIpOrder.getId()).getIpInstanceId();

        DisassociateIpAddressRequest disassociateIpAddressRequest = new DisassociateIpAddressRequest.Builder()
                .id(ipAddressId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(disassociateIpAddressRequest.getUriBuilder(), cloudUser.getToken());

        try {
            this.client.doGetRequest(disassociateIpAddressRequest.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    public String getPublicIpId(String orderId) {
        return this.asyncRequests.get(orderId).getIpInstanceId();
    }

    private PublicIpInstance getCurrentInstance(@NotNull PublicIpOrder publicIpOrder,
                                                @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        AsyncRequestInstanceStage asyncRequestInstanceStage = this.asyncRequests.get(publicIpOrder.getId());
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(this.client, this.cloudStackUrl,
                asyncRequestInstanceStage.getCurrentJobId(), cloudUser);
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);

        PublicIpInstance result;
        switch (queryAsyncJobResult.getJobStatus()) {
            case CloudStackQueryJobResult.PROCESSING:
                result = new PublicIpInstance(null, CloudStackStateMapper.PROCESSING_STATUS, null);
                break;
            case CloudStackQueryJobResult.SUCCESS:
                switch (asyncRequestInstanceStage.getState()) {
                    case ASSOCIATING_IP_ADDRESS:
                        SuccessfulAssociateIpAddressResponse response = SuccessfulAssociateIpAddressResponse.fromJson(jsonResponse);
                        String ipAddressId = response.getIpAddress().getId();
                        asyncRequestInstanceStage.setIpInstanceId(ipAddressId);
                        asyncRequestInstanceStage.setIp(response.getIpAddress().getIpAddress());

                        enableStaticNat(asyncRequestInstanceStage.getComputeInstanceId(), ipAddressId, cloudUser);

                        String createFirewallRuleJobId = createFirewallRule(ipAddressId, cloudUser);
                        asyncRequestInstanceStage.setCurrentJobId(createFirewallRuleJobId);
                        asyncRequestInstanceStage.setState(AsyncRequestInstanceStage.State.CREATING_FIREWALL_RULE);
                        result = instanceFromCurrentAsyncRequest(asyncRequestInstanceStage, CloudStackStateMapper.ASSOCIATING_IP_ADDRESS_STATUS);
                        break;
                    case CREATING_FIREWALL_RULE:
                        asyncRequestInstanceStage.setState(AsyncRequestInstanceStage.State.READY);
                        result = instanceFromCurrentAsyncRequest(asyncRequestInstanceStage, CloudStackStateMapper.CREATING_FIREWALL_RULE_STATUS);
                        break;
                    default:
                        result = null;
                        break;
                }
                break;
            case CloudStackQueryJobResult.FAILURE:
                // any failure should lead to a disassociation of the ip address
                deleteInstance(publicIpOrder, cloudUser);
                result = new PublicIpInstance(null, CloudStackStateMapper.FAILURE_STATUS, null);
                break;
            default:
                LOGGER.error(Messages.Error.UNEXPECTED_JOB_STATUS);
                result = null;
                break;
        }
        return result;
    }

    private PublicIpInstance instanceFromCurrentAsyncRequest(@NotNull AsyncRequestInstanceStage asyncRequestInstanceStage,
                                                             String state) {

        String id = asyncRequestInstanceStage == null ? null : asyncRequestInstanceStage.getIpInstanceId();
        String ip = asyncRequestInstanceStage == null ? null : asyncRequestInstanceStage.getIp();

        PublicIpInstance publicIpInstance = new PublicIpInstance(id, state, ip);
        return publicIpInstance;
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

    protected void enableStaticNat(String computeInstanceId,
                                   String ipAdressId,
                                   @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        EnableStaticNatRequest enableStaticNatRequest = new EnableStaticNatRequest.Builder()
                .ipAddressId(ipAdressId)
                .virtualMachineId(computeInstanceId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil
                .sign(enableStaticNatRequest.getUriBuilder(), cloudUser.getToken());

        try {
            this.client.doGetRequest(enableStaticNatRequest.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String createFirewallRule(String ipAdressId, @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                .protocol(DEFAULT_PROTOCOL)
                .startPort(DEFAULT_SSH_PORT)
                .endPort(DEFAULT_SSH_PORT)
                .ipAddressId(ipAdressId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(createFirewallRuleRequest.getUriBuilder(), cloudUser.getToken());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(createFirewallRuleRequest.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);
        return response.getJobId();
    }
}
