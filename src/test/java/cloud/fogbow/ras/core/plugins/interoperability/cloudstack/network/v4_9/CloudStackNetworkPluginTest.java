package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.NetworkAllocation;
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
import org.apache.http.client.HttpResponseException;
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

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, CreateNetworkResponse.class, CloudStackCloudUtils.class,
        DatabaseManager.class, GetNetworkResponse.class})
public class CloudStackNetworkPluginTest extends BaseUnitTests {

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    private CloudStackNetworkPlugin plugin;
    private CloudStackHttpClient client;
    private String networkOfferingId;
    private String zoneId;
    private String cloudStackUrl;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.networkOfferingId = properties.getProperty(CloudStackCloudUtils.NETWORK_OFFERING_ID_CONFIG);
        this.zoneId = properties.getProperty(CloudStackCloudUtils.ZONE_ID_CONFIG);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.plugin = Mockito.spy(new CloudStackNetworkPlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
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
    public void testDoRequestInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder().build("");
        String uriRequestExpected = createNetworkRequest.getUriBuilder().toString();

        String responseStr = "anyString";
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser))).thenReturn(responseStr);
        PowerMockito.mockStatic(CreateNetworkResponse.class);
        CreateNetworkResponse createNetworkResponseExpected = Mockito.mock(CreateNetworkResponse.class);
        String instanceIdExpected = "instanceID";
        Mockito.when(createNetworkResponseExpected.getId()).thenReturn(instanceIdExpected);
        PowerMockito.when(CreateNetworkResponse.fromJson(responseStr)).thenReturn(createNetworkResponseExpected);
        NetworkOrder order = Mockito.mock(NetworkOrder.class);

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
        NetworkOrder order = Mockito.mock(NetworkOrder.class);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client), Mockito.eq(uriRequestExpected),
                Mockito.eq(cloudStackUser))).thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

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
        Mockito.doNothing().when(this.plugin).updateNetworkOrder(Mockito.eq(networkOrder));

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
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateNetworkOrder(Mockito.eq(networkOrder));
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
        Mockito.doNothing().when(this.plugin).updateNetworkOrder(networkOrder);

        // verify
        this.expectedException.expect(InvalidParameterException.class);

        // exercise
        try {
            this.plugin.requestInstance(networkOrder, cloudStackUser);
            Assert.fail();
        } finally {
            // verify
            Mockito.verify(networkOrder, Mockito.times(TestUtils.NEVER_RUN)).getName();
        }
    }

    // test case: When calling the updateNetworkOrder method with order and new values,
    // it must verify if It updates the network order.
    @Test
    public void testUpdateNetworkOrder() {
        // setup
        NetworkOrder order = Mockito.mock(NetworkOrder.class);
        final int EXPECTED_INSTANCES = 1;

        Mockito.doCallRealMethod().when(order).setActualAllocation(Mockito.any(NetworkAllocation.class));
        Mockito.doCallRealMethod().when(order).getActualAllocation();

        // exercise
        this.plugin.updateNetworkOrder(order);

        // verify
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).setActualAllocation(Mockito.any(NetworkAllocation.class));
        Assert.assertEquals(EXPECTED_INSTANCES, order.getActualAllocation().getNetworks());
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
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser))).thenReturn(responseStr);

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

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),Mockito.eq(uriRequestExpected),
                Mockito.eq(cloudStackUser))).thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doGetInstance(request, cloudStackUser);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and
    // it occurs an InstanceNotFoundException in the getNetworkInstance method,
    // it must verify if It returns a InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class)
    public void testDoGetInstanceFailWhenOccursInstanceNotFoundException()
            throws FogbowException, HttpResponseException {

        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetNetworkRequest request = new GetNetworkRequest.Builder().build("");
        String uriRequestExpected = request.getUriBuilder().toString();

        String responseStr = "anything";
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(uriRequestExpected), Mockito.eq(cloudStackUser))).thenReturn(responseStr);

        GetNetworkResponse response = Mockito.mock(GetNetworkResponse.class);
        PowerMockito.mockStatic(GetNetworkResponse.class);
        PowerMockito.when(GetNetworkResponse.fromJson(Mockito.eq(responseStr))).thenReturn(response);

        Mockito.doThrow(new InstanceNotFoundException()).when(this.plugin).getNetworkInstance(response);

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

    // test case: When calling the deleteInstance method with secondary methods mocked,
    // it must verify if the doDeleteInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        NetworkOrder networkOrder = this.testUtils.createLocalNetworkOrder();
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder()
                .id(networkOrder.getInstanceId())
                .build(this.cloudStackUrl);

        // exercise
        this.plugin.deleteInstance(networkOrder, cloudStackUser);

        // verify
        RequestMatcher<DeleteNetworkRequest> matcher = new RequestMatcher.DeleteNetwork(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(
                Mockito.argThat(matcher), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the doDeleteInstance method with right parameter,
    // it must verify if It doesn't throw a exception.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder().build("");
        String urlRequest = request.getUriBuilder().toString();

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client), Mockito.eq(urlRequest),
                Mockito.eq(cloudStackUser))).thenReturn("");

        // exercise
        this.plugin.doDeleteInstance(request, cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        CloudStackCloudUtils.doRequest(Mockito.eq(this.client), Mockito.eq(urlRequest), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        DeleteNetworkRequest request = new DeleteNetworkRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDeleteInstance(request, cloudStackUser);
    }

}
