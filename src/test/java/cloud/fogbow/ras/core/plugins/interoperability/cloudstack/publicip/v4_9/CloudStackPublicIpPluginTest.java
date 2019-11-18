package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
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
import org.apache.http.client.HttpResponseException;
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

    private final int FIRST_POSITION = 1;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(CloudStackPublicIpPlugin.class);

    private Map<String, AsyncRequestInstanceState> asyncRequestInstanceStateMapMockEmpty = new HashMap<>();
    private CloudStackPublicIpPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String cloudStackUrl;

    @Before
    public void setUp() throws UnexpectedException, InvalidParameterException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.plugin = Mockito.spy(new CloudStackPublicIpPlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.plugin.setAsyncRequestInstanceStateMap(this.asyncRequestInstanceStateMapMockEmpty);

        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the requestIpAddressAssociation method and occurs a HttpResponseException,
    // it must verify if It throws a FogbowException.
    @Test
    public void testRequestIpAddressAssociationFail() throws FogbowException, HttpResponseException {
        // set up
        AssociateIpAddressRequest request = new AssociateIpAddressRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

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
        AssociateIpAddressRequest request = new AssociateIpAddressRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String jsonResponse = "anything";
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn(jsonResponse);

        String jobIdExpected = "jobId";
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
        DisassociateIpAddressRequest request = new DisassociateIpAddressRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

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
        DisassociateIpAddressRequest request = new DisassociateIpAddressRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn("");

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
        this.asyncRequestInstanceStateMapMockEmpty.put(instanceId, asyncRequestInstanceStateReady);

        Mockito.doThrow(new FogbowException()).when(this.plugin).
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

        this.asyncRequestInstanceStateMapMockEmpty = new HashMap<>();

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
        this.asyncRequestInstanceStateMapMockEmpty.put(instanceId, asyncRequestInstanceStateReady);

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

        // verify before
        Assert.assertNull(asyncRequestInstanceState.getState());

        // exercise
        this.plugin.finishAsyncRequestInstanceSteps(asyncRequestInstanceState);

        // verify after
        Assert.assertEquals(AsyncRequestInstanceState.StateType.READY, asyncRequestInstanceState.getState());
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
    }

    // test case: When calling the requestCreateFirewallRule method and occurs a HttpResponseException,
    // it must verify if It throws FogbowException.
    @Test
    public void testRequestCreateFirewallRuleFail() throws FogbowException, HttpResponseException {
        // set up
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

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
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().build("");

        String jsonResponse = "";
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
        EnableStaticNatRequest request = new EnableStaticNatRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.any(), Mockito.any(), Mockito.any())).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

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
        EnableStaticNatRequest request = new EnableStaticNatRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("");

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
        PublicIpInstance publicIpInstance = this.plugin.buildProcessingPublicIpInstance();

        // verify
        Assert.assertEquals(CloudStackStateMapper.PROCESSING_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the buildFailedPublicIpInstance method, it must verify if It
    // returns a right publicIpInstance.
    @Test
    public void testBuildFailedPublicIpInstanceSuccessfully() {
        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildFailedPublicIpInstance();

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the buildReadyPublicIpInstance method, it must verify if It
    // returns a right publicIpInstance.
    @Test
    public void testBuildReadyPublicIpInstanceSuccessfully() {
        // set up
        String ipExpected = "ip";
        String instanceIdExpected = "instanceId";
        AsyncRequestInstanceState asyncRequestInstanceState = new AsyncRequestInstanceState(null, null, null);
        asyncRequestInstanceState.setIp(ipExpected);
        asyncRequestInstanceState.setIpInstanceId(instanceIdExpected);

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.
                buildReadyPublicIpInstance(asyncRequestInstanceState);

        // verify
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, publicIpInstance.getCloudState());
        Assert.assertEquals(ipExpected, publicIpInstance.getIp());
        Assert.assertEquals(instanceIdExpected, publicIpInstance.getId());
    }

    // test case: When calling the buildCreatingFirewallPublicIpInstance method,
    // it must verify if It returns a right publicIpInstance.
    @Test
    public void testBuildCreatingFirewallPublicIpInstanceSuccessfully() {
        // set up
        String ipExpected = "ip";
        String instanceIdExpected = "instanceId";
        AsyncRequestInstanceState asyncRequestInstanceState = new AsyncRequestInstanceState(null, null, null);
        asyncRequestInstanceState.setIp(ipExpected);
        asyncRequestInstanceState.setIpInstanceId(instanceIdExpected);

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.
                buildCreatingFirewallPublicIpInstance(asyncRequestInstanceState);

        // verify
        Assert.assertEquals(CloudStackStateMapper.CREATING_FIREWALL_RULE_STATUS, publicIpInstance.getCloudState());
        Assert.assertEquals(ipExpected, publicIpInstance.getIp());
        Assert.assertEquals(instanceIdExpected, publicIpInstance.getId());
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

        String jobIdExpected = "jobId";
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
    // and It occurs a FogbowException. it must verify if It throws the same exception.
    @Test
    public void testDoCreatingFirewallOperationFail() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        String jsonResponse = "jsonResponse";

        SuccessfulAssociateIpAddressResponse response = Mockito.mock(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.mockStatic(SuccessfulAssociateIpAddressResponse.class);
        PowerMockito.when(SuccessfulAssociateIpAddressResponse.fromJson(Mockito.eq(jsonResponse))).
                thenReturn(response);

        Mockito.doThrow(new FogbowException()).when(this.plugin).doEnableStaticNat(
                Mockito.eq(response), Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.doCreatingFirewallOperation(asyncRequestInstanceState, this.cloudStackUser, jsonResponse);
    }

    // test case: When calling the doCreatingFirewallOperation method with secondary methods mocked,
    // it must verify if It goes through all methods.
    @Test
    public void testDoCreatingFirewallOperationSuccessfully() throws FogbowException {
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

    // test case: When calling the buildNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is Ready, it must verify if It returns null.
    @Test
    public void testBuildNextOperationPublicIpInstanceFail() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.READY;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doNothing().when(this.plugin).finishAsyncRequestInstanceSteps(
                Mockito.eq(asyncRequestInstanceState));

        PublicIpInstance publicIpInstanceExpected = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).buildReadyPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Assert.assertNull(publicIpInstance);
    }

    // test case: When calling the buildNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is CreatingFirewall, it must verify if It returns the PublicIpInstance by the
    // buildReadyPublicIpInstance method.
    @Test
    public void testBuildNextOperationPublicIpInstanceWhenCreatingFirewallState() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doNothing().when(this.plugin).finishAsyncRequestInstanceSteps(
                Mockito.eq(asyncRequestInstanceState));

        PublicIpInstance publicIpInstanceExpected = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).buildReadyPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Assert.assertEquals(publicIpInstanceExpected, publicIpInstance);
    }

    // test case: When calling the buildNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is AssociateIp but occurs a FogbowException, it must verify if It throws
    // a FogbowException.
    @Test
    public void testBuildNextOperationPublicIpInstanceFailWhenAssociateIpState() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doThrow(new FogbowException()).when(this.plugin).doCreatingFirewallOperation(
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.buildNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);
    }

    // test case: When calling the buildNextOperationPublicIpInstance method with secondary methods mocked
    // and the current state is AssociateIp, it must verify if It returns the PublicIpInstance by the
    // buildCreatingFirewallPublicIpInstance method.
    @Test
    public void testBuildNextOperationPublicIpInstanceWhenAssociateIpState() throws FogbowException {
        // set up
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        AsyncRequestInstanceState.StateType state = AsyncRequestInstanceState.StateType.ASSOCIATING_IP_ADDRESS;
        Mockito.when(asyncRequestInstanceState.getState()).thenReturn(state);
        String jsonResponse = "jsonResponse";

        Mockito.doNothing().when(this.plugin).doCreatingFirewallOperation(
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        PublicIpInstance publicIpInstanceExpected = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).buildCreatingFirewallPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildNextOperationPublicIpInstance(
                asyncRequestInstanceState, this.cloudStackUser, jsonResponse);

        // verify
        Assert.assertEquals(publicIpInstanceExpected, publicIpInstance);
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and job status is unknown, it must verify if It returns null.
    @Test
    public void testBuildCurrentPublicIpInstanceFailWhenUnexpected() throws FogbowException {
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
        PublicIpInstance publicIpInstance = this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertNull(publicIpInstance);
        this.loggerTestChecking.assertEquals(FIRST_POSITION, Level.ERROR, Messages.Error.UNEXPECTED_JOB_STATUS);
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and occurs an exception, it must verify if It throws a FogbowException.
    @Test
    public void testBuildCurrentPublicIpInstanceFail() throws FogbowException {
        // set up
        String jobId = "jobId";
        AsyncRequestInstanceState asyncRequestInstanceState = Mockito.mock(AsyncRequestInstanceState.class);
        Mockito.when(asyncRequestInstanceState.getCurrentJobId()).thenReturn(jobId);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);

        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl),
                Mockito.eq(jobId), Mockito.eq(this.cloudStackUser))).
                thenThrow(new FogbowException());

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready failure but it occurs a FogbowException when try delete the instance,
    // it must verify if It returns the publicIpInstance failure and log the error.
    @Test
    public void testBuildCurrentPublicIpInstanceFailWhenFailureJobStatus() throws FogbowException {
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

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doDeleteInstance(Mockito.any(), Mockito.any());

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
        String errorExpected = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE,
                PUBLIC_IP_RESOURCE, publicIpOrder.getInstanceId());
        this.loggerTestChecking.assertEquals(FIRST_POSITION, Level.ERROR, errorExpected);
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready failure, it must verify if It returns the publicIpInstance failure
    // and delete the instance.
    @Test
    public void testBuildCurrentPublicIpInstanceWhenFailureJobStatus() throws FogbowException {
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
        PublicIpInstance publicIpInstance = this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(
                Mockito.eq(publicIpOrder), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready success, it must verify if It returns the publicIpInstance processing.
    @Test
    public void testBuildCurrentPublicIpInstanceWhenProcessingJobStatus() throws FogbowException {
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
        PublicIpInstance publicIpInstance = this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.PROCESSING_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready success but it occurs a FogbowException, it must verify if It
    // returns the publicIpInstance failure.
    @Test
    public void testBuildCurrentPublicIpInstanceFailWhenSuccessJobStatus() throws FogbowException {
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

        Mockito.doThrow(new FogbowException()).when(this.plugin).buildNextOperationPublicIpInstance(
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
        String errorExpected = Messages.Error.ERROR_WHILE_PROCESSING_ASYNCHRONOUS_REQUEST_INSTANCE_STEP;
        this.loggerTestChecking.assertEquals(FIRST_POSITION, Level.ERROR, errorExpected);
    }

    // test case: When calling the buildCurrentPublicIpInstance method with secondary methods mocked
    // and the job state is ready success, it must verify if It returns the publicIpInstance returned
    // by the buildNextOperationPublicIpInstance.
    @Test
    public void testBuildCurrentPublicIpInstanceWhenSuccessJobStatus() throws FogbowException {
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
        Mockito.doReturn(publicIpInstanceExpected).when(this.plugin).buildNextOperationPublicIpInstance (
                Mockito.eq(asyncRequestInstanceState), Mockito.eq(this.cloudStackUser), Mockito.eq(jsonResponse));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildCurrentPublicIpInstance(
                asyncRequestInstanceState, publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(publicIpInstanceExpected, publicIpInstance);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and the
    // asynchronous request instance is ready, it must verify if It returns the publicIpInstance ready.
    @Test
    public void testDoGetInstanceWhenIsReady() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        AsyncRequestInstanceState asyncRequestInstanceStateReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.READY, null, null);
        this.asyncRequestInstanceStateMapMockEmpty.put(instanceId, asyncRequestInstanceStateReady);

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, publicIpInstance.getCloudState());
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
        this.asyncRequestInstanceStateMapMockEmpty.put(instanceId, asyncRequestInstanceStateNotReady);

        PublicIpInstance publicIpInstanceExcepted = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExcepted).when(this.plugin).buildCurrentPublicIpInstance(
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
        this.asyncRequestInstanceStateMapMockEmpty = new HashMap<>();

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
    }

}
