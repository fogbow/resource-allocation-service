package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
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

import java.io.IOException;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, CreateNetworkResponse.class,
        DatabaseManager.class, GetNetworkResponse.class})
public class CloudStackNetworkPluginTest extends BaseUnitTests {

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_NAME = "fake-name";

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
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
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

    // test case: When calling the getSubnetInfo method with a right parameter,
    // it must verify if It returns the subnetInfo expected.
    @Test
    public void testGetSubnetInfoSuccessfully() throws InvalidParameterException {
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

    // test case: When calling the getSubnetInfo method with a wrong parameter,
    // it must verify if It returns an InvalidParameterException.
    @Test
    public void testGetSubnetInfoFail() throws InvalidParameterException {
        // set up
        String cidrNotation = "wrong";

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(
                String.format(Messages.Exception.INVALID_CIDR, cidrNotation));

        // exercise
        this.plugin.getSubnetInfo(cidrNotation);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked,
    // it must verify if It returns the right instanceId.
    @Test
    public void testDoRequestInstanceSuccefully() throws FogbowException, HttpResponseException {
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

    // test case: When calling the doRequestInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException, HttpResponseException {
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

    // test case: When calling the getInstance method with secondary methods mocked,
    // it must verify if the doGetInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceIdExpected = "instanceId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getInstanceId()).thenReturn(instanceIdExpected);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        NetworkInstance networkInstanceExpected = Mockito.mock(NetworkInstance.class);
        Mockito.doReturn(networkInstanceExpected).when(this.plugin).doGetInstance(
                Mockito.any(GetNetworkRequest.class), Mockito.eq(cloudStackUser));

        GetNetworkRequest request = new GetNetworkRequest.Builder()
                .id(instanceIdExpected)
                .build(this.cloudStackUrl);

        // exercise
        NetworkInstance networkInstance = this.plugin.getInstance(networkOrder, cloudStackUser);

        // verify
        Assert.assertEquals(networkInstanceExpected, networkInstance);
        RequestMatcher<GetNetworkRequest> matcher = new RequestMatcher.GetNetwork(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
                Mockito.argThat(matcher), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the doGetInstance method with secondary methods mocked,
    // it must verify if It returns the right NetworkInstance.
    @Test
    public void testDoGetInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetNetworkRequest request = new GetNetworkRequest.Builder().build("");
        String uriRequestExpected = request.getUriBuilder().toString();

        String responseStr = "anyString";
        Mockito.when(this.client.doGetRequest(Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser)))
                .thenReturn(responseStr);

        GetNetworkResponse response = Mockito.mock(GetNetworkResponse.class);
        PowerMockito.mockStatic(GetNetworkResponse.class);
        PowerMockito.when(GetNetworkResponse.fromJson(Mockito.eq(responseStr))).thenReturn(response);

        NetworkInstance networkInstaceExpexted = Mockito.mock(NetworkInstance.class);
        Mockito.doReturn(networkInstaceExpexted).when(this.plugin).getNetworkInstance(Mockito.eq(response));

        // exercise
        NetworkInstance networkInstance = this.plugin.doGetInstance(request, cloudStackUser);

        // verify
        Assert.assertEquals(networkInstaceExpexted, networkInstance);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoGetInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetNetworkRequest request = new GetNetworkRequest.Builder().build("");
        String uriRequestExpected = request.getUriBuilder().toString();

        String responseStr = "anyString";
        Mockito.when(this.client.doGetRequest(Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser)))
                .thenThrow(createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.doGetInstance(request, cloudStackUser);
    }

    // test case: When calling the getNetworkInstance method with right parameter,
    // it must verify if It returns the right NetworkInstance.
    @Test
    public void testGetNetworkInstanceSuccessfully() throws IOException, InstanceNotFoundException {
        // set up
        String idExpected = "idExpected";
        String nameExpected = "nameExpected";
        String gatewayExpected = "gatewayExpected";
        String cirdExpected = "cirdExpected";
        String stateExpected = "stateExpected";
        String jsonResponse = CloudstackTestUtils.createGetNetworkResponseJson(
                idExpected, nameExpected, gatewayExpected, cirdExpected, stateExpected);
        GetNetworkResponse response = GetNetworkResponse.fromJson(jsonResponse);

        // exercise
        NetworkInstance networkInstance = this.plugin.getNetworkInstance(response);

        // verify
        Assert.assertEquals(idExpected, networkInstance.getId());
        Assert.assertEquals(cirdExpected, networkInstance.getCidr());
        Assert.assertEquals(gatewayExpected, networkInstance.getGateway());
        Assert.assertEquals(NetworkAllocationMode.DYNAMIC, networkInstance.getAllocationMode());
        Assert.assertEquals(stateExpected, networkInstance.getCloudState());
        Assert.assertNull(networkInstance.getvLAN());
        Assert.assertNull(networkInstance.getMACInterface());
        Assert.assertNull(networkInstance.getInterfaceState());
    }

    // test case: When calling the getNetworkInstance method with empty GetNetworkResponse,
    // it must verify if It throws an InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetNetworkInstanceFail() throws IOException, InstanceNotFoundException {
        // set up
        String jsonResponse = CloudstackTestUtils.createGetNetworkEmptyResponseJson();
        GetNetworkResponse response = GetNetworkResponse.fromJson(jsonResponse);

        // exercise
        this.plugin.getNetworkInstance(response);
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
        this.plugin.deleteInstance(networkOrder, CloudstackTestUtils.CLOUD_STACK_USER);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, CloudstackTestUtils.CLOUD_STACK_USER);
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
            this.plugin.deleteInstance(networkOrder, CloudstackTestUtils.CLOUD_STACK_USER);
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
            this.plugin.deleteInstance(networkOrder, CloudstackTestUtils.CLOUD_STACK_USER);
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

    private String getBaseEndpointFromCloudStackConf() {
        return this.properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
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
