package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.NoAvailableResourcesException;
import org.fogbowcloud.ras.core.exceptions.QuotaExceededException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.group.Group;
import org.opennebula.client.group.GroupPool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaClientFactory {

    private final static Logger LOGGER = Logger.getLogger(OpenNebulaClientFactory.class);

    private static final String RESPONSE_NOT_AUTORIZED = "Not authorized";
    private static final String RESPONSE_DONE = "DONE";
    private static final String FIELD_RESPONSE_LIMIT = "limit";
    private static final String FIELD_RESPONSE_QUOTA = "quota";
    private static final String RESPONSE_NOT_ENOUGH_FREE_MEMORY = "Not enough free memory";
    private static final String RESPONSE_NO_SPACE_LEFT_ON_DEVICE = "No space left on device";
    private static final String OPENNEBULA_RPC_ENDPOINT_URL = "opennebula_rpc_endpoint";
    
    private String endpoint;

	public OpenNebulaClientFactory() {
		Properties properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.endpoint = properties.getProperty(OPENNEBULA_RPC_ENDPOINT_URL);
	}

	public Client createClient(String federationTokenValue) throws UnexpectedException {
		try {
			return new Client(federationTokenValue, this.endpoint);
		} catch (ClientConfigurationException e) {
			LOGGER.error(Messages.Error.ERROR_WHILE_CREATING_CLIENT, e);
			throw new UnexpectedException();
		}
	}

    public Group createGroup(Client client, int groupId) throws UnauthorizedRequestException {
    	GroupPool groupPool = new GroupPool(client);
    	groupPool.info();
    	Group group = groupPool.getById(groupId);
    	if (group == null){
			throw new UnauthorizedRequestException();
		}
    	group.info();		
		return group;
    }

	public ImagePool createImagePool(Client client) throws UnexpectedException {
		ImagePool imagePool = new ImagePool(client);
		OneResponse response = imagePool.infoAll();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_TEMPLATES, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		LOGGER.info(String.format(Messages.Info.TEMPLATE_POOL_LENGTH, imagePool.getLength()));
		return imagePool;
	}

	public VirtualMachine createVirtualMachine(Client client, String instanceId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		int id = 0;
		try {
			id = Integer.parseInt(instanceId);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_INSTANCE_ID, instanceId));
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
		VirtualMachine virtualMachine = new VirtualMachine(id, client);
		OneResponse response = virtualMachine.info();

		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(message);
			// Not authorized to perform
			if (message.contains(RESPONSE_NOT_AUTORIZED)) {
				throw new UnauthorizedRequestException();
			}
			// Error getting virtual machine
			throw new InstanceNotFoundException(message);
		} else if (RESPONSE_DONE.equals(virtualMachine.stateStr())) {
			// The instance is not active anymore
			throw new InstanceNotFoundException();
		}
		return virtualMachine;
	}

	public VirtualMachinePool createVirtualMachinePool(Client client) throws UnexpectedException {
		VirtualMachinePool virtualMachinePool = new VirtualMachinePool(client);
		OneResponse response = virtualMachinePool.info();
		if (response.isError()) {
			LOGGER.error(response.getErrorMessage());
			throw new UnexpectedException();
		}
		return virtualMachinePool;
	}

	public VirtualNetwork createVirtualNetwork(Client client, String instanceId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		int id = 0;
		try {
			id = Integer.parseInt(instanceId);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_INSTANCE_ID, instanceId));
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
		VirtualNetwork virtualNetwork = new VirtualNetwork(id, client);
		OneResponse response = virtualNetwork.info();

		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(message);
			// Not authorized to perform
			if (message.contains(RESPONSE_NOT_AUTORIZED)) {
				throw new UnauthorizedRequestException();
			}
			// Error getting virtual network
			throw new InstanceNotFoundException(message);
		}
		return virtualNetwork;
	}

	public TemplatePool createTemplatePool(Client client) throws UnexpectedException {
		TemplatePool templatePool = new TemplatePool(client);
		OneResponse response = templatePool.infoAll();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_TEMPLATES, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		LOGGER.info(String.format(Messages.Info.TEMPLATE_POOL_LENGTH, templatePool.getLength()));
		return templatePool;
	}

    public User createUser(Client client, String username) throws UnauthorizedRequestException {
    	UserPool userpool = new UserPool(client);
 		userpool.info();
 		String userId = findUserByName(userpool, username);
 		int id = Integer.parseInt(userId);
 		User user = userpool.getById(id);
 		user.info();
 		return user;
    }

	public String allocateImage(Client client, String template, Integer datastoreId) throws InvalidParameterException {
		OneResponse response = Image.allocate(client, template, datastoreId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_IMAGE, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException(message);
		}
		Image.chmod(client, response.getIntMessage(), 744);
		return response.getMessage();
	}

	public String allocateVirtualNetwork(Client client, String template) throws InvalidParameterException {
		OneResponse response = VirtualNetwork.allocate(client, template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NETWORK, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		VirtualNetwork.chmod(client, response.getIntMessage(), 744);
		return response.getMessage();
	}

	public String allocateVirtualMachine(Client client, String template)
			throws QuotaExceededException, NoAvailableResourcesException, InvalidParameterException {
		OneResponse response = VirtualMachine.allocate(client, template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_INSTANTIATING_FROM_TEMPLATE, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			if (message.contains(FIELD_RESPONSE_LIMIT) && message.contains(FIELD_RESPONSE_QUOTA)) {
				throw new QuotaExceededException();
			}
			if ((message.contains(RESPONSE_NOT_ENOUGH_FREE_MEMORY))
					|| (message.contains(RESPONSE_NO_SPACE_LEFT_ON_DEVICE))) {
				throw new NoAvailableResourcesException();
			}
			throw new InvalidParameterException(message);
		}
		return response.getMessage();
	}
	
	private String findUserByName(UserPool userpool, String username) throws UnauthorizedRequestException {
		for (User user : userpool) {
			if (username.equals(user.getName())){
				return user.getId();
			}
		}
		throw new UnauthorizedRequestException();
	}

}
