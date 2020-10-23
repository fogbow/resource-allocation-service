package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.network.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.InsertNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.InsertNetworkResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.InsertSubnetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.enums.RoutingMode;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;

import java.util.Properties;

public class GoogleCloudNetworkPlugin implements NetworkPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudNetworkPlugin.class);

    @VisibleForTesting
    static final boolean DEFAULT_AUTO_CREATE_SUBNETWORKS = false;
    @VisibleForTesting
    static final RoutingMode DEFAULT_ROUTING_MODE = RoutingMode.GLOBAL;
    @VisibleForTesting
    static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    @VisibleForTesting
    static final String DEFAULT_NETWORK_CIDR = "10.158.0.0/20";
    @VisibleForTesting
    static final String DEFAULT_SUBNETWORK_REGION = "southamerica-east1";

    private static final String SUBNET_PREFIX = "-subnet";

    private String networkV1ApiEndpoint;
    private GoogleCloudHttpClient client;
    private String[] dnsList;

    public GoogleCloudNetworkPlugin(String confFilePath) throws FatalErrorException{
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV1ApiEndpoint = properties.getProperty(GoogleCloudPluginUtils.NETWORK_URL_KEY) +
                cloud.fogbow.common.constants.GoogleCloudConstants.V1_API_ENDPOINT + cloud.fogbow.common.constants.GoogleCloudConstants.PATH_PROJECT;
        setDNSList(properties);
        initClient();
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        InsertNetworkResponse insertNetworkResponse = insertNetwork(networkOrder.getName(), cloudUser, projectId);
        String insertedNetworkUrl = insertNetworkResponse.getTargetLink();
        insertSubnetwork(cloudUser, networkOrder, insertedNetworkUrl, projectId);
        return null;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        //TODO: implement getInstance
        return null;
    }

    @Override
    public boolean isReady(String instanceState) {
        //TODO: implement isReady
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        //TODO: implement hasFailed
        return false;
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        //TODO: implement deleteInstance
    }
    @VisibleForTesting
    public InsertNetworkResponse insertNetwork(String networkName, GoogleCloudUser cloudUser, String projectId) throws FogbowException{
        InsertNetworkResponse insertNetworkResponse = null;
        InsertNetworkRequest insertNetworkRequest = new InsertNetworkRequest.Builder()
                .name(networkName)
                .autoCreateSubnetworks(DEFAULT_AUTO_CREATE_SUBNETWORKS)
                .routingMode(DEFAULT_ROUTING_MODE)
                .build();

        String endPoint = this.networkV1ApiEndpoint + cloud.fogbow.common.constants.GoogleCloudConstants.LINE_SEPARATOR +
                projectId + cloud.fogbow.common.constants.GoogleCloudConstants.GLOBAL_IP_NETWORK;

        try {
            String response = this.client.doPostRequest(endPoint, insertNetworkRequest.toJson(), cloudUser);
            insertNetworkResponse = InsertNetworkResponse.fromJson(response);
        }catch (JsonSyntaxException e){
            LOGGER.error(Messages.Log.UNABLE_TO_GENERATE_JSON, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_GENERATE_JSON);
        }
        return  insertNetworkResponse;

    }
    @VisibleForTesting
    public void insertSubnetwork(GoogleCloudUser cloudUser, NetworkOrder networkOrder, String insertedNetworkUrl, String projectId) throws FogbowException{

        try {
            String jsonRequest = generateJsonEntityToCreateSubnetwork(insertedNetworkUrl, projectId, networkOrder);
            String endPoint = this.networkV1ApiEndpoint + cloud.fogbow.common.constants.GoogleCloudConstants.LINE_SEPARATOR
                    + projectId + cloud.fogbow.common.constants.GoogleCloudConstants.REGION_ENDPOINT
                    + cloud.fogbow.common.constants.GoogleCloudConstants.LINE_SEPARATOR + DEFAULT_SUBNETWORK_REGION + GoogleCloudConstants.SUBNET_ENDPOINT;
            this.client.doPostRequest(endPoint, jsonRequest, cloudUser);
        }catch (FogbowException fe){
            removeNetwork(insertedNetworkUrl, cloudUser);
            throw fe;
        }
    }
    @VisibleForTesting
    private void removeNetwork(String networkUrl, GoogleCloudUser cloudUser) throws FogbowException{
        this.client.doDeleteRequest(networkUrl, cloudUser);
    }

    private String generateJsonEntityToCreateSubnetwork(String insertedNetworkUrl, String projectId, NetworkOrder networkOrder) {
        String subnetName = networkOrder.getName() + SUBNET_PREFIX;

        String subNetworkCidr = networkOrder.getCidr();
        subNetworkCidr = subNetworkCidr == null ? DEFAULT_NETWORK_CIDR : subNetworkCidr;

        InsertSubnetworkRequest insertSubnetworkRequest = new InsertSubnetworkRequest.Builder()
                .name(subnetName)
                .network(insertedNetworkUrl)
                .ipCidrRange(subNetworkCidr)
                .build();
        return insertSubnetworkRequest.toJson();
    }

    @VisibleForTesting
    public void setDNSList(Properties properties) {
        String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
        if (dnsProperty != null) {
            this.dnsList = dnsProperty.split(",");
        }
    }

    private void initClient(){
        this.client = new GoogleCloudHttpClient();
    }

    @VisibleForTesting
    public void setClient(GoogleCloudHttpClient client){
        this.client = client;
    }

    @VisibleForTesting
    public String[] getDnsList(){
        return this.dnsList;
    }
}
