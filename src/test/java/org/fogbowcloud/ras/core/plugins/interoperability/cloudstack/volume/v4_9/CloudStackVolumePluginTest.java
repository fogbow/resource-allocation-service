package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class, DeleteVolumeResponse.class,
        GetVolumeResponse.class})
public class CloudStackVolumePluginTest {

    private static final String BASE_ENDPOINT_KEY = "cloudstack_api_url";
    private static final String REQUEST_FORMAT = "%s?command=%s";
    private static final String ID_FIELD = "&id=%s";
    private static final String EMPTY_INSTANCE = "";
    private static final String ONE_GIGABYTES = "1073741824";
    private static final String DEFAULT_STATE = "Ready";
    private static final String DEFAULT_DISPLAY_TEXT =
            "A description of the error will be shown if the success field is equal to false.";

    private static final String FAKE_DISK_OFFERING_ID = "fake-disk-offering-id";
    private static final String FAKE_ID = "fake-id";
    private static final String FAKE_JOB_ID = "fake-job-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_USER_ATTRIBUTES = "fake-apikey:fake-secretKey";
    private static final String FAKE_MEMBER = "fake-member";

    private static final String COMMAND_KEY = "command";
    private static final String DISK_OFFERING_ID_KEY = CreateVolumeRequest.DISK_OFFERING_ID;
    private static final String NAME_KEY = CreateVolumeRequest.VOLUME_NAME;
    private static final String SIZE_KEY = CreateVolumeRequest.VOLUME_SIZE;
    private static final String ZONE_ID_KEY = CreateVolumeRequest.ZONE_ID;

    private static final int COMPATIBLE_SIZE = 1;
    private static final int CUSTOMIZED_SIZE = 2;
    private static final int STANDARD_SIZE = 0;

