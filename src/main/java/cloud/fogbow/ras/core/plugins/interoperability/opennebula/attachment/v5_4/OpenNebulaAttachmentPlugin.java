package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaUnmarshallerContents;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaAttachmentPlugin.class);
	
	private static final String DEFAULT_DEVICE_PREFIX = "vd";
    private static final String INSTANCE_ID= "%s %s %s";
	private static final String SEPARATOR_ID = " ";

    private OpenNebulaClientFactory factory;

    public OpenNebulaAttachmentPlugin(String confFilePath) {
        this.factory = new OpenNebulaClientFactory(confFilePath);
    }

	@Override
	public String requestInstance(AttachmentOrder attachmentOrder, CloudToken localUserAttributes) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes));
		String virtualMachineId = attachmentOrder.getComputeId();
		String imageId = attachmentOrder.getVolumeId();

		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
				.imageId(imageId)
				.build();
		
		String template = request.getAttachDisk().marshalTemplate();
		VirtualMachine virtualMachine = attachVolumeImageDisk(client, virtualMachineId, imageId, template);
		
		String diskId = getDiskIdFromContenOf(virtualMachine);
		String instanceId = String.format(INSTANCE_ID, virtualMachineId, imageId, diskId);

		return instanceId;
	}

	@Override
	public void deleteInstance(String attachmentInstanceId, CloudToken localUserAttributes) throws FogbowException {
		
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, attachmentInstanceId, localUserAttributes));
		String[] instanceIds = attachmentInstanceId.split(SEPARATOR_ID);
		int virtualMachineId = Integer.parseInt(instanceIds[0]);
		int diskId = Integer.parseInt(instanceIds[2]);

		Client client = factory.createClient(localUserAttributes.getTokenValue());
		OneResponse response = VirtualMachine.diskDetach(client, virtualMachineId, diskId);
		if (response.isError()) {
			String message = response.getErrorMessage(); 
			String.format(Messages.Error.ERROR_WHILE_DETACHING_VOLUME, diskId, message);
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	@Override
	public AttachmentInstance getInstance(String attachmentInstanceId, CloudToken localUserAttributes) throws FogbowException {
		
		String[] instanceIds = attachmentInstanceId.split(SEPARATOR_ID);
		String virtualMachineId = instanceIds[0];
		String imageId = instanceIds[1];

		Client client = factory.createClient(localUserAttributes.getTokenValue());

		ImagePool imagePool = this.factory.createImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(imageId));
		String imageDevice = image.xpath(DEFAULT_DEVICE_PREFIX);
		String imageState = image.stateString();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, imageState);

		AttachmentInstance attachmentInstance = new AttachmentInstance(
				attachmentInstanceId, 
				instanceState, 
				virtualMachineId, 
				imageId, 
				imageDevice);
		
		return attachmentInstance;
	}

    private String getDiskIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		OpenNebulaUnmarshallerContents unmarshallerContents = new OpenNebulaUnmarshallerContents(xml);
		String content = unmarshallerContents.getContentOfLastElement(OpenNebulaTagNameConstants.DISK_ID);
		return content;
	}

	private VirtualMachine attachVolumeImageDisk(Client client, String virtualMachineId, String imageId, String template) 
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.diskAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME, imageId, response.getMessage()));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		return virtualMachine;
	}
    
	public void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;		
	}
}
