package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, CreateNetworkResponse.class, DatabaseManager.class})
public class CloudStackNetworkPluginTest extends BaseUnitTests {

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_NAME = "fake-name";
    public static final String FAKE_DOMAIN = "fake-domain";
    public static final String FAKE_GATEWAY = "10.0.0.1";
    public static final String FAKE_ADDRESS = "10.0.0.0/24";
    public static final String FAKE_STATE = "ready";

    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final HashMap<String, String> FAKE_COOKIE_HEADER = new HashMap<>();

    public static final CloudStackUser CLOUD_STACK_USER =
            new CloudStackUser(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String CLOUD_NAME = "cloudstack";

    public static final String JSON = "json";
    public static final String RESPONSE_KEY = "response";
    public static final String ID_KEY = "id";

    private static final String BAD_REQUEST_MSG = "Bad Request";

    private String networkOfferingId;
    private String zoneId;
    private String cloudStackUrl;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    private CloudStackNetworkPlugin plugin;
    private CloudStackHttpClient client;
    private Properties properties;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.networkOfferingId = this.properties.getProperty(CloudStackCloudUtils.NETWORK_OFFERING_ID_CONFIG);
        this.zoneId = this.properties.getProperty(CloudStackCloudUtils.ZONE_ID_CONFIG);
        this.cloudStackUrl = this.properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.plugin = Mockito.spy(new CloudStackNetworkPlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.testUtils.mockReadOrdersFromDataBase();
        ignoringCloudStackUrl();
    }

    // test case: get SubnetInfo successfully
    @Test
    public void testGetSubnetInfo() throws InvalidParameterException {
        // set up
        String cidrNotation = "10.10.10.0/24";
        String lowAddressExpected = "10.10.10.1";
        String highAddressExpected = "10.10.10.254";

        // exercise
        SubnetUtils.SubnetInfo subnetInfo = this.plugin.getSubnetInfo(cidrNotation);

        // verify
        Assert.assertEquals(lowAddressExpected, subnetInfo.getLowAddress());
        Assert.assertEquals(highAddressExpected, subnetInfo.getHighAddress());
    }

    // test case: trying get the subnet but the formar is wrong
    @Test
    public void testGetSubnetInfoWrongFormat() throws InvalidParameterException {
        // set up
        String cidrNotation = "wrong";

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(
                String.format(Messages.Exception.INVALID_CIDR, cidrNotation));

        // exercise
        this.plugin.getSubnetInfo(cidrNotation);
    }

    // test case: doRequestInstance successfully
    @Test
    public void testDoRequestInstance() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder().build("");
        String uriRequestExpected = createNetworkRequest.getUriBuilder().toString();

        String responseStr = "anyString";
        Mockito.when(this.client.doGetRequest(Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser)))
                .thenReturn(responseStr);
        PowerMockito.mockStatic(CreateNetworkResponse.class);
        CreateNetworkResponse createNetworkResponseExpected = Mockito.mock(CreateNetworkResponse.class);
        String instanceIdExpected = "instanceID";
        Mockito.when(createNetworkResponseExpected.getId()).thenReturn(instanceIdExpected);
        PowerMockito.when(CreateNetworkResponse.fromJson(responseStr)).thenReturn(createNetworkResponseExpected);

        // exercise
        String instanceId = this.plugin.doRequestInstance(createNetworkRequest, cloudStackUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: doRequestInstance and throw a HttpResponseException
    @Test
    public void testDoRequestInstanceHttpResponseException() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder().build("");
        String uriRequestExpected = createNetworkRequest.getUriBuilder().toString();

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        Mockito.when(this.client.doGetRequest(Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser)))
                .thenThrow(createBadRequestHttpResponse());

