package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Properties;

public class CloudStackNetworkPlugin implements NetworkPlugin<CloudStackUser> {

    public static final String NETWORK_OFFERING_ID = "network_offering_id";
    public static final String ZONE_ID = "zone_id";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    protected String networkOfferingId;
    protected String zoneId;
    protected String cloudStackUrl;

    private CloudStackHttpClient client;
    private Properties properties;

    public CloudStackNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
        this.networkOfferingId = properties.getProperty(NETWORK_OFFERING_ID);
        this.zoneId = properties.getProperty(ZONE_ID);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.NETWORK, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.NETWORK, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(@NotNull final NetworkOrder networkOrder,
                                  @NotNull final CloudStackUser cloudStackUser) throws FogbowException {

        SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(networkOrder.getCidr());
        String name = networkOrder.getName();
        String startingIp = subnetInfo.getLowAddress();
        String endingIp = subnetInfo.getHighAddress();
        String gateway = networkOrder.getGateway();

        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                .name(name)
                .displayText(name)
                .networkOfferingId(this.networkOfferingId)
                .zoneId(this.zoneId)
                .startIp(startingIp)
                .endingIp(endingIp)
                .gateway(gateway)
                .netmask(subnetInfo.getNetmask())
                .build(this.cloudStackUrl);
        CreateNetworkResponse createNetworkResponse = doRequestInstance(createNetworkRequest, cloudStackUser);
        return createNetworkResponse.getId();
    }

    @Override
    public NetworkInstance getInstance(@NotNull final NetworkOrder networkOrder,
                                       @NotNull final CloudStackUser cloudStackUser)
            throws FogbowException {

        GetNetworkRequest getNetworkRequest = new GetNetworkRequest.Builder()
                .id(networkOrder.getInstanceId())
                .build(this.cloudStackUrl);

        GetNetworkResponse getNetworkResponse = doGetInstance(getNetworkRequest, cloudStackUser);
        return getNetworkInstance(getNetworkResponse);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, CloudStackUser cloudUser) throws FogbowException {
        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder()
                .id(networkOrder.getInstanceId())
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        try {
            this.client.doGetRequest(request.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    @NotNull
    @VisibleForTesting
    GetNetworkResponse doGetInstance(@NotNull GetNetworkRequest request,
                                     @NotNull final CloudStackUser cloudUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = this.client.doGetRequest(uriRequest.toString(), cloudUser);
            return GetNetworkResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
            return null;
        }
    }

    @NotNull
    @VisibleForTesting
    CreateNetworkResponse doRequestInstance(@NotNull CreateNetworkRequest createNetworkRequest,
                                            @NotNull final CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = createNetworkRequest.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = this.client.doGetRequest(uriRequest.toString(), cloudStackUser);
            return CreateNetworkResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
            return null;
        }
    }

    @VisibleForTesting
    SubnetUtils.SubnetInfo getSubnetInfo(String cidrNotation) throws InvalidParameterException {
        try {
            return new SubnetUtils(cidrNotation).getInfo();
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_CIDR, cidrNotation));
        }
    }

    @NotNull
    @VisibleForTesting
    NetworkInstance getNetworkInstance(GetNetworkResponse getNetworkResponse)
            throws InstanceNotFoundException {

        List<GetNetworkResponse.Network> networks = getNetworkResponse.getNetworks();
        if (networks == null || networks.size() <= 0) {
            throw new InstanceNotFoundException();
        }
        // since an id was specified, there should be no more than one network in the getNetworkResponse
        GetNetworkResponse.Network network = networks.get(0);

        String state = network.getState();
        String networkId = network.getId();
        String label = network.getName();
        String address = network.getCidr();
        String gateway = network.getGateway();
        NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;

        return new NetworkInstance(networkId, state, label, address, gateway, null, allocationMode,
                null, null, null);
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

}
