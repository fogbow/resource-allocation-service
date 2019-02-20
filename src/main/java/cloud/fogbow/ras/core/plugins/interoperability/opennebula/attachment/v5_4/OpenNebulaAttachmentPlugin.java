package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.AttachmentInstance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaUnmarshallerContents;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin<CloudToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaAttachmentPlugin.class);
	
	private static final String CLOUD_NAME = "opennebula";
	private static final String DEFAULT_DEVICE_PREFIX = "vd";
    private static final String INSTANCE_ID= "%s %s %s";
	private static final String SEPARATOR_ID = " ";
	
	protected static final String OPENNEBULA_RPC_ENDPOINT_KEY = "opennebula_rpc_endpoint";

	@Deprecated
    private OpenNebulaClientFactory factory;
    private String endpoint;
    
	public OpenNebulaAttachmentPlugin() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		Properties properties = PropertiesUtil.readProperties(opennebulaConfFilePath);
		this.endpoint = properties.getProperty(OPENNEBULA_RPC_ENDPOINT_KEY);
	}

	@Deprecated
    public OpenNebulaAttachmentPlugin(String confFilePath) {
        this.factory = new OpenNebulaClientFactory(confFilePath);
    }

	@Override
	public String requestInstance(AttachmentOrder attachmentOrder, CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudToken));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudToken.getTokenValue());
		String virtualMachineId = attachmentOrder.getComputeId();
		String imageId = attachmentOrder.getVolumeId();

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
	public void deleteInstance(String attachmentInstanceId, CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, attachmentInstanceId, cloudToken));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudToken.getTokenValue());
		String[] instanceIds = attachmentInstanceId.split(SEPARATOR_ID);
		int virtualMachineId = Integer.parseInt(instanceIds[0]);
		int diskId = Integer.parseInt(instanceIds[2]);
		
		OneResponse response = VirtualMachine.diskDetach(client, virtualMachineId, diskId);
		if (response.isError()) {
			String message = response.getErrorMessage(); 
			String.format(Messages.Error.ERROR_WHILE_DETACHING_VOLUME, diskId, message);
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	@Override
	public AttachmentInstance getInstance(String attachmentInstanceId, CloudToken cloudToken) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudToken.getTokenValue());
		String[] instanceIds = attachmentInstanceId.split(SEPARATOR_ID);
		String virtualMachineId = instanceIds[0];
		String imageId = instanceIds[1];

		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
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
		
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.diskAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME, imageId, response.getMessage()));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		return virtualMachine;
	}
    
	@Deprecated
	public void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;		
	}
	
	protected String getEndpoint() {
		return endpoint;
	}

	protected void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	
}
