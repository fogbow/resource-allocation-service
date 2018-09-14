package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class, DetachVolumeResponse.class})
public class CloudStackAttachmentPluginTest {

    private static final String BASE_ENDPOINT_KEY = "cloudstack_api_url";
    private static final String JSON_FORMAT = "json";
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String REQUEST_FORMAT = "%s?command=%s";
    private static final String RESPONSE_FORMAT = "&response=%s";
    private static final String ID_FIELD = "&id=%s";
    private static final String JOB_ID_FIELD = "&jobid=%s";
    private static final String VM_ID_FIELD = "&virtualmachineid=%s";
    private static final String ATTACHMENT_ID_FORMAT = "%s %s";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String FAKE_JOB_ID = "fake-job-id";
    private static final String FAKE_MEMBER = "fake-member";
    private static final String FAKE_VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";
    private static final String VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";
    private static final String ATTACH_VOLUME_RESPONSE_KEY = "attachvolumeresponse";
    private static final String DETACH_VOLUME_RESPONSE_KEY = "detachvolumeresponse";
    private static final String EMPTY_INSTANCE = "";
    private static final String ATTACHING_STATE = String.valueOf(InstanceState.ATTACHING);
    private static final String READY_STATE = String.valueOf(InstanceState.READY);
    private static final String FAILED_STATE = String.valueOf(InstanceState.FAILED);
    private static final int JOB_STATUS_PENDING = 0;
    private static final int JOB_STATUS_COMPLETE = 1;
    private static final int JOB_STATUS_FAILURE = 2;
    private static final int JOB_STATUS_INCONSISTENT = 3;
    private static final int DEVICE_ID = 1;
    
