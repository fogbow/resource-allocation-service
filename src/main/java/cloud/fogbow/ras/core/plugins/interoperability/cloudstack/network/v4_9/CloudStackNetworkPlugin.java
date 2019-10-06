package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
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
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Properties;

public class CloudStackNetworkPlugin implements NetworkPlugin<CloudStackUser> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackNetworkPlugin.class);

    private String networkOfferingId;
    private String cloudStackUrl;
    private String zoneId;
    private CloudStackHttpClient client;
    private Properties properties;

    public CloudStackNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.networkOfferingId = properties.getProperty(CloudStackCloudUtils.NETWORK_OFFERING_ID_CONFIG);
        this.zoneId = properties.getProperty(CloudStackCloudUtils.ZONE_ID_CONFIG);
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
    public String requestInstance(@NotNull NetworkOrder networkOrder,
                                  @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
        SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(networkOrder.getCidr());
        String name = networkOrder.getName();
        String startingIp = subnetInfo.getLowAddress();
        String endingIp = subnetInfo.getHighAddress();
        String gateway = networkOrder.getGateway();

        CreateNetworkRequest request = new CreateNetworkRequest.Builder()
                .name(name)
                .displayText(name)
                .networkOfferingId(this.networkOfferingId)
                .zoneId(this.zoneId)
                .startIp(startingIp)
                .endingIp(endingIp)
                .gateway(gateway)
                .netmask(subnetInfo.getNetmask())
                .build(this.cloudStackUrl);

        return doRequestInstance(request, cloudStackUser);
    }

    @Override
    public NetworkInstance getInstance(@NotNull NetworkOrder networkOrder,
                                       @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, networkOrder.getInstanceId()));
        GetNetworkRequest getNetworkRequest = new GetNetworkRequest.Builder()
                .id(networkOrder.getInstanceId())
                .build(this.cloudStackUrl);

        return doGetInstance(getNetworkRequest, cloudStackUser);
    }

    @Override
    public void deleteInstance(@NotNull NetworkOrder networkOrder,
                               @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, networkOrder.getInstanceId()));
        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder()
                .id(networkOrder.getInstanceId())
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudStackUser);
    }

    @VisibleForTesting
    void doDeleteInstance(@NotNull DeleteNetworkRequest request,
                          @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            CloudStackCloudUtils.doGet(this.client, uriRequest.toString(), cloudStackUser);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    NetworkInstance doGetInstance(@NotNull GetNetworkRequest request,
                                  @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doGet(
                    this.client, uriRequest.toString(), cloudStackUser);

            GetNetworkResponse response = GetNetworkResponse.fromJson(jsonResponse);
            return getNetworkInstance(response);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    String doRequestInstance(@NotNull CreateNetworkRequest createNetworkRequest,
                             @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = createNetworkRequest.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doGet(
                    this.client, uriRequest.toString(), cloudStackUser);
            CreateNetworkResponse response = CreateNetworkResponse.fromJson(jsonResponse);
            return response.getId();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
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
    NetworkInstance getNetworkInstance(@NotNull GetNetworkResponse response)
            throws InstanceNotFoundException {

        List<GetNetworkResponse.Network> networks = response.getNetworks();
        if (networks.isEmpty()) {
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

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

}
