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
import org.opennebula.client.vnet.VirtualNetwork;
import org.opennebula.client.vrouter.VirtualRouter;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<Token> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);
	private static final String DEFAULT_NETWORK_ID = "default_network_id";
	private static final String DEFAULT_SECURITY_GROUPS_ID = "default_security_groups_id";

	private OpenNebulaClientFactory factory;
	private String networkId;
	private String securityGroupsId;

	public OpenNebulaPuplicIpPlugin(OpenNebulaClientFactory factory) {
		Properties properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.networkId = properties.getProperty(DEFAULT_NETWORK_ID);
		this.securityGroupsId = properties.getProperty(DEFAULT_SECURITY_GROUPS_ID);
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		int id = Integer.parseInt(this.networkId);
		VirtualNetwork virtualNetwork = new VirtualNetwork(id, client);
		addPublicIp(virtualNetwork);

		String sgId = updateSecurityGroups(client, virtualNetwork);
		CreateNicRequest request = new CreateNicRequest.Builder()
				.nicId(this.networkId)
				.sgId(sgId)
				.build();
		
		String template = request.getNic().generateTemplate();
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
		return null; // FIXME ...
	}

	private String updateSecurityGroups(Client client, VirtualNetwork virtualNetwork) {
		int id = Integer.parseInt(this.securityGroupsId);
		String template = ""; // FIXME ...
		SecurityGroup securityGroup = new SecurityGroup(id, client);
		OneResponse response = securityGroup.update(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
		return securityGroup.getId();
	}

	private void addPublicIp(VirtualNetwork virtualNetwork) {
		String type = null;
		String ip = null;
		String mac = null;
		String size = null;
		
		CreateAddressRangeRequest request = new CreateAddressRangeRequest.Builder()
				.type(type)
				.ip(ip)
				.mac(mac)
				.size(size)
				.build();
		
		String template = request.getAddressRange().generateTemplate();
		OneResponse response = virtualNetwork.addAr(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId,
				localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		// TODO ...
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(
				String.format(Messages.Info.GETTING_INSTANCE, publicIpInstanceId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		// TODO ...
		return null;
	}

	private PublicIpInstance createVirtualRouterInstance(VirtualRouter virtualRouter) {
		String id = virtualRouter.getId();
		InstanceState instanceState = InstanceState.READY;
		String ip = virtualRouter.xpath("VROUTER/AR_POOL/AR/IP"); // FIXME

		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, id));
		PublicIpInstance publicIpInstance = new PublicIpInstance(id, instanceState, ip);

		return publicIpInstance;
	}

}
