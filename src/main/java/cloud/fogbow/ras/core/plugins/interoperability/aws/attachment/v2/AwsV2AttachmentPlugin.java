package cloud.fogbow.ras.core.plugins.interoperability.aws.attachment.v2;

import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.aws.volume.v2.AwsV2VolumePlugin;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;

public class AwsV2AttachmentPlugin implements AttachmentPlugin<CloudUser>{

	private static final Logger LOGGER = Logger.getLogger(AwsV2VolumePlugin.class);
	private static final String ANOTHER_DEVICE_TYPE = "xvdh";
	private static final String FILTER_BY_TAG_ATTACHMENT_ID = "tag:attachment-id";
	private static final String RESOURCE_NAME = "Attachment";
	private static final int FIRST_POSITION = 0;
	private static final int SECOND_POSITION = 1;

	protected static final String ATTACHMENT_ID_PREFIX = "att-";
	protected static final String ATTACHMENT_ID_TAG = "attachment-id";
	protected static final String DEFAULT_DEVICE_NAME = "/dev/sdh";
	
	private Properties properties;
	private String region;
	
	public AwsV2AttachmentPlugin(String confFilePath) {
		this.properties = PropertiesUtil.readProperties(confFilePath);
		this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
	}
	
	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.ATTACHMENT, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.ATTACHMENT, instanceState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		// Device name example: ["/dev/sdh", "xvdh"]
		String deviceName = attachmentOrder.getDevice();
		String device = (deviceName != null && deviceName.equals(ANOTHER_DEVICE_TYPE)) ? deviceName : DEFAULT_DEVICE_NAME;
		String instanceId = attachmentOrder.getComputeId();
		String volumeId = attachmentOrder.getVolumeId();
		AttachVolumeRequest attachmentRequest = AttachVolumeRequest.builder()
				.device(device)
				.instanceId(instanceId)
				.volumeId(volumeId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		client.attachVolume(attachmentRequest);
		
		String attachmentId = ATTACHMENT_ID_PREFIX + getRandomUUID();
		CreateTagsRequest tagRequest = createTagAttachmentId(attachmentId, volumeId);
		client.createTags(tagRequest);

		return attachmentId;
	}

	@Override
	public void deleteInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, attachmentOrder.getInstanceId(), cloudUser.getToken()));

		String volumeId = attachmentOrder.getVolumeId();
		DetachVolumeRequest detachVolumeRequest = DetachVolumeRequest.builder().volumeId(volumeId).build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		try {
			client.detachVolume(detachVolumeRequest);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME,
					attachmentOrder.getInstanceId()), e);

			throw new UnexpectedException();
		}
	}

	@Override
	public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, attachmentOrder.getInstanceId(), cloudUser.getToken()));

		String attachmentId = attachmentOrder.getInstanceId();
		Filter filter = Filter.builder()
				.name(FILTER_BY_TAG_ATTACHMENT_ID)
				.values(attachmentId)
				.build();

		DescribeVolumesRequest volumesRequest = DescribeVolumesRequest.builder()
				.filters(filter)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeVolumesResponse volumesResponse = client.describeVolumes(volumesRequest);

		AttachmentInstance attachmentInstance = mountAttachmentInstance(volumesResponse);
		return attachmentInstance;
	}
	
	protected AttachmentInstance mountAttachmentInstance(DescribeVolumesResponse response) {
		Volume volume = response.volumes().get(FIRST_POSITION);
		String id = volume.tags().get(SECOND_POSITION).value();
		VolumeAttachment attachment = volume.attachments().get(FIRST_POSITION);
		String cloudState = attachment.stateAsString();
		String computeId = attachment.instanceId();
		String volumeId = attachment.volumeId();
		String device = attachment.device();
		return new AttachmentInstance(id, cloudState, computeId, volumeId, device);
	}

	protected CreateTagsRequest createTagAttachmentId(String attachmentId, String volumeId) {
		Tag tagAttachmentId = Tag.builder()
				.key(ATTACHMENT_ID_TAG)
				.value(attachmentId)
				.build();

		CreateTagsRequest tagRequest = CreateTagsRequest.builder()
				.resources(volumeId)
				.tags(tagAttachmentId)
				.build();

		return tagRequest;
	}
	
	// This method is used to aid in the tests
	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}

}
