package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaTagNameConstants;
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
	
	private static final String DEFAULT_DEVICE_PREFIX = "vd";

	private static final int ATTEMPTS_LIMIT_NUMBER = 5;

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

		CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
				.imageId(imageId)
				.build();
		
		String template = request.getAttachDisk().marshalTemplate();
		
		VirtualMachine virtualMachine = attachVolumeImageDisk(client, virtualMachineId, imageId, template);
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
		
		// A volume can only be detached if a virtual machine is power-off.
		virtualMachine.poweroff(true);
		if (isPowerOff(virtualMachine)) {
			detachVolume(virtualMachine, diskId);
			virtualMachine.resume();
		} else {
			throw new UnexpectedException(Messages.Exception.UNEXPECTED_ERROR);
		}
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
		String imageDevice = image.xpath(DEFAULT_DEVICE_PREFIX);
		String imageState = image.stateString();

		AttachmentInstance attachmentInstance = new AttachmentInstance(
				order.getInstanceId(),
				imageState,
				virtualMachineId, 
				imageId, 
				imageDevice);
		
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
	
    private String getDiskIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		String content = xmlUnmarshaller.getContentOfLastElement(OpenNebulaTagNameConstants.DISK_ID);
		return content;
	}

	private VirtualMachine attachVolumeImageDisk(Client client, String virtualMachineId, String imageId, String template) 
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