    private CloudStackVolumePlugin plugin;
    private HttpRequestClientUtil client;
    private CloudStackToken token;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin = new CloudStackVolumePlugin();
        this.plugin.setClient(this.client);
        this.token = new CloudStackToken(FAKE_USER_ATTRIBUTES);
    }

    // test case: When calling the requestInstance method with a size compatible with the
    // orchestrator's disk offering, HTTP GET requests must be made with a signed token, one to get
    // the compatible disk offering Id attached to the requisition, and another to create a volume
    // of compatible size, returning the id of the VolumeInstance object.
    @Test
    public void testCreateRequestInstanceSuccessfulWithDiskSizeCompatible()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String request = String.format(urlFormat, baseEndpoint, command);

        String id = FAKE_DISK_OFFERING_ID;
        int diskSize = COMPATIBLE_SIZE;
        boolean customized = false;
        String diskOfferings = getListDiskOfferrings(id, diskSize, customized);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(diskOfferings);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CreateVolumeRequest.CREATE_VOLUME_COMMAND);
        expectedParams.put(ZONE_ID_KEY, this.plugin.getZoneId());
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(DISK_OFFERING_ID_KEY, FAKE_DISK_OFFERING_ID);
        expectedParams.put(SIZE_KEY, ONE_GIGABYTES);

        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, SIZE_KEY);

        String response = getCreateVolumeResponse(FAKE_ID, FAKE_JOB_ID);
        Mockito.when(
                this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(this.token)))
                .thenReturn(response);

        // exercise
        VolumeOrder order =
                new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, COMPATIBLE_SIZE, FAKE_NAME);
        String volumeId = this.plugin.requestInstance(order, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.token));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(this.token));

        String expectedId = FAKE_ID;
        Assert.assertEquals(expectedId, volumeId);
    }

    // test case: When calling the requestInstance method to get a size customized by the
    // orchestrator's disk offering, HTTP GET requests must be made with a signed token, one to get
    // the standard disk offering Id attached to the requisition, and another to create a volume of
    // customized size, returning the id of the VolumeInstance object.
    @Test
    public void testCreateRequestInstanceSuccessfulWithDiskSizeCustomized()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String request = String.format(urlFormat, baseEndpoint, command);

        String id = FAKE_DISK_OFFERING_ID;
        int diskSize = STANDARD_SIZE;
        boolean customized = true;
        String diskOfferings = getListDiskOfferrings(id, diskSize, customized);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(diskOfferings);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CreateVolumeRequest.CREATE_VOLUME_COMMAND);
        expectedParams.put(ZONE_ID_KEY, this.plugin.getZoneId());
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(DISK_OFFERING_ID_KEY, FAKE_DISK_OFFERING_ID);
        expectedParams.put(SIZE_KEY, ONE_GIGABYTES);

        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, SIZE_KEY);

        String response = getCreateVolumeResponse(FAKE_ID, FAKE_JOB_ID);
        Mockito.when(
                this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(this.token)))
                .thenReturn(response);

        // exercise
        VolumeOrder order =
                new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CUSTOMIZED_SIZE, FAKE_NAME);
        String volumeId = this.plugin.requestInstance(order, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.token));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(this.token));

        String expectedId = FAKE_ID;
        Assert.assertEquals(expectedId, volumeId);
    }

    // test case: When calling the requestInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testCreateRequestInstanceThrowUnauthorizedRequestException()
            throws UnexpectedException, FogbowRasException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CUSTOMIZED_SIZE, FAKE_NAME);
            this.plugin.requestInstance(order, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When try to request instance with an ID of the volume that do not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testCreateRequestInstanceThrowNotFoundException()
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
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CUSTOMIZED_SIZE, FAKE_NAME);
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
    public void testCreateRequestInstanceThrowInvalidParameterException()
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
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CUSTOMIZED_SIZE, FAKE_NAME);
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
    public void testCreateRequestInstanceThrowUnauthenticatedUserException()
            throws UnexpectedException, FogbowRasException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CUSTOMIZED_SIZE, FAKE_NAME);
            this.plugin.requestInstance(order, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the getInstance method, an HTTP GET request must be made with a
    // signed token, which returns a response in the JSON format for the retrieval of the
    // VolumeInstance object.
    @Test
    public void testGetInstanceRequestSuccessful()
            throws HttpResponseException, FogbowRasException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        String id = FAKE_ID;
        String request = String.format(urlFormat, baseEndpoint, command, id);

        String name = FAKE_NAME;
        String size = ONE_GIGABYTES;
        String state = DEFAULT_STATE;
        String volume = getVolumeResponse(id, name, size, state);
        String response = getListVolumesResponse(volume);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        VolumeInstance recoveredInstance = this.plugin.getInstance(FAKE_ID, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Assert.assertEquals(id, recoveredInstance.getId());
        Assert.assertEquals(name, recoveredInstance.getName());
        Assert.assertEquals(size, String.valueOf(recoveredInstance.getSize()));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.token);
    }
    
    // test case: When calling the getInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetInstanceThrowUnauthorizedRequestException()
            throws UnexpectedException, FogbowRasException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.getInstance(FAKE_ID, this.token);
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
            this.plugin.getInstance(FAKE_ID, this.token);
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
            throws UnexpectedException, FogbowRasException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            this.plugin.getInstance(FAKE_ID, this.token);
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
            throws UnexpectedException, FogbowRasException, HttpResponseException {

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
            this.plugin.getInstance(FAKE_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }
    
    // test case: When calling the getInstance method and an HTTP GET request returns a failure
    // response in JSON format, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowRasException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        String id = FAKE_ID;
        String request = String.format(urlFormat, baseEndpoint, command, id);

        String volume = EMPTY_INSTANCE;
        String response = getListVolumesResponse(volume);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(response)).thenCallRealMethod();

        try {
            // exercise
            this.plugin.getInstance(FAKE_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));

            PowerMockito.verifyStatic(GetVolumeResponse.class, VerificationModeFactory.times(1));
            GetVolumeResponse.fromJson(Mockito.eq(response));
        }
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

        String urlFormat = REQUEST_FORMAT + ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = DeleteVolumeRequest.DELETE_VOLUME_COMMAND;
        String id = FAKE_ID;
        String request = String.format(urlFormat, baseEndpoint, command, id);

        boolean success = true;
        String response = getDeleteVolumeResponse(success);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(FAKE_ID, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, token);

        PowerMockito.verifyStatic(DeleteVolumeResponse.class, VerificationModeFactory.times(1));
        DeleteVolumeResponse.fromJson(Mockito.eq(response));
    }

    // test case: When calling the deleteInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testDeleteInstanceThrowUnauthorizedRequestException()
            throws UnexpectedException, FogbowRasException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, this.token);
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
            this.plugin.deleteInstance(FAKE_ID, this.token);
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
            throws UnexpectedException, FogbowRasException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, this.token);
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
            throws UnexpectedException, FogbowRasException, HttpResponseException {

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
            this.plugin.deleteInstance(FAKE_ID, this.token);
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

        String urlFormat = REQUEST_FORMAT + ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = DeleteVolumeRequest.DELETE_VOLUME_COMMAND;
        String id = FAKE_ID;
        String request = String.format(urlFormat, baseEndpoint, command, id);

        boolean success = false;
        String response = getDeleteVolumeResponse(success);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(response)).thenCallRealMethod();

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));

            PowerMockito.verifyStatic(DeleteVolumeResponse.class, VerificationModeFactory.times(1));
            DeleteVolumeResponse.fromJson(Mockito.eq(response));
        }
    }
    
    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(BASE_ENDPOINT_KEY);
    }

    private String getListDiskOfferrings(String id, int diskSize, boolean customized) {
        String response = "{\"listdiskofferingsresponse\":{" + "\"diskoffering\":[{"
                + "\"id\": \"%s\","
                + "\"disksize\": %s,"
                + "\"iscustomized\": %s"
                + "}]}}";

        return String.format(response, id, diskSize, customized);
    }

    private String getCreateVolumeResponse(String id, String jobId) {
        String response = "{\"createvolumeresponse\":{"
                + "\"id\": \"%s\", "
                + "\"jobid\": \"%s\""
                + "}}";

        return String.format(response, id, jobId);
    }

    private String getListVolumesResponse(String volume) {
        String response = "{\"listvolumesresponse\":{\"volume\":[%s]}}";

        return String.format(response, volume);
    }

    private String getVolumeResponse(String id, String name, String size, String state) {
        String response = "{\"id\":\"%s\","
                + "\"name\":\"%s\","
                + "\"size\":\"%s\","
                + "\"state\":\"%s\""
                + "}";

        return String.format(response, id, name, size, state);
    }

    private String getDeleteVolumeResponse(boolean success) {
        String value = String.valueOf(success);
        String response = "{\"deletevolumeresponse\":{"
                + "\"displaytext\": \"%s\","
                + "\"success\": \"%s\""
                + "}}";

        return String.format(response, DEFAULT_DISPLAY_TEXT, value);
    }

}
