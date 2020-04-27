package cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.Image;

public class AwsImagePlugin implements ImagePlugin<AwsV2User> {

	private static final int ONE_THOUSAND_BYTES = 1024;
	
	protected static final Integer NO_VALUE_FLAG = -1;

    private String region;

    public AwsImagePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public List<ImageSummary> getAllImages(AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        
        DescribeImagesRequest request = DescribeImagesRequest.builder()
        		.owners(cloudUser.getId())
        		.build();
        
        DescribeImagesResponse response = AwsV2CloudUtil.doDescribeImagesRequest(request, client);
        return buildImagesSummary(response);
    }

    @Override
    public ImageInstance getImage(String imageId, AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        
        DescribeImagesRequest request = DescribeImagesRequest.builder()
        		.imageIds(imageId)
        		.build();

        DescribeImagesResponse response = AwsV2CloudUtil.doDescribeImagesRequest(request, client);
        Image retrievedImage = AwsV2CloudUtil.getImagesFrom(response);
        return buildImageInstance(retrievedImage);
    }
    
	protected ImageInstance buildImageInstance(Image image) {
        String id = image.imageId();
        String name = image.name();
		String status = image.stateAsString();
		long size = getImageSize(image.blockDeviceMappings());
		long minDisk = NO_VALUE_FLAG;
		long minRam = NO_VALUE_FLAG;
        return new ImageInstance(id, name, size, minDisk, minRam, status);
    }

    protected long getImageSize(List<BlockDeviceMapping> blockDeviceMappings) {
    	long kilobyte = ONE_THOUSAND_BYTES;
    	long megabyte = kilobyte * ONE_THOUSAND_BYTES;
    	long gigabyte = megabyte * ONE_THOUSAND_BYTES;
    	long size = 0;
        for (BlockDeviceMapping blockDeviceMapping : blockDeviceMappings) {
            size += blockDeviceMapping.ebs().volumeSize();
        }
        return size * gigabyte;
    }
    
    protected List<ImageSummary> buildImagesSummary(DescribeImagesResponse response) {
        List<ImageSummary> images = new ArrayList<>();
        
        List<Image> retrievedImages = response.images();
        for (Image image : retrievedImages) {
            ImageSummary imageSummary = new ImageSummary(image.imageId(), image.name());
            images.add(imageSummary);
        }
        return images;
    }

}
