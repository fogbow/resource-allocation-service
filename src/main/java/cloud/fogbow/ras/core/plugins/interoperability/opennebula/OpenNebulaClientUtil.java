package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.Pool;
import org.opennebula.client.PoolElement;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.group.Group;
import org.opennebula.client.group.GroupPool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;
import org.opennebula.client.vnet.VirtualNetworkPool;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;

public class OpenNebulaClientUtil {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaClientUtil.class);
	
	protected static final String FIELD_RESPONSE_LIMIT = "limit";
	protected static final String FIELD_RESPONSE_QUOTA = "quota";
	protected static final String RESPONSE_NOT_AUTHORIZED = "Not authorized";
	protected static final String RESPONSE_DONE = "DONE";
	protected static final String RESPONSE_NOT_ENOUGH_FREE_MEMORY = "Not enough free memory";
	protected static final String RESPONSE_NO_SPACE_LEFT_ON_DEVICE = "No space left on device";
	private static final int RESOURCE_BELONGS_TO_USER_FILTER = -3;

	public static Client instance;
	
	private static final int CHMOD_PERMISSION_744 = 744;
	
	public static Client createClient(String endpoint, String tokenValue) throws UnexpectedException {
		try {
			synchronized (Client.class) {
				if (instance == null) {
					instance = new Client(tokenValue, endpoint);
				}
				return instance;
			}
		} catch (ClientConfigurationException e) {
			LOGGER.error(Messages.Error.ERROR_WHILE_CREATING_CLIENT, e);
			throw new UnexpectedException();
		}
	}
	
	public static Group getGroup(Client client, int groupId) throws UnauthorizedRequestException, UnexpectedException {
		GroupPool groupPool = (GroupPool) generateOnePool(client, GroupPool.class);
    	OneResponse response = groupPool.info();
        if (response.isError()) {
            LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_GROUP, response.getErrorMessage()));
            throw new UnexpectedException(response.getErrorMessage());
        }
    	Group group = groupPool.getById(groupId);
    	if (group == null){
			throw new UnauthorizedRequestException();
		}
    	group.info();		
		return group;
    }

	public static VirtualNetworkPool getNetworkPoolByUser(Client client) throws UnexpectedException {
		VirtualNetworkPool networkPool = new VirtualNetworkPool(client, RESOURCE_BELONGS_TO_USER_FILTER);
		OneResponse response = networkPool.info();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_USERS, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		return networkPool;
	}
	
	public static Image getImage(Client client, String imageId) throws InvalidParameterException,
			UnauthorizedRequestException, InstanceNotFoundException {
		
		Image image = (Image) generateOnePoolElement(client, imageId, Image.class);
		OneResponse response = image.info();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(message);
			// Not authorized to perform
			if (message.contains(RESPONSE_NOT_AUTHORIZED)) {
				throw new UnauthorizedRequestException();
			}
			// Error getting virtual network
			throw new InstanceNotFoundException(message);
		}
		return image;
	}
	
	public static ImagePool getImagePool(Client client) throws UnexpectedException {
        ImagePool imagePool = (ImagePool) generateOnePool(client, ImagePool.class);
		OneResponse response = imagePool.infoAll();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_TEMPLATES, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		return imagePool;
	}

	public static DatastorePool getDatastorePool(Client client) throws UnexpectedException {
		DatastorePool datastorePool = (DatastorePool) generateOnePool(client, DatastorePool.class);
		OneResponse response = datastorePool.info();

		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_TEMPLATES, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}

		return datastorePool;
	}

	public static VirtualMachine getVirtualMachine(Client client, String virtualMachineId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		VirtualMachine virtualMachine = (VirtualMachine) generateOnePoolElement(client, virtualMachineId, VirtualMachine.class);
		OneResponse response = virtualMachine.info();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(message);
			// Not authorized to perform
			if (message.contains(RESPONSE_NOT_AUTHORIZED)) {
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
	
	public static VirtualNetwork getVirtualNetwork(Client client, String virtualNetworkId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		VirtualNetwork virtualNetwork = (VirtualNetwork) generateOnePoolElement(client, virtualNetworkId, VirtualNetwork.class);
		OneResponse response = virtualNetwork.info();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(message);
			// Not authorized to perform
			if (message.contains(RESPONSE_NOT_AUTHORIZED)) {
				throw new UnauthorizedRequestException();
			}
			// Error getting virtual network
			throw new InstanceNotFoundException(message);
		}
		return virtualNetwork;
	}

	public static TemplatePool getTemplatePool(Client client) throws UnexpectedException {
		TemplatePool templatePool = (TemplatePool) generateOnePool(client, TemplatePool.class);
		OneResponse response = templatePool.infoAll();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_TEMPLATES, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		return templatePool;
	}

	public static UserPool getUserPool(Client client) throws UnexpectedException {
		UserPool userpool = (UserPool) generateOnePool(client, UserPool.class);
 		OneResponse response = userpool.info();
 		if (response.isError()) {
 			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_GETTING_USERS, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
 		}
		return userpool;
	}

	public static User getUser(UserPool userPool, String userName)
			throws UnauthorizedRequestException, UnexpectedException {

		User user = findUserByName(userPool, userName);
		OneResponse response = user.info();
		if (response.isError()) {
			LOGGER.error(
					String.format(Messages.Error.ERROR_WHILE_GETTING_USER, user.getId(), response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		return user;
	}
    
    public static SecurityGroup getSecurityGroup(Client client, String securityGroupId)
    			throws UnauthorizedRequestException, InvalidParameterException, InstanceNotFoundException {

    	SecurityGroup securityGroup = (SecurityGroup) generateOnePoolElement(client, securityGroupId, SecurityGroup.class);
 		OneResponse response = securityGroup.info();
 		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(message);
			// Not authorized to perform
			if (message.contains(RESPONSE_NOT_AUTHORIZED)) {
				throw new UnauthorizedRequestException();
			}
			// Error getting virtual network
			throw new InstanceNotFoundException(message);
 		}
 		return securityGroup;
    }

	public static String allocateImage(Client client, String template, Integer datastoreId)
			throws InvalidParameterException {
		
		OneResponse response = Image.allocate(client, template, datastoreId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_IMAGE, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException(message);
		}
		Image.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}

	public static String allocateSecurityGroup(Client client, String template) throws InvalidParameterException {
		OneResponse response = SecurityGroup.allocate(client, template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_SECURITY_GROUPS, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		SecurityGroup.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}

	public static String allocateVirtualMachine(Client client, String template)
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
		VirtualMachine.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}

	public static String allocateVirtualNetwork(Client client, String template) throws InvalidParameterException {
		OneResponse response = VirtualNetwork.allocate(client, template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NETWORK, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		VirtualNetwork.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}
	
	public static String reserveVirtualNetwork(Client client, int id, String template) throws InvalidParameterException {
		OneResponse response = VirtualNetwork.reserve(client, id, template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NETWORK, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		VirtualNetwork.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}
	
	public static String updateVirtualNetwork(Client client, int id, String template) throws InvalidParameterException {
		boolean append = true;
		OneResponse response = VirtualNetwork.update(client, id, template, append);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_UPDATING_NETWORK, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		VirtualNetwork.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}

	private static User findUserByName(UserPool userPool, String userName) throws UnauthorizedRequestException {
		for (User user : userPool) {
			if (userName.equals(user.getName())){
				return user;
			}
		}
		throw new UnauthorizedRequestException();
	}
	
	protected static PoolElement generateOnePoolElement(Client client, String poolElementId, Class classType)
			throws InvalidParameterException {
		
		int id;
		try {
			id = Integer.parseInt(poolElementId);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_INSTANCE_ID, poolElementId), e);
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
		if (classType.isAssignableFrom(Image.class)) {
			return new Image(id, client);
		} else if (classType.isAssignableFrom(SecurityGroup.class)) {
			return new SecurityGroup(id, client);
		} else if (classType.isAssignableFrom(VirtualMachine.class)) {
			return new VirtualMachine(id, client);
		} else if (classType.isAssignableFrom(VirtualNetwork.class)) {
			return new VirtualNetwork(id, client);
		}
		return null;
	}
	
	protected static Pool generateOnePool(Client client, Class classType) {
		if (classType.isAssignableFrom(TemplatePool.class)) {
			return new TemplatePool(client);
		} else if (classType.isAssignableFrom(GroupPool.class)) {
			return new GroupPool(client);
        } else if (classType.isAssignableFrom(ImagePool.class)) {
            return new ImagePool(client);
        } else if (classType.isAssignableFrom(UserPool.class)) {
		    return new UserPool(client);
        } else if (classType.isAssignableFrom(DatastorePool.class)) {
			return new DatastorePool(client);
		}
		return null;
	}
}
