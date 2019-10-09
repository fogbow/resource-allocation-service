package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, CloudStackUrlUtil.class, DetachVolumeResponse.class,
        DatabaseManager.class, AttachVolumeResponse.class, CloudStackCloudUtils.class})
public class CloudStackAttachmentPluginTest extends BaseUnitTests {

    private static final String JSON_FORMAT = "json";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String REQUEST_FORMAT = "%s?command=%s";
    private static final String RESPONSE_FORMAT = "&response=%s";
    private static final String ID_FIELD = "&id=%s";
    private static final String JOB_ID_FIELD = "&jobid=%s";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String FAKE_VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";

    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_DEVICE = "/dev/sdd";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_ID_PROVIDER = "fake-id-provider";
    private static final String FAKE_PROVIDER = "fake-provider";

    private static final String DETACH_VOLUME_RESPONSE_KEY = "detachvolumeresponse";
    private static final String EMPTY_INSTANCE = "";
    private static final int DEVICE_ID = 1;

    private static final String DEFAULT_COMPUTE_ID = "1";
    private static final String DEFAULT_VOLUME_ID = "2";

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    private CloudStackAttachmentPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private SharedOrderHolders sharedOrderHolders;
    private AttachmentOrder attachmentOrder;
    private AttachmentOrder basicAttachmentOrder;
    private String cloudStackUrl;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackAttachmentPlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);

        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);
        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);
        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());

        this.basicAttachmentOrder = Mockito.spy(
                this.testUtils.createLocalAttachmentOrder(new ComputeOrder(), new VolumeOrder()));
        Mockito.doReturn(DEFAULT_VOLUME_ID).when(this.basicAttachmentOrder).getVolumeId();
        Mockito.doReturn(DEFAULT_COMPUTE_ID).when(this.basicAttachmentOrder).getComputeId();
        this.attachmentOrder = createAttachmentOrder();

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the requestInstance method with secondary methods mocked,
    // it must verify if the doRequestInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        String volumeIdExpected = this.basicAttachmentOrder.getVolumeId();
        String virtualMachineIdExpected = this.basicAttachmentOrder.getComputeId();

        String attachmentIdExpeted = "attachmentIdExpected";
        Mockito.doReturn(attachmentIdExpeted).when(this.plugin).doRequestInstance(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        AttachVolumeRequest request = new AttachVolumeRequest.Builder()
                .id(volumeIdExpected)
                .virtualMachineId(virtualMachineIdExpected)
                .build(this.cloudStackUrl);

        // exercise
        String attachmentId = this.plugin.requestInstance(
                this.basicAttachmentOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(attachmentIdExpeted, attachmentId);
        RequestMatcher<AttachVolumeRequest> matcher = new RequestMatcher.AttachVolume(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the requestInstance method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.
    @Test(expected = FogbowException.class)
    public void testRequestInstanceFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doRequestInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        this.plugin.requestInstance(this.basicAttachmentOrder, this.cloudStackUser);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked,
    // it must verify if It returns the instanceId(jobId).
    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        AttachVolumeRequest request = new AttachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String resposeStr = "anything";
        PowerMockito.when(CloudStackCloudUtils.doGet(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenReturn(resposeStr);

        PowerMockito.mockStatic(AttachVolumeResponse.class);
        String jobIdExpexted = "jobIdExpected";
        AttachVolumeResponse response = Mockito.mock(AttachVolumeResponse.class);
        Mockito.when(response.getJobId()).thenReturn(jobIdExpexted);
        PowerMockito.when(AttachVolumeResponse.fromJson(Mockito.eq(resposeStr))).thenReturn(response);

        // exercise
        String jobId = this.plugin.doRequestInstance(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(jobIdExpexted, jobId);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        AttachVolumeRequest request = new AttachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doGet(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doRequestInstance(request, this.cloudStackUser);
    }

    // test case: When calling the getInstance method for a resource created, an HTTP GET request
    // must be made with a signed cloudUser, which returns a response in the JSON format for the
    // retrieval of the complete AttachmentInstance object.
    @Test
    public void testGetInstanceRequestSuccessful()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = this.cloudStackUrl;
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_INSTANCE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        int deviceId = DEVICE_ID;
        String id = FAKE_VOLUME_ID;
        String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;
        String state = CloudStackStateMapper.READY_STATUS;

        int status = CloudStackCloudUtils.JOB_STATUS_COMPLETE;
        String volume = getVolumeResponse(id, deviceId, virtualMachineId, state, jobId);
        String response = getAttachmentJobStatusResponse(status, volume);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        // exercise
        AttachmentInstance recoveredInstance = this.plugin.getInstance(attachmentOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Assert.assertEquals(jobId, recoveredInstance.getId());
        String device = String.valueOf(deviceId);
        Assert.assertEquals(device, recoveredInstance.getDevice());
        Assert.assertEquals(virtualMachineId, String.valueOf(recoveredInstance.getComputeId()));
        Assert.assertEquals(state, recoveredInstance.getCloudState());
        Assert.assertEquals(id, String.valueOf(recoveredInstance.getVolumeId()));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.cloudStackUser);
    }

    // test case: When calling the getInstance method to a resource in creating, a HTTP GET request
    // must be done with a signed cloudUser, which returns a response in the JSON format for the
    // retrieval of the complete AttachmentInstance object with status 'attaching'.
    @Test
    public void testGetInstanceRequestWithJobStatusPending()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = this.cloudStackUrl;
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_INSTANCE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        int status = CloudStackCloudUtils.JOB_STATUS_PENDING;
        String volume = EMPTY_INSTANCE;
        String response = getAttachmentJobStatusResponse(status, volume);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        // exercise
        AttachmentInstance recoveredInstance = this.plugin.getInstance(attachmentOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Assert.assertEquals(CloudStackStateMapper.PENDING_STATUS, recoveredInstance.getCloudState());
        Assert.assertEquals(FAKE_INSTANCE_ID, recoveredInstance.getId());
        Assert.assertNull(recoveredInstance.getDevice());
        Assert.assertNull(recoveredInstance.getComputeId());
        Assert.assertNull(recoveredInstance.getVolumeId());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.cloudStackUser);
    }

    // test case: When calling the getInstance method for a resource that is not working, an HTTP
    // GET request must be made with a signed cloudUser, which returns a response in the JSON format for
    // retrieving the complete AttachmentInstance object with status 'failed'.
    @Test
    public void testGetInstanceRequestWithJobStatusFailure()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = this.cloudStackUrl;
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_INSTANCE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        int status = CloudStackCloudUtils.JOB_STATUS_FAILURE;
        String volume = EMPTY_INSTANCE;
        String response = getAttachmentJobStatusResponse(status, volume);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        // exercise
        AttachmentInstance recoveredInstance = this.plugin.getInstance(attachmentOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, recoveredInstance.getCloudState());
        Assert.assertEquals(FAKE_INSTANCE_ID, recoveredInstance.getId());
        Assert.assertNull(recoveredInstance.getDevice());
        Assert.assertNull(recoveredInstance.getComputeId());
        Assert.assertNull(recoveredInstance.getVolumeId());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.cloudStackUser);
    }
    
    // test case: When calling the getInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetInstanceThrowUnauthorizedRequestException()
            throws HttpResponseException, FogbowException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.getInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When try to get an instance with an ID that does not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.getInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When calling the getInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetInstanceThrowUnauthenticatedUserException()
            throws HttpResponseException, FogbowException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            this.plugin.getInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When calling the getInstance method passing some invalid argument, an
    // FogbowException must be thrown.
    @Test(expected = FogbowException.class)
    public void testGetInstanceThrowFogbowException()
            throws HttpResponseException, FogbowException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        try {
            // exercise
            this.plugin.getInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
 // test case: When calling the getInstance method and an HTTP GET request returns a response in
    // JSON format with a job status not consistent, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = this.cloudStackUrl;
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_INSTANCE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        String response = getAttachmentJobStatusResponse(
                CloudStackCloudUtils.JOB_STATUS_INCONSISTENT, EMPTY_INSTANCE);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.getInstance(attachmentOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, cloudStackUser);

        PowerMockito.verifyStatic(DetachVolumeResponse.class, VerificationModeFactory.times(1));
    }
    
    // test case: When calling the deleteInstance method, an HTTP GET request must be made with a
    // signed cloudUser, which returns a response in the JSON format.
    @Test
    public void testDeleteInstanceRequestSuccessful()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String baseEndpoint = this.cloudStackUrl;
        String command = DetachVolumeRequest.DETACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, id);

        int status = CloudStackCloudUtils.JOB_STATUS_COMPLETE;
        String jobId = FAKE_INSTANCE_ID;
        String attributeKey = DETACH_VOLUME_RESPONSE_KEY;
        String response = getAttachmentResponse(status, attributeKey, jobId);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(attachmentOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, cloudStackUser);

        PowerMockito.verifyStatic(DetachVolumeResponse.class, VerificationModeFactory.times(1));
        DetachVolumeResponse.fromJson(Mockito.eq(response));
    }
    
    // test case: When calling the deleteInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testDeleteInstanceThrowUnauthorizedRequestException()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.deleteInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When try to delete an instance with an ID that does not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When calling the deleteInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testDeleteInstanceThrowUnauthenticatedUserException()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            this.plugin.deleteInstance(attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When calling the deleteInstance method passing some invalid argument, an
    // FogbowException must be thrown.
    @Test(expected = FogbowException.class)
    public void testDeleteInstanceThrowFogbowException()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        try {
            // exercise
            this.plugin.deleteInstance(this.attachmentOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }
    
    // test case: When calling the deleteInstance method and an HTTP GET request returns a failure
    // response in JSON format, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testDeleteInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String baseEndpoint = this.cloudStackUrl;
        String command = DetachVolumeRequest.DETACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, id);

        String response =
                getAttachmentResponse(CloudStackCloudUtils.JOB_STATUS_FAILURE, DETACH_VOLUME_RESPONSE_KEY, null);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(attachmentOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, cloudStackUser);

        PowerMockito.verifyStatic(DetachVolumeResponse.class, VerificationModeFactory.times(1));
    }
    
    private String getVolumeResponse(String id, int deviceId, String virtualMachineId, String state,
            String jobId) {
        String responseFormat = "\"volume\": {"
                + " \"id\": \"%s\"," 
                + " \"deviceid\": %s,"
                + " \"virtualmachineid\": \"%s\"," 
                + " \"state\": \"%s\"," 
                + " \"jobid\": \"%s\"}";

        return String.format(responseFormat, id, deviceId, virtualMachineId, state, jobId);
    }

    private String getAttachmentJobStatusResponse(int status, String volume) {
        String responseFormat = "{\"queryasyncjobresultresponse\": {" 
                + " \"jobstatus\": %s,"
                + " \"jobresult\": {%s}}}";

        return String.format(responseFormat, status, volume);
    }

    private String getAttachmentResponse(int status, String attributeKey, String jobId) {
        String responseFormat;
        if (status == CloudStackCloudUtils.JOB_STATUS_COMPLETE) {
            responseFormat = "{\"%s\":{" 
                    + "\"jobid\": \"%s\"" 
                    + "}}";

            return String.format(responseFormat, attributeKey, jobId);
        } else {
            responseFormat = "{\"%s\":{}}";
            return String.format(responseFormat, attributeKey);
        }
    }

    private AttachmentOrder createAttachmentOrder() {
        String instanceId = FAKE_INSTANCE_ID;
        SystemUser requester = new SystemUser(FAKE_USER_ID, FAKE_NAME, FAKE_ID_PROVIDER);
        ComputeOrder computeOrder = new ComputeOrder();
        VolumeOrder volumeOrder = new VolumeOrder();
        computeOrder.setSystemUser(requester);
        computeOrder.setProvider(FAKE_PROVIDER);
        computeOrder.setCloudName(CloudstackTestUtils.CLOUD_NAME);
        computeOrder.setInstanceId(FAKE_VIRTUAL_MACHINE_ID);
        computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
        volumeOrder.setSystemUser(requester);
        volumeOrder.setProvider(FAKE_PROVIDER);
        volumeOrder.setCloudName(CloudstackTestUtils.CLOUD_NAME);
        volumeOrder.setInstanceId(FAKE_VOLUME_ID);
        volumeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
        this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
        this.sharedOrderHolders.getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);
        AttachmentOrder attachmentOrder = new AttachmentOrder(computeOrder.getId(), volumeOrder.getId(), FAKE_DEVICE);
        attachmentOrder.setInstanceId(instanceId);
        this.sharedOrderHolders.getActiveOrdersMap().put(attachmentOrder.getId(), attachmentOrder);
        return attachmentOrder;
    }
}
