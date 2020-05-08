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
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.aws.volume.v2.AwsVolumePlugin;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;

public class AwsAttachmentPlugin implements AttachmentPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsVolumePlugin.class);
	
	protected static final String DEFAULT_DEVICE_NAME = "/dev/sdb";
	protected static final String RESOURCE_NAME = "Attachment";

	private String region;

	public AwsAttachmentPlugin(String confFilePath) {
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
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String device = getAttachedDeviceName(attachmentOrder.getDevice());
		String instanceId = attachmentOrder.getComputeId();
		String volumeId = attachmentOrder.getVolumeId();
		
		AttachVolumeRequest request = AttachVolumeRequest.builder()
				.device(device)
				.instanceId(instanceId)
				.volumeId(volumeId)
				.build();

		return doRequestInstance(request, client);
	}

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, attachmentOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String volumeId = attachmentOrder.getVolumeId();
        doDeleteInstance(volumeId, client);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, attachmentOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String attachmentId = attachmentOrder.getInstanceId();
        return doGetInstance(attachmentId, client);
    }

    protected AttachmentInstance doGetInstance(String attachmentId, Ec2Client client) throws FogbowException {
        DescribeVolumesRequest request = DescribeVolumesRequest.builder()
                .volumeIds(attachmentId)
                .build();
        
        DescribeVolumesResponse response = AwsV2CloudUtil.doDescribeVolumesRequest(request, client);
        return buildAttachmentInstance(response);
    }

    protected void doDeleteInstance(String volumeId, Ec2Client client)
            throws UnexpectedException {
        
        DetachVolumeRequest request = DetachVolumeRequest.builder()
                .volumeId(volumeId)
                .build();
        try {
            client.detachVolume(request);
        } catch (Exception e) {
            String message = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME, volumeId);
            LOGGER.error(message, e);
            throw new UnexpectedException(message);
        }
    }

	protected AttachmentInstance buildAttachmentInstance(DescribeVolumesResponse response)
			throws FogbowException {

	    Volume volume = AwsV2CloudUtil.getVolumeFrom(response);
		VolumeAttachment attachment = getAttachmentBy(volume);
		String cloudState = attachment.stateAsString();
		String computeId = attachment.instanceId();
		String volumeId = attachment.volumeId();
		String device = attachment.device();
		return new AttachmentInstance(volumeId, cloudState, computeId, volumeId, device);
	}

	protected VolumeAttachment getAttachmentBy(Volume volume) throws FogbowException {
		if (!volume.attachments().isEmpty()) {
			return volume.attachments().listIterator().next();
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}
	
	protected String doRequestInstance(AttachVolumeRequest request, Ec2Client client)
            throws FogbowException {

        String attachmentId;
        try {
            client.attachVolume(request);
            attachmentId = request.volumeId();
        } catch (Exception e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
        return attachmentId;
    }

    /*
     * The device name defines the volume mount point. Amazon EC2 requires a device
     * name to be provided and creates a symbolic link to that name, although it may
     * change it depending on circumstances, as with other operating systems that
     * behave differently.
     */
    protected String getAttachedDeviceName(String deviceName) {
        /*
         * By default, "/dev/sd[a-z]" is provided as an example for Debian based linux
         * distributions, and "/dev/xvd[a-z]" for Red Hat based distributions and their
         * variants as CentOS.
         */
        return (deviceName != null && !deviceName.isEmpty()) ? deviceName : DEFAULT_DEVICE_NAME;
    }

}
