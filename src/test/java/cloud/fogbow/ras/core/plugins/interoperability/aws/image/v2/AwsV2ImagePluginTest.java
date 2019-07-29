package cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsV2ClientUtil.class})
public class AwsV2ImagePluginTest {

    private static final String ANY_VALUE = "anything";
    private static final String CLOUD_NAME = "amazon";
    private static final String FIRST_IMAGE_ID = "first-image-id";
    private static final String FIRST_IMAGE_NAME = "first-image";
    private static final String SECOND_IMAGE_ID = "second-image-id";
    private static final String SECOND_IMAGE_NAME = "second-image";
    private static final String THIRD_IMAGE_ID = "third-image-id";
    private static final String THIRD_IMAGE_NAME = "third-image";

    private static final long EXPECTED_IMAGE_SIZE = 8*(long)Math.pow(1024, 3);

    private AwsV2ImagePlugin plugin;

    @Before
    public void setUp() {
        String awsConfFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator
                + CLOUD_NAME
                + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsV2ImagePlugin(awsConfFilePath));
    }

    // test case: check if getAllImages returns all images expected in the expected
    // format.
    @Test
    public void testGetAllImages() throws FogbowException {
        // setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        List<Image> imagesList = getMockedImages();

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest request = DescribeImagesRequest.builder().owners(cloudUser.getId()).build();

        List<ImageSummary> expectedResult = new ArrayList<>();
        for (Image each : imagesList) {
            expectedResult.add(new ImageSummary(each.imageId(), each.name()));
        }

        Mockito.when(client.describeImages(request))
                .thenReturn(DescribeImagesResponse.builder().images(imagesList).build());

        // exercise
        List<ImageSummary> result = this.plugin.getAllImages(cloudUser);

        // verify
        Assert.assertEquals(expectedResult, result);
    }

    // test case: check if the getImage returns the correct image when there are some.
    @Test
    public void testGetImageWithResult() throws FogbowException {
        //setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        List<Image> imagesList = getMockedImages();

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest request = DescribeImagesRequest.builder().imageIds(FIRST_IMAGE_ID).build();

        Mockito.when(client.describeImages(request)).thenReturn(DescribeImagesResponse.builder().images(imagesList).build());

        ImageInstance expected = createImageInstance();
        
        // exercise
        ImageInstance image = this.plugin.getImage(FIRST_IMAGE_ID, cloudUser);

        // verify
        Assert.assertEquals(expected, image);
    }

    // test case : check getImage behavior when there is no image to be returned.
    @Test(expected = InstanceNotFoundException.class) // verify
    public void testGetImageWithoutResult() throws FogbowException {
        // setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().imageIds(ANY_VALUE).build();

        Mockito.when(client.describeImages(imagesRequest)).thenReturn(DescribeImagesResponse.builder().images(new ArrayList<>()).build());

        // exercise
        this.plugin.getImage(ANY_VALUE, cloudUser);
    }

    // test case: check if testSize works properly with some specific args.
    @Test
    public void testSize() {
        // setup
        List<Integer> list = new ArrayList<>(
            Arrays.asList(0)
        );

        List<BlockDeviceMapping> blocks = getMockedBlocks(list);

        // exercise
        long size = this.plugin.getImageSize(blocks);

        //verify
        Assert.assertEquals(0, size);

        // setup
        list = new ArrayList<>(
            Arrays.asList(1, 4, 2, 8, 10)
        );

        blocks = getMockedBlocks(list);

        //exercise
        size = this.plugin.getImageSize(blocks);

        // verify
        Assert.assertEquals((long) Math.pow(1024, 3)*(1+4+2+8+10), size);
    }

    private List<BlockDeviceMapping> getMockedBlocks(List<Integer> sizes) {
        List<BlockDeviceMapping> blocks = new ArrayList<>();

        BlockDeviceMapping block;
        for (Integer size : sizes) {
            block = BlockDeviceMapping.builder().ebs(EbsBlockDevice.builder().volumeSize(size).build()).build();
            blocks.add(block);
        }

        return blocks;
    }

    private ImageInstance createImageInstance() {
		return new ImageInstance(
                FIRST_IMAGE_ID,
                FIRST_IMAGE_NAME,
                EXPECTED_IMAGE_SIZE,
                AwsV2ImagePlugin.NO_VALUE_FLAG,
                AwsV2ImagePlugin.NO_VALUE_FLAG, 
                AwsV2StateMapper.AVAILABLE_STATE);
	}
    
    private List<software.amazon.awssdk.services.ec2.model.Image> getMockedImages() {
        BlockDeviceMapping block = BlockDeviceMapping.builder()
                .ebs(EbsBlockDevice.builder()
                        .volumeSize(8)
                        .build())
                .build();
        
        Image imageOne = Image.builder()
            .imageId(FIRST_IMAGE_ID)
            .name(FIRST_IMAGE_NAME)
            .blockDeviceMappings(block)
            .state(AwsV2StateMapper.AVAILABLE_STATE)
            .build();
        
        Image imageTwo = software.amazon.awssdk.services.ec2.model.Image.builder()
            .imageId(SECOND_IMAGE_ID)
            .name(SECOND_IMAGE_NAME)
            .build();
        
        Image imageThree = software.amazon.awssdk.services.ec2.model.Image.builder()
            .imageId(THIRD_IMAGE_ID)
            .name(THIRD_IMAGE_NAME)
            .build();

        List<Image> imagesList = new ArrayList<>();
        imagesList.add(imageOne);
        imagesList.add(imageTwo);
        imagesList.add(imageThree);

        return imagesList;
    }
}
