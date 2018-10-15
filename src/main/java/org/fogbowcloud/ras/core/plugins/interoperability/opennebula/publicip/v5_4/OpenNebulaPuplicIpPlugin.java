package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vrouter.VirtualRouter;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<Token> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private OpenNebulaClientFactory factory;

	public OpenNebulaPuplicIpPlugin(OpenNebulaClientFactory factory) {
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		// TODO create default virtual network with public ip associated... 
		
		String networkId = "";
		
		CreateNicRequest request = new CreateNicRequest.Builder().nicId(networkId).build();
		
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

}
