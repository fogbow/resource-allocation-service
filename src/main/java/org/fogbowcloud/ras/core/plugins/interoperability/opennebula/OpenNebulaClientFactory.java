package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import java.security.InvalidParameterException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.NoAvailableResourcesException;
import org.fogbowcloud.ras.core.exceptions.QuotaExceededException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.group.Group;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
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
    private static final String OPENNEBULA_AAA_TOKEN = "opennebula_aaa_token";
    private Properties properties;

    public Client createClient() throws UnexpectedException {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);
        try {
            String tokenValue = properties.getProperty(OPENNEBULA_AAA_TOKEN);
            String endpoint = properties.getProperty(OPENNEBULA_RPC_ENDPOINT_URL);
            return new Client(tokenValue, endpoint);
        } catch (ClientConfigurationException e) {
            LOGGER.error(Messages.Error.ERROR_WHILE_CREATING_CLIENT, e);
            throw new UnexpectedException();
        }
    }

    public Group createGroup(Client client, int groupId) {
        return null;
    }

    public ImagePool createImagePool(Client client) {
        return null;
    }

    public VirtualMachine createVirtualMachine(Client client, String instanceId) throws UnauthorizedRequestException, InstanceNotFoundException {
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
            //Not authorized to perform
            if (message.contains(RESPONSE_NOT_AUTORIZED)) {
                throw new UnauthorizedRequestException();
            }
            //Error getting virtual machine
            throw new InstanceNotFoundException(message);
        } else if (RESPONSE_DONE.equals(virtualMachine.stateStr())) {
            //The instance is not active anymore
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

    public VirtualNetwork createVirtualNetwork(Client client, String instanceIdStr) {
        return null;
    }

    public TemplatePool createTemplatePool(Client client) {
        return null;
    }

    public User createUser(Client client, String username) {
        return null;
    }

    public String allocateImage(Client client, String template, Integer datastoreId) {
        OneResponse response = Image.allocate(client, "Some decription for this image", (int) datastoreId);
        if(response.isError()){
            // TODO

        }
        return null;
    }

    public String allocateVirtualNetwork(Client client, String template) {
        return null;
    }

    public String allocateVirtualMachine(Client oneClient, String template) throws QuotaExceededException, NoAvailableResourcesException {
        OneResponse response = VirtualMachine.allocate(oneClient, template);
        if (response.isError()) {
            String message = response.getErrorMessage();
            LOGGER.error(String.format(Messages.Error.ERROR_WHILE_INSTANTIATING_FROM_TEMPLATE, template));
            LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
            if (message.contains(FIELD_RESPONSE_LIMIT) && message.contains(FIELD_RESPONSE_QUOTA)) {
                throw new QuotaExceededException();
            }
            if ((message.contains(RESPONSE_NOT_ENOUGH_FREE_MEMORY)) || (message.contains(RESPONSE_NO_SPACE_LEFT_ON_DEVICE))) {
                throw new NoAvailableResourcesException();
            }
            throw new InvalidParameterException(message);
        }
        return response.getMessage();
    }

}
