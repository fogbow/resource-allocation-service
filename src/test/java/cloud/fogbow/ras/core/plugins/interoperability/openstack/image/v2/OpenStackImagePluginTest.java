package cloud.fogbow.ras.core.plugins.interoperability.openstack.image.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.HttpErrorConditionToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import com.google.gson.Gson;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.util.*;

@PrepareForTest({GetImageResponse.class, HttpErrorConditionToFogbowExceptionMapper.class, DatabaseManager.class})
public class OpenStackImagePluginTest extends BaseUnitTests{

    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String UNAVAILABLE_STATE = "unavailable";
    private static final String ACTIVE_STATE = "active";
    private static final String PRIVATE_VISIBILITY = "private";
    private static final String PUBLIC_VISIBILITY = "public";
    private static final String FOGBOW_OWNER = "fogbow";
    private static final String UNDEFINED_OWNER = "undefined";

    private OpenStackImagePlugin plugin;
    private OpenStackHttpClient client;
    private Properties properties;
    private OpenStackV3User localUserAttributes;

    @Before
    public void setUp() throws InternalServerErrorException {
        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.plugin = Mockito.spy(new OpenStackImagePlugin(cloudConfPath));
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.properties = Mockito.mock(Properties.class);
        this.localUserAttributes = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        this.plugin.setProperties(this.properties);
        this.plugin.setClient(this.client);
        testUtils.mockReadOrdersFromDataBase();
    }

    //test case: check if getAllImages make the right calls.
    @Test
    public void testGetAllImages() throws FogbowException{
        //setup
        Mockito.doReturn(new ArrayList<>()).when(plugin).getAvailableImages(Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        //exercise
        plugin.getAvailableImages(cloudUser);
        //verify
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).getAvailableImages(Mockito.any());
    }

    //test case: Check if getAllImages is returning all expected images from Json response when the request uses pagination.
    // this test is out of our pattern but it is very important to test the pagination through getAllImages.
    @Test
    public void testGetAllImagesWithPagination() throws FogbowException, HttpResponseException {
        //set up
        String imageGlancev2UrlKey = "image-url-key";
        String nextUrl1 = "next-url1";
        String nextUrl2 = "next-url2";

        String endpoint1 =
                imageGlancev2UrlKey
                        + OpenStackConstants.GLANCE_V2_API_ENDPOINT
                        + OpenStackConstants.ENDPOINT_SEPARATOR
                        + OpenStackConstants.IMAGE_ENDPOINT
                        + OpenStackConstants.QUERY_ACTIVE_IMAGES;

        String endpoint2 =
                imageGlancev2UrlKey
                        + nextUrl1;

        String endpoint3 =
                imageGlancev2UrlKey
                        + nextUrl2;

        List<Map<String, String>> generatedImages1 = generateImages(0, 100);
        List<Map<String, String>> generatedImages2 = generateImages(200, 100);
        List<Map<String, String>> generatedImages3 = generateImages(400, 100);

        String jsonResponse1 = getImagesJsonWithNext(generatedImages1, nextUrl1);
        String jsonResponse2 = getImagesJsonWithNext(generatedImages2, nextUrl2);
        String jsonResponse3 = getImagesJson(generatedImages3);

        Mockito.when(this.properties.getProperty(OpenStackPluginUtils.IMAGE_GLANCE_URL_KEY)).thenReturn(imageGlancev2UrlKey);
        Mockito.when(this.client.doGetRequest(endpoint1, localUserAttributes)).thenReturn(jsonResponse1);
        Mockito.when(this.client.doGetRequest(endpoint2, localUserAttributes)).thenReturn(jsonResponse2);
        Mockito.when(this.client.doGetRequest(endpoint3, localUserAttributes)).thenReturn(jsonResponse3);

        List<ImageSummary> expectedOutput = generateJustPublicAndPrivateImagesFromProject(0, 100);
        expectedOutput.addAll(generateJustPublicAndPrivateImagesFromProject(200, 100));
        expectedOutput.addAll(generateJustPublicAndPrivateImagesFromProject(400, 100));
        expectedOutput.sort(Comparator.comparing(ImageSummary::getId));

        //exercise
        List<ImageSummary> imagePluginOutput = this.plugin.getAllImages(localUserAttributes);
        imagePluginOutput.sort(Comparator.comparing(ImageSummary::getId));

        //verify
        Assert.assertEquals(expectedOutput, imagePluginOutput);
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(endpoint1, localUserAttributes);
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(endpoint2, localUserAttributes);
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(endpoint3, localUserAttributes);
    }

