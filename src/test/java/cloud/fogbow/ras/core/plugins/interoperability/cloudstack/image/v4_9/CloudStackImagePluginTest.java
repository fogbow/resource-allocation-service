package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

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
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@PrepareForTest({CloudStackUrlUtil.class, DatabaseManager.class,
        GetAllImagesResponse.class, CloudStackCloudUtils.class})
public class CloudStackImagePluginTest extends BaseUnitTests {

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

        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
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
    // it must verify if It returns a FogbowException.
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
    // it must verify if It returns a FogbowException.
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

    // test case: When calling the getImage method with secondary methods mocked,
    // it must verify if the buildImageInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testGetImageSuccessfully() throws FogbowException {
        // set up
        String imageId = "imageId";
        ImageInstance imageExpected = Mockito.mock(ImageInstance.class);
        Mockito.doReturn(imageExpected).when(this.plugin).buildImageInstance(Mockito.any(),
                Mockito.eq(this.cloudStackUser));

        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .id(imageId)
                .build(this.cloudStackUrl);

        // exercise
        ImageInstance image = this.plugin.getImage(imageId, this.cloudStackUser);

        // verify
        Assert.assertEquals(imageExpected, image);
        RequestMatcher<GetAllImagesRequest> matcher = new RequestMatcher.GetAllImages(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildImageInstance(
                Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the getImage method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.
    @Test(expected = FogbowException.class)
    public void testGetImageFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin).buildImageInstance(Mockito.any(),
                Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.getImage("", this.cloudStackUser);
    }

    // test case: When calling the buildImageInstance method with secondary methods mocked,
    // it must verify if It returns the rigth imageInstance.
    @Test
    public void testBuildImageInstanceSuccessfully() throws FogbowException {
        // set up
        GetAllImagesRequest request = Mockito.mock(GetAllImagesRequest.class);
        String idExpected = "one";
        String nameExpected = "nameOne";

        GetAllImagesResponse.Image imageMockedOne = Mockito.mock(GetAllImagesResponse.Image.class);
        Mockito.when(imageMockedOne.getId()).thenReturn(idExpected);
        Mockito.when(imageMockedOne.getName()).thenReturn(nameExpected);

        GetAllImagesResponse response = Mockito.mock(GetAllImagesResponse.class);
        List<GetAllImagesResponse.Image> imagesExpected = new ArrayList<>();
        imagesExpected.add(imageMockedOne);
        Mockito.when(response.getImages()).thenReturn(imagesExpected);
        Mockito.doReturn(response).when(this.plugin).doDescribeImagesRequest(
                Mockito.eq(request), Mockito.eq(this.cloudStackUser));

        // exercise
        ImageInstance imageInstance = this.plugin.buildImageInstance(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(nameExpected, imageInstance.getName());
        Assert.assertEquals(idExpected, imageInstance.getId());
        Assert.assertEquals(CloudStackImagePlugin.DEFAULT_MIN_DISK_VALUE, imageInstance.getMinDisk());
        Assert.assertEquals(CloudStackImagePlugin.DEFAULT_MIN_RAM_VALUE, imageInstance.getMinRam());
        Assert.assertEquals(CloudStackImagePlugin.DEFAULT_STATUS_VALUE, imageInstance.getStatus());
    }

    // test case: When calling the getAllImages method and there is no image,
    // it must verify if It returns an InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class)
    public void testBuildImageInstanceFailWhenImageNotFound() throws FogbowException {
        // set up
        GetAllImagesRequest request = Mockito.mock(GetAllImagesRequest.class);

        GetAllImagesResponse response = Mockito.mock(GetAllImagesResponse.class);
        List<GetAllImagesResponse.Image> imagesEmpty = new ArrayList<>();
        Mockito.when(response.getImages()).thenReturn(imagesEmpty);
        Mockito.doReturn(response).when(this.plugin).doDescribeImagesRequest(
                Mockito.eq(request), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.buildImageInstance(request, this.cloudStackUser);
    }

    // test case: When calling the getAllImages method and occurs a FogbowException when it's call the
    // doDescribeImagesRequest method, it must verify if It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testBuildImageInstanceFail() throws FogbowException {
        // set up
        GetAllImagesRequest request = Mockito.mock(GetAllImagesRequest.class);

        Mockito.doThrow(new FogbowException()).when(this.plugin).doDescribeImagesRequest(
                Mockito.eq(request), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.buildImageInstance(request, this.cloudStackUser);
    }

}
