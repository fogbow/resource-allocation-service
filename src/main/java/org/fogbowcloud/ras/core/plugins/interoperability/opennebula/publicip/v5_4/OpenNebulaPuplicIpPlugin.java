package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vrouter.VirtualRouter;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<Token> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private static final String DEFAULT_NETWORK_BRIDGE = "default_network_bridge";

	private OpenNebulaClientFactory factory;
	private String bridge;

	public OpenNebulaPuplicIpPlugin(OpenNebulaClientFactory factory) {
		Properties properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);
		
		this.bridge = properties.getProperty(DEFAULT_NETWORK_BRIDGE);
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		// TODO create default virtual network with public ip associated... 
		
		String nicId = PublicIpNetwork.createDefaultVirtualNetwork();
		
		CreateNicRequest request = new CreateNicRequest.Builder().nicId(nicId).build();
		String template = request.getNic().generateTemplate();
				
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
		
		// TODO ...
		return null;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		// TODO ...
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, publicIpInstanceId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		// TODO ...
		return null;
	}

	private PublicIpInstance createVirtualRouterInstance(VirtualRouter virtualRouter) {
		String id = virtualRouter.getId();
		InstanceState instanceState = InstanceState.READY;
		String ip = virtualRouter.xpath("VROUTER/AR_POOL/AR/IP"); // FIXME
		
		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, id));
		PublicIpInstance publicIpInstance = new PublicIpInstance(
				id, 
				instanceState, 
				ip);
		
		return publicIpInstance;
	}

	public static class PublicIpNetwork {

		public static String createDefaultVirtualNetwork() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
}
