package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin.PUBLIC_IP_RESOURCE;

@PrepareForTest({DatabaseManager.class, CloudStackQueryJobResult.class, CloudStackQueryAsyncJobResponse.class,
        SuccessfulAssociateIpAddressResponse.class})
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
    public void setUp() throws UnexpectedException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.plugin = Mockito.spy(new CloudStackPublicIpPlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.plugin.setAsyncRequestInstanceStateMap(this.asyncRequestInstanceStateMapMockEmpty);

        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        this.testUtils.mockReadOrdersFromDataBase();
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

    // TODO(chico) - Implement
    @Test
    public void testBuildCurrentPublicIpInstanceFailWhenUnexpected() throws FogbowException {
        Assert.fail();
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
