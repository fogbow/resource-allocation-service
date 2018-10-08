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

		CreatePublicIpRequest request = null; // TODO implement with builder patterns...

		String template = request.getVirtualRouter().generateTemplate();
		return this.factory.allocateVirtualRouter(client, template);
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(
				String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId, localUserAttributes.getTokenValue()));
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualRouter virtualRouter = this.factory.createVirtualRouter(client, publicIpInstanceId);
		OneResponse response = virtualRouter.delete();
		if (response.isError()) {
			LOGGER.error(
					String.format(Messages.Error.ERROR_WHILE_REMOVING_VR, publicIpInstanceId, response.getMessage()));
		}
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(
				String.format(Messages.Info.GETTING_INSTANCE, publicIpInstanceId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualRouter virtualRouter = this.factory.createVirtualRouter(client, publicIpInstanceId);
		return createVirtualRouterInstance(virtualRouter);
	}

	private PublicIpInstance createVirtualRouterInstance(VirtualRouter virtualRouter) {
		String id = virtualRouter.getId();
		int state = virtualRouter.state();
		InstanceState instanceState = null;
		String ip = virtualRouter.xpath(""); // FIXME
		
		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, id));
		PublicIpInstance publicIpInstance = new PublicIpInstance(
				id, 
				instanceState, 
				ip);
		
		return publicIpInstance;
	}

}
