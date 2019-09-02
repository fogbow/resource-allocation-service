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
	
	private static final String TARGET_PATH_FORMAT = "//TEMPLATE/DISK[%s]/TARGET";
	private static final String IMAGE_ID_PATH_FORMAT = "//TEMPLATE/DISK[%s]/IMAGE_ID";
	private static final String DEFAULT_TARGET = "hdb";
	private static final String DEVICE_PATH_SEPARATOR = "/";
	private static final int ATTEMPTS_LIMIT_NUMBER = 5;
	private static final int TARGET_INDEX = 2;
	protected static final String POWEROFF_STATE = "POWEROFF";

	protected static final long ONE_POINT_TWO_SECONDS = 1200;

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
	public String requestInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		String virtualMachineId = attachmentOrder.getComputeId();
		String imageId = attachmentOrder.getVolumeId();
		String[] targetList = attachmentOrder.getDevice().split(DEVICE_PATH_SEPARATOR);
		// NOTE(pauloewerton): expecting a default target device such as /dev/sdb
		String target = targetList.length == 3 ? targetList[TARGET_INDEX] : DEFAULT_TARGET;

		CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
				.imageId(imageId)
                .target(target)
				.build();
		
		String template = request.getAttachDisk().marshalTemplate();
		
		VirtualMachine virtualMachine = doRequestInstance(client, virtualMachineId, imageId, template);
		String diskId = getDiskIdFromContenOf(virtualMachine);
		return diskId;
	}

	@Override
	public void deleteInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
		if (order == null) {
			throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
		}

		String virtualMachineIdStr = order.getComputeId();
		int diskId = Integer.parseInt(order.getInstanceId());

		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineIdStr);
		
		detachVolume(virtualMachine, diskId);
	}

	@Override
	public AttachmentInstance getInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
		if (order == null) {
			throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
		}
		String virtualMachineId = order.getComputeId();
		String imageId = order.getVolumeId();
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(imageId));
		String imageState = image.stateString();
		String target = null;

		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, order.getComputeId());

		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			String imgId = virtualMachine.xpath(String.format(IMAGE_ID_PATH_FORMAT, i));

			if (!imgId.equalsIgnoreCase(imageId)) continue;
			else {
				target = virtualMachine.xpath(String.format(TARGET_PATH_FORMAT, i));
				break;
			}
		}

		AttachmentInstance attachmentInstance = new AttachmentInstance(
				order.getInstanceId(),
				imageState,
				virtualMachineId, 
				imageId, 
				target);
		
		return attachmentInstance;
	}

	protected void detachVolume(VirtualMachine virtualMachine, int diskId) {
		OneResponse response = virtualMachine.diskDetach(diskId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_DETACHING_VOLUME, diskId, message));
		}
	}

	protected boolean isPowerOff(VirtualMachine virtualMachine) {
		String state;
		int count = 0;
		while (count < ATTEMPTS_LIMIT_NUMBER) {
			count++;
			waitMoment();
			virtualMachine.info();
			state = virtualMachine.stateStr();
			if (state.equalsIgnoreCase(POWEROFF_STATE)) {
				return true;
			}
		}
		return false;
	}

	protected void waitMoment() {
		try {
			Thread.sleep(ONE_POINT_TWO_SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
		}
	}
	
    protected String getDiskIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		String content = xmlUnmarshaller.getContentOfLastElement(OpenNebulaConstants.DISK_ID);
		return content;
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
}
