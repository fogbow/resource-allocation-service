package cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.CloudUser;
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
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsV2ClientUtil.class})
public class AwsV2ImagePluginTest {

    private static final String CLOUD_NAME = "amazon";
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

    @Test
    public void testGetAllImages() throws FogbowException {
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
        Map<String, String> result = this.plugin.getAllImages(cloudUser);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testGetImageWithResult() throws FogbowException {
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        List<software.amazon.awssdk.services.ec2.model.Image> imagesList = getMockedImages();

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().imageIds("mockedId").build();

        Mockito.when(client.describeImages(imagesRequest)).thenReturn(DescribeImagesResponse.builder().images(imagesList).build());

        cloud.fogbow.ras.api.http.response.Image image = this.plugin.getImage("mockedId", cloudUser);

        Assert.assertEquals(new cloud.fogbow.ras.api.http.response.Image(
                "mockedId",
                "first",
                0,
                -1,
                -1,
                AwsV2StateMapper.map(ResourceType.IMAGE, "available").getValue()
        ), image);
    }

    private List<software.amazon.awssdk.services.ec2.model.Image> getMockedImages() {
        software.amazon.awssdk.services.ec2.model.Image image = software.amazon.awssdk.services.ec2.model.Image.builder().imageId("mockedId").name("first").state("available").build();
        software.amazon.awssdk.services.ec2.model.Image image2 = software.amazon.awssdk.services.ec2.model.Image.builder().imageId("mockedId2").name("second").build();
        software.amazon.awssdk.services.ec2.model.Image image3 = software.amazon.awssdk.services.ec2.model.Image.builder().imageId("mockedId3").name("third").build();

        List<Image> imagesList = new ArrayList<>();
        imagesList.add(image);
        imagesList.add(image2);
        imagesList.add(image3);

        return imagesList;
    }
}
