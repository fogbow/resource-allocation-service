package cloud.fogbow.ras.core.plugins.interoperability.aws.volume.v2;

import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

public class AwsV2VolumePlugin implements VolumePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2VolumePlugin.class);
	private static final String AWS_TAG_NAME = "Name";
	private static final String RESOURCE_NAME = "Volume";
	private static final int FIRST_POSITION = 0;
	
	private Properties properties;
	private String region;
	private String zone;
	
	public AwsV2VolumePlugin(String confFilePath) {
		this.properties = PropertiesUtil.readProperties(confFilePath);
		this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_AVAILABILITY_ZONE_KEY);
	}
	
	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		
		CreateVolumeRequest volumeRequest = CreateVolumeRequest.builder()
				.size(volumeOrder.getVolumeSize())
				.availabilityZone(this.zone)
				.build();
		
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		CreateVolumeResponse volumeResponse = client.createVolume(volumeRequest);
		String volumeId = volumeResponse.volumeId();
		
		String volumeName = volumeOrder.getName();
		String name = volumeName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : volumeName;
		Tag tagName = Tag.builder()
	            .key(AWS_TAG_NAME)
	            .value(name)
	            .build();
		
		CreateTagsRequest tagRequest = CreateTagsRequest.builder()
				.resources(volumeId)
				.tags(tagName)
				.build();
		
		client.createTags(tagRequest);

		return volumeId;
	}

	@Override
	public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, volumeOrder.getInstanceId(), cloudUser.getToken()));

		String volumeId = volumeOrder.getInstanceId();
		DescribeVolumesRequest volumesRequest = DescribeVolumesRequest.builder()
				.volumeIds(volumeId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeVolumesResponse volumesResponse = client.describeVolumes(volumesRequest);
		VolumeInstance volumeInstance = mountVolumeInstance(volumesResponse);
		return volumeInstance;
	}

	private VolumeInstance mountVolumeInstance(DescribeVolumesResponse volumesResponse) {
		Volume volume = volumesResponse.volumes().get(FIRST_POSITION);
		String id = volume.volumeId();
		String cloudState = volume.stateAsString();
		String name = volume.tags().get(FIRST_POSITION).value();
		Integer size = volume.size();
		VolumeInstance volumeInstance = new VolumeInstance(id, cloudState, name, size);
		return volumeInstance;
	}

	@Override
	public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, volumeOrder.getInstanceId(), cloudUser.getToken()));
		
		String volumeId = volumeOrder.getInstanceId();
		DeleteVolumeRequest deleteVolumeRequest = DeleteVolumeRequest.builder()
				.volumeId(volumeId)
				.build();
		
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		try {
			client.deleteVolume(deleteVolumeRequest);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME, volumeId), e);
			throw new UnexpectedException();
		}
	}

	// This method is used to aid in the tests
	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
}
