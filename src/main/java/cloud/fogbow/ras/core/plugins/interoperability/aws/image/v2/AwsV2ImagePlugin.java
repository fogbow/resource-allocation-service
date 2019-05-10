package cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.Image;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeAccountAttributesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;

import java.util.*;

public class AwsV2ImagePlugin implements ImagePlugin<AwsV2User> {

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
        software.amazon.awssdk.services.ec2.model.Image retrievedImage = imagesResponse.images().get(0);

//        return new Image(
//            retrievedImage.imageId(),
//            retrievedImage.name(),
//
//        );
        return null;
    }


}