        // exercise
        this.plugin.doRequestInstance(createNetworkRequest, cloudStackUser);
    }

    // test case: When calling the requestInstance method with secondary methods mocked,
    // it must verify if the doRequestInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        String cirdExpected = "10.10.10.0/24";
        String nameExpected = "nameExpected";
        String gatewayExpected = "10.10.10.1";
        Mockito.when(networkOrder.getCidr()).thenReturn(cirdExpected);
        Mockito.when(networkOrder.getName()).thenReturn(nameExpected);
        Mockito.when(networkOrder.getGateway()).thenReturn(gatewayExpected);

        SubnetUtils.SubnetInfo subnetInfoExpected = new SubnetUtils(cirdExpected).getInfo();
        String startingIpExpected = subnetInfoExpected.getLowAddress();
        String endingIpExpected = subnetInfoExpected.getHighAddress();
        String netmarkExpected = subnetInfoExpected.getNetmask();
        Mockito.doReturn(subnetInfoExpected).when(this.plugin).getSubnetInfo(Mockito.eq(cirdExpected));

        String instanceIdExpected = "instanceId";
        Mockito.doReturn(instanceIdExpected).when(this.plugin)
                .doRequestInstance(Mockito.any(), Mockito.eq(cloudStackUser));

        CreateNetworkRequest request = new CreateNetworkRequest.Builder()
                .name(nameExpected)
                .displayText(nameExpected)
                .networkOfferingId(this.networkOfferingId)
                .zoneId(this.zoneId)
                .startIp(startingIpExpected)
                .endingIp(endingIpExpected)
                .gateway(gatewayExpected)
                .netmask(netmarkExpected)
                .build(this.cloudStackUrl);

        // exercise
        String instanceId = this.plugin.requestInstance(networkOrder, cloudStackUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
        RequestMatcher<CreateNetworkRequest> matcher = new RequestMatcher.CreateNetwork(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                Mockito.argThat(matcher), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the requestInstance method and occurs an InvalidParameterException
    // in the getSubnetInfo method, it must verify if It has been ended.
    @Test
    public void testRequestInstanceFailWhenThrowExceptionOnGetSubnetInfo() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.doThrow(new InvalidParameterException()).when(this.plugin)
                .getSubnetInfo(Mockito.anyString());

        // exercise
        try {
            this.plugin.requestInstance(networkOrder, cloudStackUser);
            Assert.fail();
        } catch (InvalidParameterException e) {}
        // verify
        Mockito.verify(networkOrder, Mockito.times(TestUtils.NEVER_RUN)).getName();
    }

    @Test
    // test case: when getting a network, the token should be signed and an HTTP GET request should be made
    public void testGettingAValidNetwork() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String command = GetNetworkRequest.LIST_NETWORKS_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command, RESPONSE_KEY, JSON, ID_KEY, FAKE_ID);

        String successfulResponse = generateSuccessfulGetNetworkResponse(FAKE_ID, FAKE_NAME, FAKE_GATEWAY, FAKE_ADDRESS, FAKE_STATE);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedRequestUrl, CLOUD_STACK_USER)).thenReturn(successfulResponse);

        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setInstanceId(FAKE_ID);

        // exercise
        NetworkInstance retrievedInstance = this.plugin.getInstance(networkOrder, CLOUD_STACK_USER);

        // verify
        Assert.assertEquals(FAKE_ID, retrievedInstance.getId());
        Assert.assertEquals(FAKE_ADDRESS, retrievedInstance.getCidr());
        Assert.assertEquals(FAKE_GATEWAY, retrievedInstance.getGateway());
        Assert.assertEquals(FAKE_NAME, retrievedInstance.getName());
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, retrievedInstance.getCloudState());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, CLOUD_STACK_USER);
    }

    // test case: getting a non-existing network should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetNonExistingNetwork() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.deleteInstance(networkOrder, CLOUD_STACK_USER);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class));
        }
    }

    // test case: when deleting a network, the token should be signed and an HTTP GET request should be made
    @Test
    public void testDeleteNetworkSignsTokenBeforeMakingRequest() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String command = DeleteNetworkRequest.DELETE_NETWORK_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command, RESPONSE_KEY, JSON, ID_KEY, FAKE_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setInstanceId(FAKE_ID);

        // exercise
        this.plugin.deleteInstance(networkOrder, CLOUD_STACK_USER);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, CLOUD_STACK_USER);
    }

    // test case: an UnauthorizedRequestException should be thrown when the user tries to delete
    // a network and was not allowed to do that
    @Test(expected = UnauthorizedRequestException.class)
    public void testUnauthorizedExceptionIsThrownWhenOrchestratorForbids() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.deleteInstance(networkOrder, CLOUD_STACK_USER);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class));
        }
    }

    // test case: deleting a non-existing network should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteNonExistingNetwork() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.deleteInstance(networkOrder, CLOUD_STACK_USER);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class));
        }
    }

    private String generateExpectedUrl(String endpoint, String command, String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            // there should be one value for each key
            return null;
        }

        String url = String.format("%s?command=%s", endpoint, command);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            url += String.format("&%s=%s", key, value);
        }

        return url;
    }

    private String generateSuccessfulCreateNetworkResponse(String id) {
        String format = "{\"createnetworkresponse\":{\"network\":{\"id\":\"%s\"}}}";
        return String.format(format, id);
    }

    private String generateSuccessfulGetNetworkResponse(String id, String name, String gateway, String cidr, String state) {
        String format = "{\"listnetworksresponse\":{\"count\":1" +
                ",\"network\":[" +
                "{\"id\":\"%s\"" +
                ",\"name\":\"%s\"" +
                ",\"gateway\":\"%s\"" +
                ",\"cidr\":\"%s\"" +
                ",\"state\":\"%s\"" +
                "}]}}";

        return String.format(format, id, name, gateway, cidr, state);
    }

    private String getBaseEndpointFromCloudStackConf() {
        return this.properties.getProperty(CLOUDSTACK_URL);
    }

    private void ignoringCloudStackUrl() throws InvalidParameterException {
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();
    }

    private HttpResponseException createBadRequestHttpResponse() {
        return new HttpResponseException(HttpStatus.SC_BAD_REQUEST, BAD_REQUEST_MSG);
    }

}
