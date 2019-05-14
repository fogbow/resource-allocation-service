package cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.Image;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.*;

public class AwsV2ImagePlugin implements ImagePlugin<AwsV2User> {

    private final Integer K_BYTES = 1024;
    private final Integer K_BYTES_TO_GB = 3;
    private final Integer NO_VALUE_FLAG = -1;
    private final Integer EMPTY_LIST_FLAG = 0;

    private Properties properties;
    private String region;

    public AwsV2ImagePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    public Map<String, String> getAllImages(AwsV2User cloudUser) throws FogbowException {
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().owners(cloudUser.getId()).build();
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);

        DescribeImagesResponse imagesResponse = client.describeImages(imagesRequest);
        Map<String, String> images = new HashMap<>();
        List<software.amazon.awssdk.services.ec2.model.Image> retrievedImages = imagesResponse.images();

        for(software.amazon.awssdk.services.ec2.model.Image image : retrievedImages) {
            images.put(image.imageId(), image.name());
        }

        return images;
    }

    public Image getImage(String imageId, AwsV2User cloudUser) throws FogbowException {
        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().imageIds(imageId).build();

        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);

        DescribeImagesResponse imagesResponse = client.describeImages(imagesRequest);

        Image image = null;

        if(imagesResponse.images().size() > EMPTY_LIST_FLAG) {
            software.amazon.awssdk.services.ec2.model.Image retrievedImage = imagesResponse.images().get(0);
            image = getImageResponse(retrievedImage);
        }

        return image;
    }

    private Image getImageResponse(software.amazon.awssdk.services.ec2.model.Image awsImage) {
        long size = getSize(awsImage.blockDeviceMappings());

        return new Image(
                awsImage.imageId(),
                awsImage.name(),
                size,
                NO_VALUE_FLAG,
                NO_VALUE_FLAG,
                AwsV2StateMapper.map(ResourceType.IMAGE, awsImage.stateAsString()).getValue()
        );
    }

    protected long getSize(List<BlockDeviceMapping> blocks) {
        long size = EMPTY_LIST_FLAG;

        for (BlockDeviceMapping block : blocks) {
            size += block.ebs().volumeSize();
        }

        return  size* (long) Math.pow(K_BYTES, K_BYTES_TO_GB);
    }

}
