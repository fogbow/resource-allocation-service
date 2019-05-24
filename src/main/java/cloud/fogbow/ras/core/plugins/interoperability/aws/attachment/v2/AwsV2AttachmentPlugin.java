package cloud.fogbow.ras.core.plugins.interoperability.aws.attachment.v2;

import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
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
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;

public class AwsV2AttachmentPlugin implements AttachmentPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2VolumePlugin.class);
	private static final String RESOURCE_NAME = "Attachment";
	private static final int FIRST_POSITION = 0;

	protected static final String ATTACHMENT_ID_PREFIX = "att-";
	protected static final String ATTACHMENT_ID_TAG = "attachment-id";
	protected static final String DEFAULT_DEVICE_NAME = "/dev/sdh";
	protected static final String FILTER_BY_TAG_ATTACHMENT_ID = "tag:attachment-id";
	protected static final String XVDH_DEVICE_NAME = "xvdh";

	private String region;

	public AwsV2AttachmentPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
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
	public String requestInstance(AttachmentOrder attachmentOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		String device = defineDeviceNameAttached(attachmentOrder.getDevice());
		String instanceId = attachmentOrder.getComputeId();
		String volumeId = attachmentOrder.getVolumeId();
		AttachVolumeRequest request = AttachVolumeRequest.builder()
				.device(device)
				.instanceId(instanceId)
				.volumeId(volumeId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		client.attachVolume(request);

		String attachmentId = attachmentOrder.getVolumeId();
		return attachmentId;
	}

	@Override
	public void deleteInstance(AttachmentOrder attachmentOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, attachmentOrder.getInstanceId(), 
				cloudUser.getToken()));

		String volumeId = attachmentOrder.getVolumeId();
		DetachVolumeRequest request = DetachVolumeRequest.builder()
				.volumeId(volumeId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		try {
			client.detachVolume(request);
		} catch (Ec2Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME,
					attachmentOrder.getInstanceId()), e);

			throw new UnexpectedException();
		}
	}

	@Override
	public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, attachmentOrder.getInstanceId(), 
				cloudUser.getToken()));

		String attachmentId = attachmentOrder.getInstanceId();
		DescribeVolumesRequest request = DescribeVolumesRequest.builder()
				.volumeIds(attachmentId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeVolumesResponse volumesResponse = client.describeVolumes(request);

		AttachmentInstance attachmentInstance = mountAttachmentInstance(volumesResponse);
		return attachmentInstance;
	}

	protected AttachmentInstance mountAttachmentInstance(DescribeVolumesResponse response)
			throws InstanceNotFoundException {

		VolumeAttachment attachment = getFirstAttachment(response);
		String cloudState = attachment.stateAsString();
		String computeId = attachment.instanceId();
		String volumeId = attachment.volumeId();
		String device = attachment.device();
		return new AttachmentInstance(volumeId, cloudState, computeId, volumeId, device);
	}

	protected VolumeAttachment getFirstAttachment(DescribeVolumesResponse response) throws InstanceNotFoundException {
		Volume volume = getFirstVolume(response);
		if (!volume.attachments().isEmpty()) {
			return volume.attachments().get(FIRST_POSITION);
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected Volume getFirstVolume(DescribeVolumesResponse response) throws InstanceNotFoundException {
		if (!response.volumes().isEmpty()) {
			return response.volumes().get(FIRST_POSITION);
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected String defineDeviceNameAttached(String deviceName) {
		// Device name example: ["/dev/sdh", "xvdh"]
		return (deviceName != null && deviceName.equals(XVDH_DEVICE_NAME)) ? deviceName : DEFAULT_DEVICE_NAME;
	}

}
