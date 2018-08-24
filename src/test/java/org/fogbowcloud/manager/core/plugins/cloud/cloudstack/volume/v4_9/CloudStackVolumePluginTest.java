package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import java.io.File;
import java.util.Properties;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class, DeleteVolumeResponse.class,
        GetVolumeResponse.class})
public class CloudStackVolumePluginTest {

    private static final String BASE_ENDPOINT_KEY = "cloudstack_api_url";
    private static final String DEFAULT_REQUEST_FORMAT = "%s?command=%s";
    private static final String DEFAULT_STATE = "Ready";
    private static final String DEFAULT_DISPLAY_TEXT =
            "A description of the error will be shown if the success field is equal to false.";
    private static final String FAKE_ID = "fake-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_USER_ATTRIBUTES = "fake-apikey:fake-secretKey";
    private static final String FIELD_ID = "&id=%s";
    private static final String ONE_GIGABYTES = "1073741824";
    private static final String TEST_PATH = "src/test/resources/private";

    private CloudStackVolumePlugin plugin;
    private HttpRequestClientUtil httpClient;
    private CloudStackToken token;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath(TEST_PATH);
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.httpClient = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin = new CloudStackVolumePlugin();
        this.plugin.setClient(this.httpClient);
        this.token = new CloudStackToken(FAKE_USER_ATTRIBUTES);
    }

    // test case: When calling the getInstance method, an HTTP GET request must be made with a
    // signed token, which returns a response in the JSON format for the retrieval of the
    // VolumeInstance object.
    @Test
    public void testGetInstanceRequestSuccessful()
            throws HttpResponseException, FogbowManagerException, UnexpectedException {

        // set up
        String request = String.format(DEFAULT_REQUEST_FORMAT + FIELD_ID,
                getBaseEndpointFromCloudStackConf(), GetVolumeRequest.LIST_VOLUMES_COMMAND,
                FAKE_ID);

        String id = FAKE_ID;
        String name = FAKE_NAME;
        String size = ONE_GIGABYTES;
        String state = DEFAULT_STATE;
        String volume = getVolumeResponse(id, name, size, state);
        String response = getListVolumesResponse(volume);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.httpClient.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        VolumeInstance recoveredInstance = this.plugin.getInstance(FAKE_ID, this.token);

        // verify
        Assert.assertEquals(id, recoveredInstance.getId());
        Assert.assertEquals(name, recoveredInstance.getName());
        Assert.assertEquals(size, String.valueOf(recoveredInstance.getSize()));

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(request, this.token);
    }

    // test case: When try to get volume that does not exist, an InstanceNotFoundException
    // should be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void test() throws HttpResponseException, FogbowManagerException, UnexpectedException {
        // set up
        String request = String.format(DEFAULT_REQUEST_FORMAT + FIELD_ID,
                getBaseEndpointFromCloudStackConf(), GetVolumeRequest.LIST_VOLUMES_COMMAND,
                FAKE_ID);

        String volume = "";
        String response = getListVolumesResponse(volume);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.httpClient.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(response)).thenCallRealMethod();

        try {
            // exercise
            this.plugin.getInstance(FAKE_ID, this.token);
        } finally {
            // verify
            Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));

            PowerMockito.verifyStatic(GetVolumeResponse.class, VerificationModeFactory.times(1));
            GetVolumeResponse.fromJson(Mockito.eq(response));
        }
    }

    // test case: When calling the deleteInstance method, an HTTP DELETE request must be made with a
    // signed token, which returns a response in the JSON format.
    @Test
    public void testDeleteInstanceRequestSuccessful()
            throws HttpResponseException, FogbowManagerException, UnexpectedException {

        // set up
        String request = String.format(DEFAULT_REQUEST_FORMAT + FIELD_ID,
                getBaseEndpointFromCloudStackConf(), DeleteVolumeRequest.DELETE_VOLUME_COMMAND,
                FAKE_ID);

        boolean success = true;
        String response = getDeleteVolumeResponse(success);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.httpClient.doGetRequest(request, this.token)).thenReturn(response);

        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(response)).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(FAKE_ID, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(request, token);

        PowerMockito.verifyStatic(DeleteVolumeResponse.class, VerificationModeFactory.times(1));
        DeleteVolumeResponse.fromJson(Mockito.eq(response));
    }

    // test case: When calling the deleteInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testDeleteInstanceThrowUnauthorizedRequestException()
            throws UnexpectedException, FogbowManagerException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.httpClient.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, this.token);
        } finally {
            // verify
            Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }

    // test case: When try to delete a volume that does not exist, an InstanceNotFoundException
    // should be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteInstanceThrowInstanceNotFoundException()
            throws HttpResponseException, FogbowManagerException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.httpClient.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, this.token);
        } finally {
            // verify
            Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }

    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getInstance().getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(BASE_ENDPOINT_KEY);
    }

    private String getListVolumesResponse(String volume) {
        String response = "{\"listvolumesresponse\":{\"volume\":[%s]}}";

        return String.format(response, volume);
    }

    private String getVolumeResponse(String id, String name, String size, String state) {
        String response = "{\"id\":\"%s\"," + "\"name\":\"%s\"," + "\"size\":\"%s\","
                + "\"state\":\"%s\"" + "}";

        return String.format(response, id, name, size, state);
    }

    private String getDeleteVolumeResponse(boolean success) {
        String value = String.valueOf(success);
        String response = "{\"deletevolumeresponse\":{" + "\"displaytext\": \"%s\","
                + "\"success\": \"%s\"" + "}}";

        return String.format(response, DEFAULT_DISPLAY_TEXT, value);
    }

}
