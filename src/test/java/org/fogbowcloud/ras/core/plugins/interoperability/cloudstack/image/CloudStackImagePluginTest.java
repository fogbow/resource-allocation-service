package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.image;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.image.GetAllImagesRequest.EXECUTABLE_TEMPLATES_VALUE;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.image.GetAllImagesRequest.TEMPLATE_FILTER_KEY;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackImagePluginTest {

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String JSON = "json";
    private static final String RESPONSE_KEY = "response";

    public static final CloudStackToken FAKE_TOKEN = new CloudStackToken(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE,
            FAKE_USER_ID, FAKE_USERNAME);

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_NAME = "fake-name";
    public static final long FAKE_SIZE = 1000L;

    private CloudStackImagePlugin plugin;
    private HttpRequestClientUtil client;

    @Before
    public void setUp() {
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.plugin = new CloudStackImagePlugin();

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin.setClient(this.client);
    }

    @Test
    // test case: when getting all templaes, the token should be signed and an HTTP GET request should be made
    public void testGettingAllTemplates() throws FogbowRasException, UnexpectedException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String command = GetAllImagesRequest.LIST_TEMPLATES_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command,
                RESPONSE_KEY, JSON,
                TEMPLATE_FILTER_KEY, EXECUTABLE_TEMPLATES_VALUE);

        List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
        responses.add(new TemplateResponse().id(FAKE_ID).name(FAKE_NAME).size(FAKE_SIZE));
        String successfulResponse = generateSuccessfulListTemplatesResponse(responses);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedRequestUrl, FAKE_TOKEN)).thenReturn(successfulResponse);

        // exercise
        Map<String, String> retrievedImages = this.plugin.getAllImages(FAKE_TOKEN);

        Assert.assertTrue(retrievedImages.values().contains(FAKE_NAME));
        Assert.assertTrue(retrievedImages.keySet().contains(FAKE_ID));

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, FAKE_TOKEN);
    }

    @Test
    // test case: when getting a valid template, besides token being signed and an HTTP GET request being made
    // the returned image attributes should match with the ones provided by the cloud
    public void testGettingExistingTemplate() throws FogbowRasException, UnexpectedException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String command = GetAllImagesRequest.LIST_TEMPLATES_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command,
                RESPONSE_KEY, JSON,
                TEMPLATE_FILTER_KEY, EXECUTABLE_TEMPLATES_VALUE,
                GetAllImagesRequest.ID_KEY, FAKE_ID);

        List<TemplateResponse> responses = new ArrayList<>();
        responses.add(new TemplateResponse().id(FAKE_ID).name(FAKE_NAME).size(FAKE_SIZE));
        String successfulResponse = generateSuccessfulListTemplatesResponse(responses);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedRequestUrl, FAKE_TOKEN)).thenReturn(successfulResponse);

        // exercise
        Image retrievedImage = this.plugin.getImage(FAKE_ID, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, retrievedImage.getId());
        Assert.assertEquals(FAKE_NAME, retrievedImage.getName());
        Assert.assertEquals(FAKE_SIZE, retrievedImage.getSize());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, FAKE_TOKEN);
    }

    // test case: getting a non-existing image should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetNonExistingTemplate() throws FogbowRasException, HttpResponseException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            Image image = this.plugin.getImage("unexisting-id", FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class));
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

    private String generateSuccessfulListTemplatesResponse(List<TemplateResponse> templateResponses) {
        JSONArray responses = new JSONArray();
        for (TemplateResponse response :
                templateResponses) {
            JSONObject templateResponse = new JSONObject();
            templateResponse.put("id", response.id);
            templateResponse.put("name", response.name);
            templateResponse.put("size", response.size);
            responses.put(templateResponse);
        }

        JSONObject listTemplatesResponse = new JSONObject();
        listTemplatesResponse.put("count", templateResponses.size());
        listTemplatesResponse.put("template", responses);

        JSONObject root = new JSONObject();
        root.put("listtemplatesresponse", listTemplatesResponse);

        return root.toString(4);
    }

    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(CloudStackTokenGeneratorPlugin.CLOUDSTACK_URL);
    }

    private static class TemplateResponse {

        private String id;
        private String name;
        private long size;

        public TemplateResponse id(String id) {
            this.id = id;
            return this;
        }

        public TemplateResponse name(String name) {
            this.name = name;
            return this;
        }

        public TemplateResponse size(long size) {
            this.size = size;
            return this;
        }
    }
}