    private CloudStackAttachmentPlugin plugin;
    private HttpRequestClientUtil client;
    private CloudStackToken token;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin = new CloudStackAttachmentPlugin();
        this.plugin.setClient(this.client);
        this.token = new CloudStackToken(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_USERNAME);
    }

    // test case: When calling the requestInstance method a HTTP GET request must be made with a
    // signed token, returning the id of the Attachment.
    @Test
    public void testAttachRequestInstanceSuccessful()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD + VM_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachVolumeRequest.ATTACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, id, virtualMachineId);

        int status = JOB_STATUS_COMPLETE;
        String jobId = FAKE_JOB_ID;
        String attributeKey = ATTACH_VOLUME_RESPONSE_KEY;
        String response = getAttachmentResponse(status, attributeKey, jobId);


        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);

        String volumeId = this.plugin.requestInstance(order, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.token));

        String expectedId = String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
        Assert.assertEquals(expectedId, volumeId);
    }

    // test case: When calling the requestInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testAttachRequestInstanceThrowUnauthorizedRequestException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                    FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);

            this.plugin.requestInstance(order, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When try to request instance with an ID of the volume and an ID of the virtual
    // machine that do not exist, an InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testAttachRequestInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                    FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);

            this.plugin.requestInstance(order, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the requestInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testAttachRequestInstanceThrowUnauthenticatedUserException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                    FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);

            this.plugin.requestInstance(order, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the requestInstance method passing some invalid argument, an
    // InvalidParameterException must be thrown.
    @Test(expected = InvalidParameterException.class)
    public void testAttachRequestInstanceThrowInvalidParameterException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        try {
            // exercise
            AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                    FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);

            this.plugin.requestInstance(order, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the requestInstance method and an HTTP GET request returns a failure
    // response in JSON format, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testAttachRequestInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD + VM_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachVolumeRequest.ATTACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, id, virtualMachineId);

        String response =
                getAttachmentResponse(JOB_STATUS_FAILURE, ATTACH_VOLUME_RESPONSE_KEY, null);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);

        this.plugin.requestInstance(order, this.token);

        PowerMockito.mockStatic(AttachVolumeResponse.class);
        PowerMockito.when(AttachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.token));

        PowerMockito.verifyStatic(AttachVolumeResponse.class, VerificationModeFactory.times(1));
    }
    
 // test case: When calling the getInstance method for a resource created, an HTTP GET request
    // must be made with a signed token, which returns a response in the JSON format for the
    // retrieval of the complete AttachmentInstance object.
    @Test
    public void testGetInstanceRequestSuccessful()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_JOB_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        int deviceId = DEVICE_ID;
        String id = FAKE_VOLUME_ID;
        String virtualMachineId = VIRTUAL_MACHINE_ID;
        String state = READY_STATE;

        int status = JOB_STATUS_COMPLETE;
        String volume = getVolumeResponse(id, deviceId, virtualMachineId, state, jobId);
        String response = getAttachmentJobStatusResponse(status, volume);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        String attachmentInstanceId = String.format(ATTACHMENT_ID_FORMAT, id, jobId);
        AttachmentInstance recoveredInstance =
                this.plugin.getInstance(attachmentInstanceId, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Assert.assertEquals(attachmentInstanceId, recoveredInstance.getId());
        String device = String.valueOf(deviceId);
        Assert.assertEquals(device, recoveredInstance.getDevice());
        Assert.assertEquals(virtualMachineId, String.valueOf(recoveredInstance.getServerId()));
        Assert.assertEquals(state, String.valueOf(recoveredInstance.getState()));
        Assert.assertEquals(id, String.valueOf(recoveredInstance.getVolumeId()));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.token);
    }

    // test case: When calling the getInstance method to a resource in creating, a HTTP GET request
    // must be done with a signed token, which returns a response in the JSON format for the
    // retrieval of the complete AttachmentInstance object with status 'attaching'.
    @Test
    public void testGetInstanceRequestWithJobStatusPending()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_JOB_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        int status = JOB_STATUS_PENDING;
        String volume = EMPTY_INSTANCE;
        String response = getAttachmentJobStatusResponse(status, volume);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        String attachmentInstanceId =
                String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
        AttachmentInstance recoveredInstance =
                this.plugin.getInstance(attachmentInstanceId, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        String state = ATTACHING_STATE;
        Assert.assertEquals(state, String.valueOf(recoveredInstance.getState()));
        Assert.assertEquals(attachmentInstanceId, recoveredInstance.getId());
        Assert.assertNull(recoveredInstance.getDevice());
        Assert.assertNull(recoveredInstance.getServerId());
        Assert.assertNull(recoveredInstance.getVolumeId());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.token);
    }

    // test case: When calling the getInstance method for a resource that is not working, an HTTP
    // GET request must be made with a signed token, which returns a response in the JSON format for
    // retrieving the complete AttachmentInstance object with status 'failed'.
    @Test
    public void testGetInstanceRequestWithJobStatusFailure()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_JOB_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        int status = JOB_STATUS_FAILURE;
        String volume = EMPTY_INSTANCE;
        String response = getAttachmentJobStatusResponse(status, volume);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        String attachmentInstanceId =
                String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
        AttachmentInstance recoveredInstance =
                this.plugin.getInstance(attachmentInstanceId, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        String state = FAILED_STATE;
        Assert.assertEquals(state, String.valueOf(recoveredInstance.getState()));
        Assert.assertEquals(attachmentInstanceId, recoveredInstance.getId());
        Assert.assertNull(recoveredInstance.getDevice());
        Assert.assertNull(recoveredInstance.getServerId());
        Assert.assertNull(recoveredInstance.getVolumeId());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.token);
    }
    
    // test case: When calling the getInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetInstanceThrowUnauthorizedRequestException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            String attachmentInstanceId =
                    String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
            this.plugin.getInstance(attachmentInstanceId, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When try to get an instance with an ID that does not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            String attachmentInstanceId =
                    String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);

            this.plugin.getInstance(attachmentInstanceId, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the getInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetInstanceThrowUnauthenticatedUserException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            String attachmentInstanceId =
                    String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
            this.plugin.getInstance(attachmentInstanceId, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the getInstance method passing some invalid argument, an
    // InvalidParameterException must be thrown.
    @Test(expected = InvalidParameterException.class)
    public void testGetInstanceThrowInvalidParameterException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        try {
            // exercise
            String attachmentInstanceId =
                    String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
            this.plugin.getInstance(attachmentInstanceId, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
 // test case: When calling the getInstance method and an HTTP GET request returns a response in
    // JSON format with a job status not consistent, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + JOB_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachmentJobStatusRequest.QUERY_ASYNC_JOB_RESULT_COMMAND;
        String jobId = FAKE_JOB_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, jobId);

        String response = getAttachmentJobStatusResponse(JOB_STATUS_INCONSISTENT, EMPTY_INSTANCE);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        String attachmentInstanceId =
                String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
        this.plugin.getInstance(attachmentInstanceId, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, token);

        PowerMockito.verifyStatic(DetachVolumeResponse.class, VerificationModeFactory.times(1));
    }
    
    // test case: When calling the deleteInstance method, an HTTP GET request must be made with a
    // signed token, which returns a response in the JSON format.
    @Test
    public void testDeleteInstanceRequestSuccessful()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = DetachVolumeRequest.DETACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, id);

        int status = JOB_STATUS_COMPLETE;
        String jobId = FAKE_JOB_ID;
        String attributeKey = DETACH_VOLUME_RESPONSE_KEY;
        String response = getAttachmentResponse(status, attributeKey, jobId);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(FAKE_VOLUME_ID, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, token);

        PowerMockito.verifyStatic(DetachVolumeResponse.class, VerificationModeFactory.times(1));
        DetachVolumeResponse.fromJson(Mockito.eq(response));
    }
    
    // test case: When calling the deleteInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testDeleteInstanceThrowUnauthorizedRequestException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_VOLUME_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When try to delete an instance with an ID that does not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_VOLUME_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the deleteInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testDeleteInstanceThrowUnauthenticatedUserException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_VOLUME_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the deleteInstance method passing some invalid argument, an
    // InvalidParameterException must be thrown.
    @Test(expected = InvalidParameterException.class)
    public void testDeleteInstanceThrowInvalidParameterException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_VOLUME_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the deleteInstance method and an HTTP GET request returns a failure
    // response in JSON format, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testDeleteInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = DetachVolumeRequest.DETACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, baseEndpoint, command, jsonFormat, id);

        String response =
                getAttachmentResponse(JOB_STATUS_FAILURE, DETACH_VOLUME_RESPONSE_KEY, null);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(FAKE_VOLUME_ID, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, token);

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
        if (status == JOB_STATUS_COMPLETE) {
            responseFormat = "{\"%s\":{" 
                    + "\"jobid\": \"%s\"" 
                    + "}}";

            return String.format(responseFormat, attributeKey, jobId);
        } else {
            responseFormat = "{\"%s\":{}}";
            return String.format(responseFormat, attributeKey);
        }
    }

    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(BASE_ENDPOINT_KEY);
    }

}