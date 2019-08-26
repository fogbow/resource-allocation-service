package cloud.fogbow.ras.core.plugins.interoperability.openstack.image.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.*;

@PrepareForTest({GetImageResponse.class})
public class OpenStackImagePluginTest extends BaseUnitTests{

    private static final String FAKE_IMAGE_ID = "fake-image-id";

    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";

    private OpenStackImagePlugin plugin;
    private OpenStackHttpClient client;
    private Properties properties;
    private OpenStackV3User localUserAttributes;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.plugin = Mockito.spy(new OpenStackImagePlugin(cloudConfPath));
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.properties = Mockito.mock(Properties.class);
        this.localUserAttributes = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        this.plugin.setProperties(this.properties);
        this.plugin.setClient(this.client);
        
        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());

        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
    }

    @Test
    public void testGetAllImages() throws FogbowException{
        Mockito.doReturn(new ArrayList<>()).when(plugin).getAvailableImages(Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        plugin.getAvailableImages(cloudUser);
        Mockito.verify(plugin, Mockito.times(1)).getAvailableImages(Mockito.any());
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
                        + OpenStackImagePlugin.IMAGE_V2_API_ENDPOINT
                        + OpenStackImagePlugin.IMAGE_V2_API_SUFFIX
                        + OpenStackImagePlugin.QUERY_ACTIVE_IMAGES;

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

        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn(imageGlancev2UrlKey);
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
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(endpoint1, localUserAttributes);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(endpoint2, localUserAttributes);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(endpoint3, localUserAttributes);
    }

    @Test
    public void testGetUnavailableImage() throws FogbowException{
        GetImageResponse getImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(getImageResponse.getId()).thenReturn(FAKE_IMAGE_ID);
        Mockito.when(getImageResponse.getStatus()).thenReturn("unavailable");
        Mockito.doReturn(getImageResponse).when(plugin).getImageResponse(Mockito.any(), Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        ImageInstance imageInstance = plugin.getImage(FAKE_IMAGE_ID, cloudUser);

        Mockito.verify(plugin, Mockito.times(1)).getImageResponse(Mockito.any(), Mockito.any());
        Assert.assertEquals(null, imageInstance);
    }

    @Test
    public void testGetImage() throws FogbowException{
        GetImageResponse getImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(getImageResponse.getId()).thenReturn(FAKE_IMAGE_ID);
        Mockito.when(getImageResponse.getStatus()).thenReturn("active");
        Mockito.doReturn(getImageResponse).when(plugin).getImageResponse(Mockito.any(), Mockito.any());
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        ImageInstance imageInstance = plugin.getImage(FAKE_IMAGE_ID, cloudUser);

        Mockito.verify(plugin, Mockito.times(1)).getImageResponse(Mockito.any(), Mockito.any());
        Assert.assertFalse(imageInstance == null);
    }

    //test case: Test if getImageId throws UnauthorizedRequestException when the http requisition is SC_FORBIDDEN.
    @Test(expected = UnauthorizedRequestException.class)//verify
    public void testGetImageResponseWhenForbidden() throws FogbowException, HttpResponseException {
        //set up
        String imageId = "image-id";
        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn("");
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, "");
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.anyObject())).thenThrow(httpResponseException);

        //exercise
        this.plugin.getImageResponse(imageId, this.localUserAttributes);
    }

    //test case: Test if testGet throws UnexpectedException when the http requisition status is unknown.
    @Test(expected = UnexpectedException.class)//verify
    public void testGetImageResponseWhenUnexpectedException() throws FogbowException, HttpResponseException {
        //set up
        String imageId = "image-id";
        int unexpectedHttpStatus = -1;
        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn("");
        HttpResponseException httpResponseException = new HttpResponseException(unexpectedHttpStatus, "");
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.anyObject())).thenThrow(httpResponseException);

        //exercise
        this.plugin.getImageResponse(imageId, this.localUserAttributes);
    }

    //test case: Test if getAllImages throws UnauthorizedRequestException when the http requisition is SC_FORBIDDEN.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetAllImagesWhenForbidden() throws FogbowException, HttpResponseException {
        //set up
        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn("");
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, "");
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.anyObject())).thenThrow(httpResponseException);

        //exercise/verify
        this.plugin.getAllImages(this.localUserAttributes);
    }

    //test case: Test if getAllImages throws UnexpectedException when the http requisition status is unknown.
    @Test(expected = UnexpectedException.class)
    public void testGetAllImagesWhenUnexpectedException() throws FogbowException, HttpResponseException {
        int unexpectedHttpStatus = -1;
        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn("");
        HttpResponseException httpResponseException = new HttpResponseException(unexpectedHttpStatus, "");
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.anyObject())).thenThrow(httpResponseException);
        this.plugin.getAllImages(this.localUserAttributes);
    }

    //test case: Test if getAllImages throws UnauthorizedRequestException when the http requisition is SC_FORBIDDEN during pagination.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetAllImagesWithPaginationWhenForbidden() throws FogbowException, HttpResponseException {
        //set up
        String imageGlancev2UrlKey = "image-url-key";
        String nextUrl1 = "next-url1";

        String endpoint1 =
                imageGlancev2UrlKey
                        + OpenStackImagePlugin.IMAGE_V2_API_ENDPOINT
                        + OpenStackImagePlugin.IMAGE_V2_API_SUFFIX
                        + OpenStackImagePlugin.QUERY_ACTIVE_IMAGES;

        String endpoint2 =
                imageGlancev2UrlKey
                        + nextUrl1;

        List<Map<String, String>> generatedImages1 = generateImages(0, 100);

        String jsonResponse1 = getImagesJsonWithNext(generatedImages1, nextUrl1);

        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn(imageGlancev2UrlKey);
        Mockito.when(this.client.doGetRequest(endpoint1, localUserAttributes)).thenReturn(jsonResponse1);
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, "");
        Mockito.when(this.client.doGetRequest(endpoint2, localUserAttributes)).thenThrow(httpResponseException);

        //exercise/verify
        this.plugin.getAllImages(localUserAttributes);
    }

    //test case: Test if getAllImages throws UnexpectedException when the http requisition status is unknown during pagination.
    @Test(expected = UnexpectedException.class)//verify
    public void testGetAllImagesWithPaginationWhenUnexpectedException() throws FogbowException, HttpResponseException {
        //set up
        String imageGlancev2UrlKey = "image-url-key";
        String nextUrl1 = "next-url1";

        String endpoint1 =
                imageGlancev2UrlKey
                        + OpenStackImagePlugin.IMAGE_V2_API_ENDPOINT
                        + OpenStackImagePlugin.IMAGE_V2_API_SUFFIX
                        + OpenStackImagePlugin.QUERY_ACTIVE_IMAGES;

        String endpoint2 =
                imageGlancev2UrlKey
                        + nextUrl1;

        List<Map<String, String>> generatedImages1 = generateImages(0, 100);

        String jsonResponse1 = getImagesJsonWithNext(generatedImages1, nextUrl1);

        Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn(imageGlancev2UrlKey);
        Mockito.when(this.client.doGetRequest(endpoint1, localUserAttributes)).thenReturn(jsonResponse1);
        int unexpectedHttpStatus = -1;
        HttpResponseException httpResponseException = new HttpResponseException(unexpectedHttpStatus, "");
        Mockito.when(this.client.doGetRequest(endpoint2, localUserAttributes)).thenThrow(httpResponseException);

        //exercise
        this.plugin.getAllImages(localUserAttributes);
    }

    @Test
    public void testGetImageResponse() throws Exception{
        String json = "";
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenReturn(json);

        PowerMockito.mockStatic(GetImageResponse.class);
        PowerMockito.when(GetImageResponse.class, "fromJson", Mockito.any()).thenCallRealMethod();

        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        plugin.getImageResponse("", cloudUser);

        Mockito.verify(client, Mockito.times(1)).doGetRequest(Mockito.any(), Mockito.any());

        PowerMockito.verifyStatic(GetImageResponse.class, VerificationModeFactory.times(1));
        GetImageResponse.fromJson(json);
    }

    @Test(expected = UnexpectedException.class)
    public void testGetImageResponseUnsuccessfully() throws Exception{
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenThrow(new HttpResponseException(500, ""));

        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        plugin.getImageResponse("", cloudUser);
    }

    @Test
    public void testGetImagesResponse() throws Exception {
        String json = "";
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenReturn(json);

        PowerMockito.mockStatic(GetImageResponse.class);
        PowerMockito.when(GetImageResponse.class, "fromJson", Mockito.any()).thenCallRealMethod();

        GetAllImagesResponse getImageResponse = Mockito.mock(GetAllImagesResponse.class);
        Mockito.when(getImageResponse.getImages()).thenReturn(new ArrayList<>());
        Mockito.doReturn(getImageResponse).when(plugin).getAllImagesResponse(Mockito.any());
        Mockito.doNothing().when(plugin).getNextImageListResponseByPagination(Mockito.any(), Mockito.any(), Mockito.any());

        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        plugin.getImagesResponse(cloudUser);

        Mockito.verify(client, Mockito.times(1)).doGetRequest(Mockito.any(), Mockito.any());
        Mockito.verify(plugin, Mockito.times(1)).getAllImagesResponse(Mockito.any());
        Mockito.verify(plugin).getNextImageListResponseByPagination(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testGetNextImageListResponseByPagination() throws Exception{
        GetAllImagesResponse getImageResponse = Mockito.mock(GetAllImagesResponse.class);
        List<GetImageResponse> images = new ArrayList<>();
        images.add(new GetImageResponse());
        Mockito.when(getImageResponse.getImages()).thenReturn(images);
        Mockito.when(getImageResponse.getNext()).thenReturn("json");
        Mockito.when(client.doGetRequest(Mockito.any(), Mockito.any())).thenReturn("");

        GetAllImagesResponse getAllImagesResponseAux = Mockito.mock(GetAllImagesResponse.class);
        Mockito.when(getAllImagesResponseAux.getImages()).thenReturn(new ArrayList<>());
        Mockito.doReturn(getAllImagesResponseAux).when(plugin).getAllImagesResponse(Mockito.any());

        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        plugin.getNextImageListResponseByPagination(cloudUser, getImageResponse, new ArrayList<>());

        Mockito.verify(client, Mockito.times(1)).doGetRequest(Mockito.any(), Mockito.any());
        Mockito.verify(plugin, Mockito.times(1)).getAllImagesResponse(Mockito.any());
    }

    @Test
    public void testGetPublicImagesResponse() {
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

        List<GetImageResponse> allowedImages = plugin.getPublicImagesResponse(images);

        Assert.assertTrue(allowedImages.stream().allMatch(each -> each.getVisibility().equalsIgnoreCase("public")));
    }

    @Test
    public void testGetPrivateImagesResponse() {
        GetImageResponse firstPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse firstPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(firstPublicImageResponse.getVisibility()).thenReturn("public");
        Mockito.when(secondPublicImageResponse.getVisibility()).thenReturn("public");
        Mockito.when(firstPrivateImageResponse.getVisibility()).thenReturn("private");
        Mockito.when(secondPrivateImageResponse.getVisibility()).thenReturn("private");
        Mockito.when(firstPrivateImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(secondPrivateImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(firstPublicImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(secondPublicImageResponse.getOwner()).thenReturn("fogbow");

        List<GetImageResponse> images = new ArrayList<>();
        images.add(firstPublicImageResponse);
        images.add(secondPublicImageResponse);
        images.add(firstPrivateImageResponse);
        images.add(secondPrivateImageResponse);

        List<GetImageResponse> filteredImages = plugin.getPrivateImagesResponse(images, "fogbow");

        Assert.assertTrue(filteredImages.stream().allMatch(each -> each.getVisibility().equalsIgnoreCase("private")));
    }

    @Test
    public void testGetAvailableImages() throws FogbowException{
        GetImageResponse firstPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPublicImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse firstPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse secondPrivateImageResponse = Mockito.mock(GetImageResponse.class);
        GetImageResponse anyImageResponse = Mockito.mock(GetImageResponse.class);
        Mockito.when(firstPublicImageResponse.getVisibility()).thenReturn("public");
        Mockito.when(secondPublicImageResponse.getVisibility()).thenReturn("public");
        Mockito.when(firstPrivateImageResponse.getVisibility()).thenReturn("private");
        Mockito.when(secondPrivateImageResponse.getVisibility()).thenReturn("private");
        Mockito.when(anyImageResponse.getVisibility()).thenReturn("any");
        Mockito.when(firstPrivateImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(secondPrivateImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(firstPublicImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(secondPublicImageResponse.getOwner()).thenReturn("fogbow");
        Mockito.when(anyImageResponse.getOwner()).thenReturn("fogbow");

        List<GetImageResponse> images = new ArrayList<>();
        images.add(firstPublicImageResponse);
        images.add(secondPublicImageResponse);
        images.add(firstPrivateImageResponse);
        images.add(secondPrivateImageResponse);
        images.add(anyImageResponse);

        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        Mockito.when(cloudUser.getProjectId()).thenReturn("fogbow");
        Mockito.doReturn(images).when(plugin).getImagesResponse(Mockito.any());

        List<ImageSummary> availableImages = plugin.getAvailableImages(cloudUser);

        Assert.assertFalse(availableImages.contains(anyImageResponse));
        Assert.assertTrue(availableImages.size() == 4);
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

    private List<ImageSummary> generateImageSummaries(int startId, int qtdImages) {
        String projectId2 = "2";
        List<ImageSummary> myList = new ArrayList<ImageSummary>();

        qtdImages /= 2;

        for (int i = 0; i < qtdImages; i++) {
            List<ImageSummary> image = new ArrayList<>();
            ImageSummary imageSummary = new ImageSummary("id" + Integer.toString(i + startId),
                    "name" + Integer.toString(i + startId));
            myList.add(imageSummary);
        }

        for (int i = 0; i < qtdImages; i++) {
            List<ImageSummary> image = new ArrayList<>();
            ImageSummary imageSummary = new ImageSummary("id" + Integer.toString(i + startId),
                    "name" + Integer.toString(i + startId));
            myList.add(imageSummary);
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

    private String getImageJsonFromImage(ImageInstance imageInstance) {
        Map<String, String> jsonMap = new HashMap<String, String>();
        jsonMap.put(OpenStackConstants.Image.ID_KEY_JSON, imageInstance.getId());
        jsonMap.put(OpenStackConstants.Image.NAME_KEY_JSON, imageInstance.getName());
        jsonMap.put(OpenStackConstants.Image.SIZE_KEY_JSON, Long.toString(imageInstance.getSize()));
        jsonMap.put(OpenStackConstants.Image.MIN_DISK_KEY_JSON, Long.toString(imageInstance.getMinDisk()));
        jsonMap.put(OpenStackConstants.Image.MIN_RAM_KEY_JSON, Long.toString(imageInstance.getMinRam()));
        jsonMap.put(OpenStackConstants.Image.STATUS_KEY_JSON, imageInstance.getStatus());
        Gson gson = new Gson();
        String jsonResponse = gson.toJson(jsonMap);
        return jsonResponse;
    }
}
