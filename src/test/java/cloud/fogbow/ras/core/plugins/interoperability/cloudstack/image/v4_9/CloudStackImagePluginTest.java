package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

@PrepareForTest({CloudStackUrlUtil.class, DatabaseManager.class,
        GetAllImagesResponse.class, CloudStackCloudUtils.class})
public class CloudStackImagePluginTest extends BaseUnitTests {

    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String JSON = "json";
    private static final String RESPONSE_KEY = "response";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String CLOUD_NAME = "cloudstack";
    private static final HashMap<String, String> FAKE_COOKIE_HEADER = new HashMap<>();

    public static final CloudStackUser FAKE_TOKEN =  new CloudStackUser(FAKE_USER_ID, FAKE_USERNAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_NAME = "fake-name";
    public static final long FAKE_SIZE = 1000L;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    private CloudStackImagePlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String cloudStackUrl;

    @Before
    public void setUp() throws UnexpectedException, InvalidParameterException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.plugin = Mockito.spy(new CloudStackImagePlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);

        this.cloudStackUrl = properties.getProperty(CLOUDSTACK_URL);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the getAllImages method with secondary methods mocked,
    // it must verify if the buildImagesSummary is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testGetAllImagesSuccessfully() throws FogbowException {
        // set up
        List<ImageSummary> allImagesExpected = new ArrayList<>();
        Mockito.doReturn(allImagesExpected).when(this.plugin)
                .buildImagesSummary(Mockito.any(), Mockito.eq(this.cloudStackUser));

        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .build(this.cloudStackUrl);

        // exercise
        List<ImageSummary> allImages = this.plugin.getAllImages(this.cloudStackUser);

        // verify
        Assert.assertEquals(allImagesExpected, allImages);
        RequestMatcher<GetAllImagesRequest> matcher = new RequestMatcher.GetAllImages(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildImagesSummary(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the getAllImages method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.
    @Test(expected = FogbowException.class)
    public void testGetAllImagesFail() throws FogbowException {
        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .buildImagesSummary(Mockito.any(), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.getAllImages(this.cloudStackUser);
    }

    // test case: When calling the buildImagesSummary method with secondary methods mocked,
    // it must verify if It returns the list of image summaries.
    @Test
    public void testBuildImagesSummarySuccessfully() throws FogbowException {
        // set up
        GetAllImagesRequest request = Mockito.mock(GetAllImagesRequest.class);
        String idOneExpected = "one";
        String nameOneExpected = "nameOne";
        String idTwoExpected = "two";
        String nameTwoExpected = "nametwo";

        GetAllImagesResponse.Image imageMockedOne = Mockito.mock(GetAllImagesResponse.Image.class);
        Mockito.when(imageMockedOne.getId()).thenReturn(idOneExpected);
        Mockito.when(imageMockedOne.getName()).thenReturn(nameOneExpected);

        GetAllImagesResponse.Image imageMockedTwo = Mockito.mock(GetAllImagesResponse.Image.class);
        Mockito.when(imageMockedTwo.getId()).thenReturn(idTwoExpected);
        Mockito.when(imageMockedTwo.getName()).thenReturn(nameTwoExpected);

        GetAllImagesResponse response = Mockito.mock(GetAllImagesResponse.class);
        List<GetAllImagesResponse.Image> imagesSummaryExpected = new ArrayList<>();
        imagesSummaryExpected.add(imageMockedOne);
        imagesSummaryExpected.add(imageMockedTwo);
        Mockito.when(response.getImages()).thenReturn(imagesSummaryExpected);
        Mockito.doReturn(response).when(this.plugin).doDescribeImagesRequest(
                Mockito.eq(request), Mockito.eq(this.cloudStackUser));

        // exercise
        List<ImageSummary> imagesSummary = this.plugin.buildImagesSummary(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(imagesSummaryExpected.size(), imagesSummary.size());
        ImageSummary firstImageSummary = imagesSummary.get(0);
        Assert.assertEquals(idOneExpected, firstImageSummary.getId());
        Assert.assertEquals(nameOneExpected, firstImageSummary.getName());
        ImageSummary secondImageSummary = imagesSummary.get(1);
        Assert.assertEquals(idTwoExpected, secondImageSummary.getId());
        Assert.assertEquals(nameTwoExpected, secondImageSummary.getName());
    }

    // test case: When calling the buildImagesSummary method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.
    @Test(expected = FogbowException.class)
    public void testBuildImagesSummaryFail() throws FogbowException {
        // set up
        GetAllImagesRequest request = Mockito.mock(GetAllImagesRequest.class);
        Mockito.doThrow(new FogbowException()).when(this.plugin).doDescribeImagesRequest(
                Mockito.eq(request), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.buildImagesSummary(request, this.cloudStackUser);
    }

    // test case: When calling the doDescribeImagesRequest method with secondary methods mocked,
    // it must verify if It returns the right GetAllImagesResponse.
    @Test
    public void testDoDescribeImagesRequestSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        GetAllImagesRequest request = new GetAllImagesRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String responseStr = "anything";
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenReturn(responseStr);

        PowerMockito.mockStatic(GetAllImagesResponse.class);
        GetAllImagesResponse responseExpected = Mockito.mock(GetAllImagesResponse.class);
        PowerMockito.when(GetAllImagesResponse.fromJson(responseStr)).thenReturn(responseExpected);

        // exercise
        GetAllImagesResponse response = this.plugin.doDescribeImagesRequest(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
    }

    // test case: When calling the doDescribeImagesRequest method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoDescribeImagesRequestFail() throws FogbowException, HttpResponseException {
        // set up
        GetAllImagesRequest request = new GetAllImagesRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDescribeImagesRequest(request, this.cloudStackUser);
    }

    @Test
    // test case: when getting a valid template, besides token being signed and an HTTP GET request being made
    // the returned image attributes should match with the ones provided by the cloud
    public void testGettingExistingTemplate() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = this.cloudStackUrl;
        String command = CloudStackConstants.Image.LIST_TEMPLATES_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command,
                RESPONSE_KEY, JSON,
                CloudStackConstants.Image.TEMPLATE_FILTER_KEY_JSON,
                CloudStackConstants.Image.EXECUTABLE_TEMPLATES_VALUE,
                CloudStackConstants.Image.ID_KEY_JSON, FAKE_ID);

        List<TemplateResponse> responses = new ArrayList<>();
        responses.add(new TemplateResponse().id(FAKE_ID).name(FAKE_NAME).size(FAKE_SIZE));
        String successfulResponse = generateSuccessfulListTemplatesResponse(responses);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedRequestUrl, FAKE_TOKEN)).thenReturn(successfulResponse);

        // exercise
        ImageInstance retrievedImageInstance = this.plugin.getImage(FAKE_ID, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, retrievedImageInstance.getId());
        Assert.assertEquals(FAKE_NAME, retrievedImageInstance.getName());
        Assert.assertEquals(FAKE_SIZE, retrievedImageInstance.getSize());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, FAKE_TOKEN);
    }

    // test case: getting a non-existing image should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetNonExistingTemplate() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            ImageInstance imageInstance = this.plugin.getImage("unexisting-id", FAKE_TOKEN);
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
