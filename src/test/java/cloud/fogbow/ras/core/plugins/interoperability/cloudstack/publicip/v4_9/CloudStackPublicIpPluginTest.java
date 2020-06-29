package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9.ListPublicIpAddressRequest;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin.PUBLIC_IP_RESOURCE;

@PrepareForTest({DatabaseManager.class, CloudStackQueryJobResult.class, CloudStackQueryAsyncJobResponse.class,
        SuccessfulAssociateIpAddressResponse.class, CloudStackUrlUtil.class, CloudStackCloudUtils.class,
        CreateFirewallRuleAsyncResponse.class, AssociateIpAddressAsyncJobIdResponse.class})
public class CloudStackPublicIpPluginTest extends BaseUnitTests {

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(CloudStackPublicIpPlugin.class);

    private Map<String, AsyncRequestInstanceState> asyncRequestInstanceStateMapMocked = new HashMap<>();
    private CloudStackPublicIpPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String defaultNetworkId;
    private String cloudStackUrl;

    @Before
    public void setUp() throws InternalServerErrorException, InvalidParameterException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.plugin = Mockito.spy(new CloudStackPublicIpPlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.plugin.setAsyncRequestInstanceStateMap(this.asyncRequestInstanceStateMapMocked);

        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.defaultNetworkId = properties.getProperty(CloudStackCloudUtils.DEFAULT_NETWORK_ID_KEY);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the setAsyncRequestInstanceFirstStep method, it must verify if It
    // sets up a new asyncRequestInstanceState with new values.
    @Test
    public void testSetAsyncRequestInstanceFirstStepSuccessfully() {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceIdExpected = "instanceId";
        String computeIdExpected = "computeId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceIdExpected);
        Mockito.when(publicIpOrder.getComputeId()).thenReturn(computeIdExpected);
        String jobId = "jobId";

        String messageExpected = String.format(Messages.Log.ASYNCHRONOUS_PUBLIC_IP_STATE_S,
                instanceIdExpected, AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS);

        // verify before
        AsyncRequestInstanceState asyncRequestInstanceState = this.asyncRequestInstanceStateMapMocked.get(instanceIdExpected);
        Assert.assertNull(asyncRequestInstanceState);

        // exercise
        this.plugin.setAsyncRequestInstanceFirstStep(jobId, publicIpOrder);

        // verify after
        asyncRequestInstanceState = this.asyncRequestInstanceStateMapMocked.get(instanceIdExpected);
        Assert.assertEquals(AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS,
                asyncRequestInstanceState.getState());
        Assert.assertEquals(jobId, asyncRequestInstanceState.getCurrentJobId());
        Assert.assertEquals(computeIdExpected, asyncRequestInstanceState.getComputeInstanceId());
        Assert.assertEquals(instanceIdExpected, asyncRequestInstanceState.getOrderInstanceId());
        this.loggerTestChecking.assertEqualsInOrder(Level.INFO, messageExpected);
    }


    // test case: When calling the requestInstance method with secondary methods mocked,
    // it must verify if the buildCreateVolumeRequest, doRequestInstance and updateVolumeOrder are called;
    // this includes the checking in the Cloudstack request.
    @Test
    public void testRequestInstance() throws FogbowException {
        // setup
        PublicIpOrder order = Mockito.mock(PublicIpOrder.class);
        CloudStackUser user = Mockito.mock(CloudStackUser.class);

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(order), Mockito.eq(user));

        // exercise
        this.plugin.requestInstance(order, user);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(order), Mockito.eq(user));
    }

    // test case: When calling the doRequestInstance method and occurs any exception,
    // it must verify if It throws the same exception.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException {
        // set up
        String instanceIdExpected = "instanceId";
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceIdExpected);

        Mockito.doThrow(new FogbowException("")).when(this.plugin).
                requestIpAddressAssociation(Mockito.any(), Mockito.any());

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.doRequestInstance(publicIpOrder, this.cloudStackUser);
    }

    // test case: When calling the doRequestInstance method with methods mocked,
    // it must verify if It returns the right instanceId.
    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceIdExpected = "instanceId";
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceIdExpected);

        String jobId = "jobId";
        Mockito.doReturn(jobId).when(this.plugin).
                requestIpAddressAssociation(Mockito.any(), Mockito.any());

        Mockito.doNothing().when(this.plugin).setAsyncRequestInstanceFirstStep(Mockito.any(), Mockito.any());

        AssociateIpAddressRequest request = new AssociateIpAddressRequest.Builder()
                .networkId(this.defaultNetworkId)
                .build(this.cloudStackUrl);

        // exercise
        String instanceId = this.plugin.doRequestInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
        RequestMatcher<AssociateIpAddressRequest> matcher = new RequestMatcher.AssociateIpAddress(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .requestIpAddressAssociation(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the requestIpAddressAssociation method and occurs a HttpResponseException,
    // it must verify if It throws a FogbowException.
    @Test
    public void testRequestIpAddressAssociationFail() throws FogbowException, HttpResponseException {
        // set up
        AssociateIpAddressRequest request = new AssociateIpAddressRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createInvalidParameterException());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestIpAddressAssociation(request, this.cloudStackUser);
    }

    // test case: When calling the requestIpAddressAssociation method, it must verify if It
    //  returns the right jobId.
    @Test
    public void testRequestIpAddressAssociationSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        AssociateIpAddressRequest request = new AssociateIpAddressRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String jsonResponse = TestUtils.ANY_VALUE;
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn(jsonResponse);

        String jobIdExpected = "jobIdExpected";
        AssociateIpAddressAsyncJobIdResponse response = Mockito.mock(AssociateIpAddressAsyncJobIdResponse.class);
        Mockito.when(response.getJobId()).thenReturn(jobIdExpected);
        PowerMockito.mockStatic(AssociateIpAddressAsyncJobIdResponse.class);
        PowerMockito.when(AssociateIpAddressAsyncJobIdResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        // exercise
        String jobId = this.plugin.requestIpAddressAssociation(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(jobIdExpected, jobId);
    }

    // test case: When calling the requestDisassociateIpAddress method and occurs a HttpResponseException,
    // it must verify if It throws a FogbowException.
    @Test
    public void testRequestDisassociateIpAddressFail() throws FogbowException, HttpResponseException {
        // set up
        DisassociateIpAddressRequest request = new DisassociateIpAddressRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createInvalidParameterException());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestDisassociateIpAddress(request, this.cloudStackUser);
    }

    // test case: When calling the requestDisassociateIpAddress method, it must verify if It
    // goes through the method without errors.
    @Test
    public void testRequestDisassociateIpAddressSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        DisassociateIpAddressRequest request = new DisassociateIpAddressRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn(TestUtils.EMPTY_STRING);

        // exercise
        this.plugin.requestDisassociateIpAddress(request, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the doDeleteInstance method and occurs a FogbowException,
    // it must verify if It throws the same error.
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);

        AsyncRequestInstanceState asyncRequestInstanceStateReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.READY, null, null);
        String ipAddressId = "ipAddressId";
        asyncRequestInstanceStateReady.setIpInstanceId(ipAddressId);
        this.asyncRequestInstanceStateMapMocked.put(instanceId, asyncRequestInstanceStateReady);

        Mockito.doThrow(new FogbowException("")).when(this.plugin).
                requestDisassociateIpAddress(Mockito.any(), Mockito.any());

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.doDeleteInstance(publicIpOrder, this.cloudStackUser);
    }

    // test case: When calling the doDeleteInstance method and is not found asynchronous request instance,
    // it must verify if It throws an InstanceNotFoundException.
    @Test
    public void testDoDeleteInstanceFailWhenThereIsNoAsyncRequest() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);

        this.asyncRequestInstanceStateMapMocked = new HashMap<>();

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.plugin.doDeleteInstance(publicIpOrder, this.cloudStackUser);
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked,
    // it must verify if the requestDisassociateIpAddress is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);

        AsyncRequestInstanceState asyncRequestInstanceStateReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.READY, null, null);
        String ipAddressId = "ipAddressId";
        asyncRequestInstanceStateReady.setIpInstanceId(ipAddressId);
        this.asyncRequestInstanceStateMapMocked.put(instanceId, asyncRequestInstanceStateReady);

        Mockito.doNothing().when(this.plugin).requestDisassociateIpAddress(Mockito.any(), Mockito.any());

        DisassociateIpAddressRequest request = new DisassociateIpAddressRequest.Builder()
                .id(ipAddressId)
                .build(this.cloudStackUrl);

        // exercise
        this.plugin.doDeleteInstance(publicIpOrder, this.cloudStackUser);

        // verify
        RequestMatcher<DisassociateIpAddressRequest> matcher = new RequestMatcher.DisassociateIpAddress(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .requestDisassociateIpAddress(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the finishAsyncRequestInstanceSteps method, it must verify if It
    // sets the right values in the AsyncRequestInstanceState.
    @Test
    public void testFinishAsyncRequestInstanceStepsSuccessfully() {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = new AsyncRequestInstanceState(null, null , null);

        String messageExpected = String.format(Messages.Log.ASYNCHRONOUS_PUBLIC_IP_STATE_S,
                asyncRequestInstanceState.getOrderInstanceId(),
                AsyncRequestInstanceState.StateType.READY);

        // verify before
        Assert.assertNull(asyncRequestInstanceState.getState());

        // exercise
        this.plugin.finishAsyncRequestInstanceSteps(asyncRequestInstanceState);

        // verify after
        Assert.assertEquals(AsyncRequestInstanceState.StateType.READY, asyncRequestInstanceState.getState());
        this.loggerTestChecking.assertEqualsInOrder(Level.INFO, messageExpected);
    }

    // test case: When calling the setAsyncRequestInstanceSecondStep method, it must verify if It
    // sets the right values in the AsyncRequestInstanceState.
    @Test
    public void testSetAsyncRequestInstanceSecondStepSuccessfully() {
        // set up
        String ipAddressIdExpected = "ipId";
        String ipExpected = "ip";

        SuccessfulAssociateIpAddressResponse response = Mockito.mock(SuccessfulAssociateIpAddressResponse.class);
        SuccessfulAssociateIpAddressResponse.IpAddress ipAddress =
                Mockito.mock(SuccessfulAssociateIpAddressResponse.IpAddress.class);
        Mockito.when(ipAddress.getId()).thenReturn(ipAddressIdExpected);
        Mockito.when(ipAddress.getIpAddress()).thenReturn(ipExpected);

        Mockito.when(response.getIpAddress()).thenReturn(ipAddress);
        AsyncRequestInstanceState asyncRequestInstanceState = new AsyncRequestInstanceState(null, null , null);
        String createFirewallRuleJobId = "jobId";

        String messageExpexted = String.format(Messages.Log.ASYNCHRONOUS_PUBLIC_IP_STATE_S,
                asyncRequestInstanceState.getOrderInstanceId(),
                AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE);

        // verify before
        Assert.assertNull(asyncRequestInstanceState.getIpInstanceId());
        Assert.assertNull(asyncRequestInstanceState.getIp());
        Assert.assertNull(asyncRequestInstanceState.getState());
        Assert.assertNull(asyncRequestInstanceState.getCurrentJobId());

        // exercise
        this.plugin.setAsyncRequestInstanceSecondStep(response, asyncRequestInstanceState, createFirewallRuleJobId);

        // verify after
        Assert.assertEquals(ipAddressIdExpected, asyncRequestInstanceState.getIpInstanceId());
        Assert.assertEquals(ipExpected, asyncRequestInstanceState.getIp());
        Assert.assertEquals(AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE,
                asyncRequestInstanceState.getState());
        Assert.assertEquals(createFirewallRuleJobId, asyncRequestInstanceState.getCurrentJobId());
        this.loggerTestChecking.assertEqualsInOrder(Level.INFO, messageExpexted);
    }

    // test case: When calling the requestCreateFirewallRule method and occurs a HttpResponseException,
    // it must verify if It throws FogbowException.
    @Test
    public void testRequestCreateFirewallRuleFail() throws FogbowException, HttpResponseException {
        // set up
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createInvalidParameterException());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestCreateFirewallRule(request, this.cloudStackUser);
    }

    // test case: When calling the requestCreateFirewallRule method, it must verify if It
    // returns the right jobId.
    @Test
    public void testRequestCreateFirewallRuleSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        String jsonResponse = TestUtils.ANY_VALUE;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(jsonResponse);

        String jobIdExpexted = "jobId";
        CreateFirewallRuleAsyncResponse response = Mockito.mock(CreateFirewallRuleAsyncResponse.class);
        Mockito.when(response.getJobId()).thenReturn(jobIdExpexted);
        PowerMockito.mockStatic(CreateFirewallRuleAsyncResponse.class);
        PowerMockito.when(CreateFirewallRuleAsyncResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        // exercise
        String jobId = this.plugin.requestCreateFirewallRule(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(jobIdExpexted, jobId);
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the requestEnableStaticNat method and occurs a HttpResponseException,
    // it must verify if It throws FogbowException.
    @Test
    public void testRequestEnableStaticNatFail() throws FogbowException, HttpResponseException {
        // set up
        EnableStaticNatRequest request = new EnableStaticNatRequest.Builder().
                build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createInvalidParameterException());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestEnableStaticNat(request, this.cloudStackUser);
    }

    // test case: When calling the requestEnableStaticNat method, it must verify if It
    // goes through the method without errors.
    @Test
    public void testRequestEnableStaticNatSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        EnableStaticNatRequest request = new EnableStaticNatRequest.Builder().build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(TestUtils.EMPTY_STRING);

        // exercise
        this.plugin.requestEnableStaticNat(request, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the buildProcessingPublicIpInstance method, it must verify if It
    // returns a right publicIpInstance.
    @Test
    public void testBuildProcessingPublicIpInstanceSuccessfully() {
        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createProcessingPublicIpInstance();

        // verify
        Assert.assertEquals(CloudStackStateMapper.PROCESSING_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the createFailedPublicIpInstance method, it must verify if It
    // returns a right publicIpInstance.
    @Test
    public void testCreateFailedPublicIpInstanceSuccessfully() {
        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createFailedPublicIpInstance();

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the createReadyPublicIpInstance method, it must verify if It
    // returns a right publicIpInstance.
    @Test
    public void testCreateReadyPublicIpInstanceSuccessfully() throws FogbowException {
        // set up
        String ipExpected = "ip";
        String instanceIdExpected = "instanceId";
        AsyncRequestInstanceState asyncRequestInstanceState = new AsyncRequestInstanceState(null, null, null);
        asyncRequestInstanceState.setIp(ipExpected);
        asyncRequestInstanceState.setIpInstanceId(instanceIdExpected);

        Mockito.doNothing().when(this.plugin).checkIpAddressExist(Mockito.eq(asyncRequestInstanceState),
                Mockito.eq(this.cloudStackUser));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.
                createReadyPublicIpInstance(asyncRequestInstanceState, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, publicIpInstance.getCloudState());
        Assert.assertEquals(ipExpected, publicIpInstance.getIp());
        Assert.assertEquals(instanceIdExpected, publicIpInstance.getId());
    }

    // test case: When calling the checkIpAddressExist method and the request to
    // obtain the public IP address from its ID returns a null list, it must
    // check if an InstanceNotFoundExeception has been launched.
    @Test
    public void testCheckIpAddressExistFail() throws Exception {
        // set up
        String publicIpAddressId = TestUtils.FAKE_INSTANCE_ID;
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito
                .mock(AsyncRequestInstanceState.class);

        Mockito.when(asyncRequestInstanceState.getIpInstanceId()).thenReturn(publicIpAddressId);

        ListPublicIpAddressRequest request = new ListPublicIpAddressRequest.Builder()
                .build(TestUtils.EMPTY_STRING);

        Mockito.doReturn(request).when(this.plugin).buildPublicIpAddressRequest(publicIpAddressId);

        String jsonResponse = "{\"listpublicipaddressesresponse\":{}}";
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.class, "doRequest", Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))
                .thenReturn(jsonResponse);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.checkIpAddressExist(asyncRequestInstanceState, this.cloudStackUser);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the buildPublicIpAddressRequest method, passing a
    // public IP address ID, it must verify if the expected URL was been
    // returned.
    @Test
    public void testBuildPublicIpAddressRequestSuccessfully() throws FogbowException {
        // set up
        String publicIpAddressId = TestUtils.FAKE_INSTANCE_ID;
        String expected = "https://localhost:8080/client/api"
                +"?command=listPublicIpAddresses"
                + "&response=json"
                + "&id=fake-instance-id";

        // exercise
        ListPublicIpAddressRequest request = this.plugin.buildPublicIpAddressRequest(publicIpAddressId);

        // verify
        Assert.assertEquals(expected, request.getUriBuilder().toString());
    }

    // test case: When calling the doCreateFirewallRule method with secondary methods mocked,
    // it must verify if the requestCreateFirewallRule is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDoCreateFirewallRuleSuccessfully() throws FogbowException {
        // set up
        SuccessfulAssociateIpAddressResponse.IpAddress ipAddress =
                Mockito.mock(SuccessfulAssociateIpAddressResponse.IpAddress.class);
        String ipAddressId = "ipAddressId";
        Mockito.when(ipAddress.getId()).thenReturn(ipAddressId);
        SuccessfulAssociateIpAddressResponse response = Mockito.mock(SuccessfulAssociateIpAddressResponse.class);
        Mockito.when(response.getIpAddress()).thenReturn(ipAddress);

        String jobIdExpected = "jobIdExpected";
        Mockito.doReturn(jobIdExpected).when(this.plugin).requestCreateFirewallRule(Mockito.any(), Mockito.any());

        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder()
                .protocol(CloudStackPublicIpPlugin.DEFAULT_PROTOCOL)
                .startPort(CloudStackPublicIpPlugin.DEFAULT_SSH_PORT)
                .endPort(CloudStackPublicIpPlugin.DEFAULT_SSH_PORT)
                .ipAddressId(ipAddressId)
                .build(this.cloudStackUrl);

        // exercise
        String jobId = this.plugin.doCreateFirewallRule(response, this.cloudStackUser);

        // verify
        Assert.assertEquals(jobIdExpected, jobId);
        RequestMatcher<CreateFirewallRuleRequest> matcher = new RequestMatcher.CreateFirewallRule(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .requestCreateFirewallRule(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the doEnableStaticNat method with secondary methods mocked,
    // it must verify if the doRequestInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDoEnableStaticNatSuccessfully() throws FogbowException {
        // set up
        SuccessfulAssociateIpAddressResponse.IpAddress ipAddress =
                Mockito.mock(SuccessfulAssociateIpAddressResponse.IpAddress.class);
        String ipAddressId = "ipAddressId";
        Mockito.when(ipAddress.getId()).thenReturn(ipAddressId);
        SuccessfulAssociateIpAddressResponse response = Mockito.mock(SuccessfulAssociateIpAddressResponse.class);
        Mockito.when(response.getIpAddress()).thenReturn(ipAddress);

        String computeInstanceId = "computeInstanceId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getComputeInstanceId()).thenReturn(computeInstanceId);

        EnableStaticNatRequest request = new EnableStaticNatRequest.Builder()
                .ipAddressId(ipAddressId)
                .virtualMachineId(computeInstanceId)
                .build(this.cloudStackUrl);

        // exercise
        this.plugin.doEnableStaticNat(response, asyncRequestInstanceState, this.cloudStackUser);

        // verify
        RequestMatcher<EnableStaticNatRequest> matcher = new RequestMatcher.EnableStaticNat(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .requestEnableStaticNat(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the doCreatingFirewallOperation method with secondary methods mocked
    // and It occurs a HttpResponseException. it must verify if It throws a FogbowException.
    @Test
    public void testDoCreatingFirewallOperationFailWhenThrowHttpResponseExeption()
            throws FogbowException, HttpResponseException {

        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        String jsonResponse = "jsonResponse";

        PowerMockito.mockStatic(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.when(SuccessfulAssociateIpAddressResponse.fromJson(Mockito.eq(jsonResponse))).
                thenThrow(CloudstackTestUtils.createInvalidParameterException());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doCreatingFirewallOperation(asyncRequestInstanceState, this.cloudStackUser, jsonResponse);
    }

    // test case: When calling the doCreatingFirewallOperation method with secondary methods mocked
    // and It occurs a FogbowException. it must verify if It throws the same exception.
    @Test
    public void testDoCreatingFirewallOperationFail() throws FogbowException, HttpResponseException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        String jsonResponse = "jsonResponse";

        SuccessfulAssociateIpAddressResponse response = Mockito.mock(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.mockStatic(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.when(SuccessfulAssociateIpAddressResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        Mockito.doThrow(new FogbowException("")).when(this.plugin).doEnableStaticNat(
                Mockito.eq(response), Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.doCreatingFirewallOperation(asyncRequestInstanceState, this.cloudStackUser, jsonResponse);
    }

    // test case: When calling the doCreatingFirewallOperation method with secondary methods mocked,
    // it must verify if It goes through all methods.
    @Test
    public void testDoCreatingFirewallOperationSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        String jsonResponse = "jsonResponse";

        SuccessfulAssociateIpAddressResponse response = Mockito.mock(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.mockStatic(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.when(SuccessfulAssociateIpAddressResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        Mockito.doNothing().when(this.plugin).doEnableStaticNat(
                Mockito.eq(response), Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser));

        String jobId = "jobId";
        Mockito.doReturn(jobId).when(this.plugin).doCreateFirewallRule(
                Mockito.eq(response), Mockito.eq(this.cloudStackUser));

        Mockito.doNothing().when(this.plugin).setAsyncRequestInstanceSecondStep(
                Mockito.any(), Mockito.any(), Mockito.any());

        // exercise
        this.plugin.doCreatingFirewallOperation(asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).setAsyncRequestInstanceSecondStep(
                Mockito.eq(response), Mockito.eq(asyncRequestInstanceState), Mockito.eq(jobId));
    }

    // test case: When calling the createNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is Ready, it must verify if It returns null.
    @Test
    public void testCreateNextOperationPublicIpInstanceFail() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.READY;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doNothing().when(this.plugin).finishAsyncRequestInstanceSteps(
                Mockito.eq(asyncRequestInstanceState));

        PublicIpInstance publicIpInstanceExpected = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).createPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState), Mockito.anyString());

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Assert.assertNull(publicIpInstance);
    }

    // test case: When calling the createNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is CreatingFirewall, it must verify if It returns the PublicIpInstance by the
    // buildReadyPublicIpInstance method.
    @Test
    public void testCreateNextOperationPublicIpInstanceWhenCreatingFirewallState() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doNothing().when(this.plugin).finishAsyncRequestInstanceSteps(
                Mockito.eq(asyncRequestInstanceState));

        PublicIpInstance publicIpInstanceExpected = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).createPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState), Mockito.anyString());

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Assert.assertEquals(publicIpInstanceExpected, publicIpInstance);
    }

    // test case: When calling the createNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is AssociateIp but occurs a FogbowException, it must verify if It throws
    // a FogbowException.
    @Test
    public void testCreateNextOperationPublicIpInstanceFailWhenAssociateIpState() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doThrow(new FogbowException("")).when(this.plugin).doCreatingFirewallOperation(
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.createNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);
    }

    // test case: When calling the createNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is AssociateIp, it must verify if It returns the PublicIpInstance by the
    // buildCreatingFirewallPublicIpInstance method.
    @Test
    public void testCreateNextOperationPublicIpInstanceWhenAssociateIpState() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doNothing().when(this.plugin).doCreatingFirewallOperation(
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        PublicIpInstance publicIpInstanceExpected = new PublicIpInstance(
                asyncRequestInstanceState.getIpInstanceId(),
                CloudStackStateMapper.CREATING_FIREWALL_RULE_STATUS,
                asyncRequestInstanceState.getIp());

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Assert.assertEquals(publicIpInstanceExpected, publicIpInstance);
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and job status is unknown, it must verify if it returns null.
    @Test
    public void testCreateCurrentPublicIpInstanceFailWhenUnexpected() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        String jsonResponse = "jsonResponse";
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenReturn(jsonResponse);

        Integer jobStatusUnknown = -1;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(jobStatusUnknown);
        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertNull(publicIpInstance);
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, Messages.Log.UNEXPECTED_JOB_STATUS);
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and occurs an exception, it must verify if It throws a FogbowException.
    @Test
    public void testCreateCurrentPublicIpInstanceFail() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenThrow(new FogbowException(""));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready failure but it occurs a FogbowException when try delete the instance,
    // it must verify if It returns the publicIpInstance failure and log the error.
    @Test
    public void testCreateCurrentPublicIpInstanceFailWhenFailureJobStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getInstanceId()).thenReturn(instanceId);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        String jsonResponse = "jsonResponse";
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenReturn(jsonResponse);

        Integer jobStatus = CloudStackQueryJobResult.FAILURE;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);
        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        Mockito.doThrow(new FogbowException("")).when(this.plugin)
                .doDeleteInstance(Mockito.any(), Mockito.any());

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
        String errorExpected = String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S,
                PUBLIC_IP_RESOURCE, publicIpOrder.getInstanceId());
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, errorExpected);
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready failure, it must verify if It returns the publicIpInstance failure
    // and delete the instance.
    @Test
    public void testCreateCurrentPublicIpInstanceWhenFailureJobStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        String jsonResponse = "jsonResponse";
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenReturn(jsonResponse);

        Integer jobStatus = CloudStackQueryJobResult.FAILURE;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);
        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(), Mockito.any());

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(
                Mockito.eq(publicIpOrder), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready success, it must verify if It returns the publicIpInstance processing.
    @Test
    public void testCreateCurrentPublicIpInstanceWhenProcessingJobStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        String jsonResponse = "jsonResponse";
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenReturn(jsonResponse);

        Integer jobStatus = CloudStackQueryJobResult.PROCESSING;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);
        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.PROCESSING_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready success but it occurs a FogbowException, it must verify if It
    // returns the publicIpInstance failure.
    @Test
    public void testCreateCurrentPublicIpInstanceFailWhenSuccessJobStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        String jsonResponse = "jsonResponse";
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenReturn(jsonResponse);

        Integer jobStatus = CloudStackQueryJobResult.SUCCESS;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);
        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        Mockito.doThrow(new FogbowException("")).when(this.plugin).createNextOperationPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
        String errorExpected = Messages.Log.ERROR_WHILE_PROCESSING_ASYNCHRONOUS_REQUEST_INSTANCE_STEP;
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, errorExpected);
    }

    // test case: When calling the createCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready success, it must verify if It returns the publicIpInstance returned
    // by the buildNextOperationPublicIpInstance.
    @Test
    public void testCreateCurrentPublicIpInstanceWhenSuccessJobStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        String jsonResponse = "jsonResponse";
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenReturn(jsonResponse);

        Integer jobStatus = CloudStackQueryJobResult.SUCCESS;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);
        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        PublicIpInstance publicIpInstanceExpected = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).createNextOperationPublicIpInstance (
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.createCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(publicIpInstanceExpected, publicIpInstance);
    }

    // test case: When calling the doGetInstance method with secondary methods
    // mocked and the asynchronous request instance is ready, it must verify
    // if the buildPublicIpInstance method was called.
    @Test
    public void testDoGetInstanceWhenIsReady() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        AsyncRequestInstanceState asyncRequestInstanceStateReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.READY, null, null);
        this.asyncRequestInstanceStateMapMocked.put(instanceId, asyncRequestInstanceStateReady);

        PublicIpInstance publicIpInstance = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstance).when(this.plugin)
                .createReadyPublicIpInstance(Mockito.eq(asyncRequestInstanceStateReady),
                Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .createReadyPublicIpInstance(Mockito.eq(asyncRequestInstanceStateReady),
                        Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and the
    // asynchronous request instance is neither ready or failed, it must verify if It returns
    // the current publicIpInstance returned by the buildCurrentPublicIpInstance.
    @Test
    public void testDoGetInstanceWhenIsNotReady() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        AsyncRequestInstanceState asyncRequestInstanceStateNotReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE, null, null);
        this.asyncRequestInstanceStateMapMocked.put(instanceId, asyncRequestInstanceStateNotReady);

        PublicIpInstance publicIpInstanceExcepted = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExcepted).when(this.plugin).createCurrentPublicIpInstance(
                Mockito.eq(asyncRequestInstanceStateNotReady), Mockito.eq(publicIpOrder), Mockito.eq(this.cloudStackUser));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(publicIpInstanceExcepted, publicIpInstance);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and the
    // asynchronous request instance is null because a memory lost, it must verify if It returns
    // the current publicIpInstance failure.
    @Test
    public void testDoGetInstanceFailWhenThereMemoryLost() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        this.asyncRequestInstanceStateMapMocked = new HashMap<>();

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
    }

}
