package cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.File;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsV2ClientUtil.class})
public class AwsV2ImagePluginTest {

    private static final String CLOUD_NAME = "amazon";
    private static final String FIMAGE_ID = "mockedId";
    private static final String FIMAGE_NAME = "first";
    private static final String SIMAGE_ID = "mockedId2";
    private static final String SIMAGE_NAME = "second";
    private static final String TIMAGE_ID = "mockedId3";
    private static final String TIMAGE_NAME = "third";
    private static final String AVAILABLE_STATE = "available";
    private static final long EXPECTED_FIMAGE_SIZE = 8*(long)Math.pow(1024, 3);
    private final Integer NO_VALUE_FLAG = -1;

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

    // test case: check if getAllImages returns all images expected in the expected format.
    @Test
    public void testGetAllImages() throws FogbowException {
        // setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        List<software.amazon.awssdk.services.ec2.model.Image> imagesList = getMockedImages();

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().owners(cloudUser.getId()).build();

        Map<String, String> expectedResult = new HashMap<>();
        for(Image each: imagesList) {
            expectedResult.put(each.imageId(), each.name());
        }

        Mockito.when(client.describeImages(imagesRequest)).thenReturn(DescribeImagesResponse.builder().images(imagesList).build());

        // exercise
        Map<String, String> result = this.plugin.getAllImages(cloudUser);

        //verify
        Assert.assertEquals(expectedResult, result);
    }

    // test case: check if the getImage returns the correct image when there are some.
    @Test
    public void testGetImageWithResult() throws FogbowException {
        //setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        List<software.amazon.awssdk.services.ec2.model.Image> imagesList = getMockedImages();

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().imageIds(FIMAGE_ID).build();

        Mockito.when(client.describeImages(imagesRequest)).thenReturn(DescribeImagesResponse.builder().images(imagesList).build());

        // exercise
        cloud.fogbow.ras.api.http.response.Image image = this.plugin.getImage(FIMAGE_ID, cloudUser);

        // verify
        Assert.assertEquals(new cloud.fogbow.ras.api.http.response.Image(
                FIMAGE_ID,
                FIMAGE_NAME,
                EXPECTED_FIMAGE_SIZE,
                NO_VALUE_FLAG,
                NO_VALUE_FLAG,
                AwsV2StateMapper.map(ResourceType.IMAGE, AVAILABLE_STATE).getValue()), image);
    }

    // test case : check getImage behavior when there is no image to be returned.
    @Test
    public void testGetImageWithoutResult() throws FogbowException {
        // setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().imageIds("mockedNullId").build();

        Mockito.when(client.describeImages(imagesRequest)).thenReturn(DescribeImagesResponse.builder().images(new ArrayList<>()).build());

        // exercise
        cloud.fogbow.ras.api.http.response.Image image = this.plugin.getImage("mockedNullId", cloudUser);

        // verify
        Assert.assertEquals(null, image);
    }

    // check if testSize works properly with some specific args.
    @Test
    public void testSize() {
        // setup
        List<Integer> list = new ArrayList<>(
            Arrays.asList(0)
        );

        List<BlockDeviceMapping> blocks = getMockedBlocks(list);

        // exercise
        long size = this.plugin.getSize(blocks);

        //verify
        Assert.assertEquals(0, size);

        // setup
        list = new ArrayList<>(
            Arrays.asList(1, 4, 2, 8, 10)
        );

        blocks = getMockedBlocks(list);

        //exercise
        size = this.plugin.getSize(blocks);

        // verify
        Assert.assertEquals((long) Math.pow(1024, 3)*(1+4+2+8+10), size);
    }

    private List<BlockDeviceMapping> getMockedBlocks(List<Integer> sizes) {
        List<BlockDeviceMapping> blocks = new ArrayList<>();

        BlockDeviceMapping block;
        for(Integer size : sizes) {
            block = BlockDeviceMapping.builder().ebs(EbsBlockDevice.builder().volumeSize(size).build()).build();
            blocks.add(block);
        }

        return blocks;
    }

    private List<software.amazon.awssdk.services.ec2.model.Image> getMockedImages() {
        BlockDeviceMapping block = BlockDeviceMapping.builder().ebs(EbsBlockDevice.builder().volumeSize(8).build()).build();
        software.amazon.awssdk.services.ec2.model.Image image = software.amazon.awssdk.services.ec2.model.Image.builder()
            .imageId(FIMAGE_ID)
            .name(FIMAGE_NAME)
            .blockDeviceMappings(block)
            .state(AVAILABLE_STATE)
            .build();
        software.amazon.awssdk.services.ec2.model.Image image2 = software.amazon.awssdk.services.ec2.model.Image.builder()
            .imageId(SIMAGE_ID)
            .name(SIMAGE_NAME)
            .build();
        software.amazon.awssdk.services.ec2.model.Image image3 = software.amazon.awssdk.services.ec2.model.Image.builder()
            .imageId(TIMAGE_ID)
            .name(TIMAGE_NAME)
            .build();

        List<Image> imagesList = new ArrayList<>();
        imagesList.add(image);
        imagesList.add(image2);
        imagesList.add(image3);

        return imagesList;
    }
}