    //test case: test if getImage returns null when the image is unavailable.
    @Test
    public void testGetUnavailableImage() throws FogbowException {
        //setup
        GetImageResponse getImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(getImageResponse.getId()).thenReturn(TestUtils.FAKE_IMAGE_ID);
        Mockito.when(getImageResponse.getStatus()).thenReturn(UNAVAILABLE_STATE);
        Mockito.doReturn(getImageResponse).when(plugin).getImageResponse(Mockito.any(), Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        //exercise
        ImageInstance imageInstance = plugin.getImage(TestUtils.FAKE_IMAGE_ID, cloudUser);
        //verify
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageResponse(Mockito.any(), Mockito.any());
        Assert.assertEquals(null, imageInstance);
    }

    //test case: test if getImage works as expected when the image state is active
    @Test
    public void testGetImage() throws FogbowException{
        //setup
        GetImageResponse getImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(getImageResponse.getId()).thenReturn(TestUtils.FAKE_IMAGE_ID);
        Mockito.when(getImageResponse.getStatus()).thenReturn(ACTIVE_STATE);
        Mockito.doReturn(getImageResponse).when(plugin).getImageResponse(Mockito.any(), Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        //exercise
        ImageInstance imageInstance = plugin.getImage(TestUtils.FAKE_IMAGE_ID, cloudUser);
        //verify
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageResponse(Mockito.any(), Mockito.any());
        Assert.assertNotNull(imageInstance);
    }

    //test case: Test if getImageId calls HttpErrorConditionToFogbowExceptionMapper when a http error occurs.
    @Test
    public void testGetImageResponseWhenHttpError() throws Exception {
        //set up
        UnauthorizedRequestException expectedException = new UnauthorizedRequestException(TestUtils.EMPTY_STRING);
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.anyObject())).thenThrow(expectedException);

        try {
            //exercise
            this.plugin.getImageResponse(TestUtils.FAKE_IMAGE_ID, this.localUserAttributes);
            Assert.fail();
        } catch(UnauthorizedRequestException ex) {
            //verify
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    //test case: test if getImageResponse makes the right calls.
    @Test
    public void testGetImageResponse() throws Exception{
        //setup
        String json = TestUtils.EMPTY_STRING;
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenReturn(json);
        PowerMockito.mockStatic(GetImageResponse.class);
        PowerMockito.when(GetImageResponse.class, "fromJson", Mockito.any()).thenCallRealMethod();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        //exercise
        plugin.getImageResponse(TestUtils.EMPTY_STRING, cloudUser);
        //verify
        Mockito.verify(client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
        PowerMockito.verifyStatic(GetImageResponse.class, VerificationModeFactory.times(1));
        GetImageResponse.fromJson(json);
    }

    //test case: test if the method makes the exepected calls when a http error occurs.
    @Test
    public void testGetImagesResponseWhenHttpError() throws Exception {
        //setup
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        FogbowException expectedException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.anyObject())).thenThrow(expectedException);
        try {
            //exercise
            plugin.getImagesResponse(cloudUser);
            //verify
            Assert.fail();
        } catch (FogbowException ex) {
            //verify
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    //test case: test if the method makes the exepected calls.
    @Test
    public void testGetImagesResponse() throws Exception {
        //setup
        String json = TestUtils.EMPTY_STRING;
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenReturn(json);
        PowerMockito.mockStatic(GetImageResponse.class);
        PowerMockito.when(GetImageResponse.class, "fromJson", Mockito.any()).thenCallRealMethod();
        GetAllImagesResponse getImageResponse = Mockito.mock(GetAllImagesResponse.class);
        Mockito.when(getImageResponse.getImages()).thenReturn(new ArrayList<>());
        Mockito.doReturn(getImageResponse).when(plugin).getAllImagesResponse(Mockito.any());
        Mockito.doNothing().when(plugin).getNextImageListResponseByPagination(Mockito.any(), Mockito.any(), Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        //exercise
        plugin.getImagesResponse(cloudUser);
        //verify
        Mockito.verify(client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).getAllImagesResponse(Mockito.any());
        Mockito.verify(plugin).getNextImageListResponseByPagination(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: tests if the method makes the right calls.
    @Test
    public void testGetNextImageListResponseByPagination() throws Exception{
        //setup
        GetAllImagesResponse getImageResponse = Mockito.mock(GetAllImagesResponse.class);
        List<GetImageResponse> images = new ArrayList<>();
        images.add(new GetImageResponse());
        Mockito.when(getImageResponse.getImages()).thenReturn(images);
        Mockito.when(getImageResponse.getNext()).thenReturn("json");
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenReturn(TestUtils.EMPTY_STRING);
        GetAllImagesResponse getAllImagesResponseAux = Mockito.mock(GetAllImagesResponse.class);
        Mockito.when(getAllImagesResponseAux.getImages()).thenReturn(new ArrayList<>());
        Mockito.doReturn(getAllImagesResponseAux).when(plugin).getAllImagesResponse(Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        //exercise
        plugin.getNextImageListResponseByPagination(cloudUser, getImageResponse, new ArrayList<>());
        //verify
        Mockito.verify(client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).getAllImagesResponse(Mockito.any());
    }

    //test case: check if the method calls the exception mapper when a http error occurs.
    @Test
    public void testGetNextImageListResponseByPaginationWhenHttpError() throws Exception {
        //setup
        GetAllImagesResponse getImageResponse = Mockito.mock(GetAllImagesResponse.class);
        List<GetImageResponse> images = new ArrayList<>();
        images.add(new GetImageResponse());
        Mockito.when(getImageResponse.getImages()).thenReturn(images);
        Mockito.when(getImageResponse.getNext()).thenReturn("json");
        FogbowException expectedException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenThrow(expectedException);
        GetAllImagesResponse getAllImagesResponseAux = Mockito.mock(GetAllImagesResponse.class);
        Mockito.when(getAllImagesResponseAux.getImages()).thenReturn(new ArrayList<>());
        Mockito.doReturn(getAllImagesResponseAux).when(plugin).getAllImagesResponse(Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        try {
            //exercise
            plugin.getNextImageListResponseByPagination(cloudUser, getImageResponse, new ArrayList<>());
        } catch (FogbowException ex) {
            //verify
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(1));
        }
    }

    //test case: test if the method returns only public images
    @Test
    public void testGetPublicImagesResponse() {
        //setup
        GetImageResponse firstPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse privateImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(firstPublicImageResponse.getVisibility()).thenReturn("public");
        Mockito.when(secondPublicImageResponse.getVisibility()).thenReturn("public");
        Mockito.when(privateImageResponse.getVisibility()).thenReturn("private");
        List<GetImageResponse> images = new ArrayList<>();
        images.add(firstPublicImageResponse);
        images.add(secondPublicImageResponse);
        images.add(privateImageResponse);
        //exercise
        List<GetImageResponse> allowedImages = plugin.getPublicImagesResponse(images);
        //verify
        Assert.assertTrue(allowedImages.stream().allMatch(each -> each.getVisibility().equalsIgnoreCase("public")));
    }

    //test case: test if the method return only private images.
    @Test
    public void testGetPrivateImagesResponse() {
        //setup
        GetImageResponse firstPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse firstPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(firstPublicImageResponse.getVisibility()).thenReturn(PUBLIC_VISIBILITY);
        Mockito.when(secondPublicImageResponse.getVisibility()).thenReturn(PUBLIC_VISIBILITY);
        Mockito.when(firstPrivateImageResponse.getVisibility()).thenReturn(PRIVATE_VISIBILITY);
        Mockito.when(secondPrivateImageResponse.getVisibility()).thenReturn(PRIVATE_VISIBILITY);
        Mockito.when(firstPrivateImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(secondPrivateImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(firstPublicImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(secondPublicImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        List<GetImageResponse> images = new ArrayList<>();
        images.add(firstPublicImageResponse);
        images.add(secondPublicImageResponse);
        images.add(firstPrivateImageResponse);
        images.add(secondPrivateImageResponse);
        //exercise
        List<GetImageResponse> filteredImages = plugin.getPrivateImagesResponse(images, FOGBOW_OWNER);
        //verify
        Assert.assertTrue(filteredImages.stream().allMatch(each -> each.getVisibility().equalsIgnoreCase(PRIVATE_VISIBILITY)));
    }

    //test case: test if only available images are returned from the method.
    @Test
    public void testGetAvailableImages() throws FogbowException{
        //setup
        GetImageResponse firstPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse firstPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse anyImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(firstPublicImageResponse.getVisibility()).thenReturn(PUBLIC_VISIBILITY);
        Mockito.when(secondPublicImageResponse.getVisibility()).thenReturn(PUBLIC_VISIBILITY);
        Mockito.when(firstPrivateImageResponse.getVisibility()).thenReturn(PRIVATE_VISIBILITY);
        Mockito.when(secondPrivateImageResponse.getVisibility()).thenReturn(PRIVATE_VISIBILITY);
        Mockito.when(anyImageResponse.getVisibility()).thenReturn(UNDEFINED_OWNER);
        Mockito.when(firstPrivateImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(secondPrivateImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(firstPublicImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(secondPublicImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        Mockito.when(anyImageResponse.getOwner()).thenReturn(FOGBOW_OWNER);
        List<GetImageResponse> images = new ArrayList<>();
        images.add(firstPublicImageResponse);
        images.add(secondPublicImageResponse);
        images.add(firstPrivateImageResponse);
        images.add(secondPrivateImageResponse);
        images.add(anyImageResponse);
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        Mockito.when(cloudUser.getProjectId()).thenReturn(FOGBOW_OWNER);
        Mockito.doReturn(images).when(plugin).getImagesResponse(Mockito.any());
        //exercise
        List<ImageSummary> availableImages = plugin.getAvailableImages(cloudUser);
        //verify
        Assert.assertFalse(availableImages.contains(anyImageResponse));
        Assert.assertEquals(4, availableImages.size());
    }

    private String getImagesJson(List<Map<String, String>> imagesList) {
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("images", imagesList);
        Gson gson = new Gson();
        return gson.toJson(jsonMap);
    }

    private String getImagesJsonWithNext(List<Map<String, String>> imagesList, String nextValue) {
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("images", imagesList);
        jsonMap.put("next", nextValue);
        Gson gson = new Gson();
        return gson.toJson(jsonMap);
    }

    private List<Map<String, String>> generateImages(int startId, int qtdImages) {
        String projectId2 = FAKE_PROJECT_ID + "2";
        List<Map<String, String>> myList = new ArrayList<Map<String, String>>();

        qtdImages /= 2;

        for (int i = 0; i < qtdImages; i++) {
            Map<String, String> image = new HashMap<String, String>();
            image.put("visibility", i % 2 == 0 ? "public" : "private");
            image.put("owner", i < qtdImages / 2 ? projectId2 : FAKE_PROJECT_ID);
            image.put("id", "id" + Integer.toString(i + startId));
            image.put("name", "name" + Integer.toString(i + startId));
            myList.add(image);
        }

        for (int i = 0; i < qtdImages; i++) {
            Map<String, String> image = new HashMap<String, String>();
            image.put("visibility", i % 2 == 0 ? "community" : "shared");
            image.put("owner", i < qtdImages / 2 ? projectId2 : FAKE_PROJECT_ID);
            image.put("id", "id" + Integer.toString(qtdImages + i + startId));
            image.put("name", "name" + Integer.toString(qtdImages + i + startId));
            myList.add(image);
        }

        return myList;
    }

    private List<ImageSummary> generateJustPublicAndPrivateImagesFromProject(int startId, int qtdImages) {
        List<ImageSummary> images = new ArrayList<>();
        qtdImages /= 2;
        for (int i = 0; i < qtdImages; i++) {
            List<ImageSummary> image = new ArrayList<>();
            if (i % 2 == 0 || (i % 2 != 0 && i >= qtdImages / 2)) {
                ImageSummary imageSummary = new ImageSummary("id" + Integer.toString(i + startId),
                        "name" + Integer.toString(i + startId));
                images.add(imageSummary);
            }
        }
        return images;
    }
}
