package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.group.Group;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaClientFactory {

	private final static Logger LOGGER = Logger.getLogger(OpenNebulaClientFactory.class);

	public Client createClient(String accessId, String openNebulaEndpoint) throws UnexpectedException {
		try {
			return new Client(accessId, openNebulaEndpoint);
		} catch (ClientConfigurationException e) {
			LOGGER.error(Messages.Error.ERROR_WHILE_CREATING_CLIENT, e);
			throw new UnexpectedException();
		}
	}
	
	public Group createGroup(Client oneClient, int groupId) {
		return null;
	}
	
	public ImagePool createImagePool(Client oneClient) {
		return null;
	}
	
	public VirtualMachine createVirtualMachine(Client oneClient, String instanceIdStr) {
		return null;
	}
	
	public VirtualMachinePool createVirtualMachinePool(Client oneClient) {
		return null;
	}
	
	public VirtualNetwork createVirtualNetwork (Client oneClient, String instanceIdStr) {
		return null;
	}
	
	public TemplatePool createTemplatePool(Client oneClient) {
		return null;
	}
	
	public User createUser(Client oneClient, String username) {
		return null;
	}
	
	public String allocateImage(Client oneClient, String vmTemplate, Integer datastoreId) {
		return null;
	}
	
	public String allocateNetwork(Client oneClient, String networkTemplate) {
		return null;
	}
	
	public String allocateVirtualMachine(Client oneClient, String vmTemplate) {
		return null;
	}
	
}
