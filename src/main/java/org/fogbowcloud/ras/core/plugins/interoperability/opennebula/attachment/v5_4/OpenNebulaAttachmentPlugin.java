package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.AttachmentPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaUnmarshallerContents;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin<OpenNebulaToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaAttachmentPlugin.class);
	
	private static final String DEFAULT_DEVICE_PREFIX = "vd";
    private static final String INSTANCE_ID= "%s %s %s";
	private static final String SEPARATOR_ID = " ";

    private OpenNebulaClientFactory factory;

    public OpenNebulaAttachmentPlugin() {
        this.factory = new OpenNebulaClientFactory();
    }

	@Override
	public String requestInstance(AttachmentOrder attachmentOrder, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getUserName()));
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
	public void deleteInstance(String attachmentInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, attachmentInstanceId, localUserAttributes.getUserName()));
		String[] instanceIds = attachmentInstanceId.split(SEPARATOR_ID);
		int virtualMachineId = Integer.parseInt(instanceIds[0]);
		int diskId = Integer.parseInt(instanceIds[2]);

		Client client = factory.createClient(localUserAttributes.getTokenValue());
		OneResponse response = VirtualMachine.diskDetach(client, virtualMachineId, diskId);
		if (response.isError()) {
			String message = response.getErrorMessage(); 
			String.format(Messages.Error.ERROR_WHILE_DETACH_VOLUME, diskId, message);
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	@Override
	public AttachmentInstance getInstance(String attachmentInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
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
		String content = unmarshallerContents.unmarshalLastItemOf(OpenNebulaTagNameConstants.DISK_ID);
		return content;
	}

	private VirtualMachine attachVolumeImageDisk(Client client, String virtualMachineId, String imageId, String template) 
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.diskAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_ATTACH_VOLUME, imageId, response.getMessage()));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		return virtualMachine;
	}
    
	public void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;		
	}
}
