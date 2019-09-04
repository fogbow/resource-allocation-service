package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import java.util.Properties;

import cloud.fogbow.common.exceptions.*;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

import cloud.fogbow.common.constants.OpenNebulaConstants;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.XmlUnmarshaller;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaAttachmentPlugin.class);
	
	protected static final String TARGET_PATH_FORMAT = "//TEMPLATE/DISK[%s]/TARGET";
	protected static final String IMAGE_ID_PATH_FORMAT = "//TEMPLATE/DISK[%s]/IMAGE_ID";
	private static final String DEFAULT_TARGET = "hdb";
	private static final String DEVICE_PATH_SEPARATOR = "/";
	private static final int TARGET_INDEX = 2;
	private static final int MAX_DISK_NUMBER_SEARCH = 100;

	private String endpoint;

	public OpenNebulaAttachmentPlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
	}

	@Override
	public boolean isReady(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
		// NOTE(pauloewerton): expecting a default target device such as /dev/sdb
		String[] targetList = order.getDevice().split(DEVICE_PATH_SEPARATOR);
		String target = targetList.length == 3 ? targetList[TARGET_INDEX] : DEFAULT_TARGET;
		String imageId = order.getVolumeId();

		CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
				.imageId(imageId)
                .target(target)
				.build();

		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		String virtualMachineId = order.getComputeId();
		String template = request.getAttachDisk().marshalTemplate();
		VirtualMachine virtualMachine = doRequestInstance(client, virtualMachineId, imageId, template);
		String diskId = getDiskIdFromContentOf(virtualMachine);

		return diskId;
	}

	@Override
	public void deleteInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
		if (order == null) {
			throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
		}

		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		String computeId = order.getComputeId();
		int diskId = Integer.parseInt(order.getVolumeId());

		this.doDeleteInstance(client, computeId, diskId);
	}

	@Override
	public AttachmentInstance getInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
		if (order == null) {
			throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
		}

		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		String instanceId = order.getInstanceId();
		String computeId = order.getComputeId();
		String volumeId = order.getVolumeId();

		return this.doGetInstance(client, instanceId, computeId, volumeId);
	}

	protected VirtualMachine doRequestInstance(Client client, String virtualMachineId, String imageId, String template)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.diskAttach(template);

		if (response.isError()) {
			String message = String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME, imageId, response.getMessage());
			LOGGER.error(message);
			throw new InvalidParameterException(message);
		}

		return virtualMachine;
	}

	protected void doDeleteInstance(Client client, String computeId, int diskId) throws UnauthorizedRequestException,
			InstanceNotFoundException, InvalidParameterException, UnexpectedException {
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeId);
		OneResponse response = virtualMachine.diskDetach(diskId);

		if (response.isError()) {
			String message = String.format(Messages.Error.ERROR_WHILE_DETACHING_VOLUME, diskId, response.getMessage());
			LOGGER.error(message);
			throw new UnexpectedException(message);
		}
	}

	protected AttachmentInstance doGetInstance(Client client, String instanceId, String computeId, String volumeId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException, UnexpectedException {
		String device = this.getTargetDevice(client, computeId, volumeId);
		String state = this.getState(client, volumeId);

		AttachmentInstance attachmentInstance = new AttachmentInstance(
				instanceId, state, computeId, volumeId, device);

		return attachmentInstance;
	}

	protected String getDiskIdFromContentOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		String content = xmlUnmarshaller.getContentOfLastElement(OpenNebulaConstants.DISK_ID);

		return content;
	}

	protected String getTargetDevice(Client client, String computeId, String volumeId) throws UnauthorizedRequestException,
			InstanceNotFoundException, InvalidParameterException {
		String device = null;
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeId);

		for (int i = 1; i < MAX_DISK_NUMBER_SEARCH; i++) {
			String imageId = virtualMachine.xpath(String.format(IMAGE_ID_PATH_FORMAT, i));

			if (!imageId.equalsIgnoreCase(volumeId)) continue;

			device = virtualMachine.xpath(String.format(TARGET_PATH_FORMAT, i));
            break;
		}

		if (device == null) throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);

		return device;
	}

	protected String getState(Client client, String volumeId) throws UnexpectedException {
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeId));

		return image.stateString();
	}
}
