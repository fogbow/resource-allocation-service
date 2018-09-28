package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaNetworkPlugin implements NetworkPlugin<Token> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaNetworkPlugin.class);

	private static final String TEMPLATE_NETWORK_ADDRESS_PATH = "TEMPLATE/NETWORK_ADDRESS";
	private static final String TEMPLATE_NETWORK_GATEWAY_PATH = "TEMPLATE/NETWORK_GATEWAY";
	private static final String TEMPLATE_VLAN_ID_PATH = "TEMPLATE/VLAN_ID";
	
	private OpenNebulaClientFactory factory;
	
	public OpenNebulaNetworkPlugin() {
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(NetworkOrder networkOrder, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.debug("Requesting network instance with token=" + localUserAttributes.getTokenValue()); // FIXME
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		String template = "";
		LOGGER.debug("The network instance will be allocated according to template: " + template); // FIXME
		return this.factory.allocateVirtualNetwork(client, template);
	}

	@Override
	public NetworkInstance getInstance(String networkInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info("Getting network instance ID=" + networkInstanceId + " and token=" + localUserAttributes.getTokenValue()); // FIXME
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, networkInstanceId);
		return createInstance(virtualNetwork);
	}

	@Override
	public void deleteInstance(String networkInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info("Removing network instance ID=" + networkInstanceId + " and token=" + localUserAttributes.getTokenValue()); // FIXME
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, networkInstanceId);
		OneResponse response = virtualNetwork.delete();
		if (response.isError()) {
			LOGGER.error("Error occurred while trying to delete network instance with ID=" 
					+ networkInstanceId + "; " + response.getErrorMessage()); // FIXME
		}
	}

	private NetworkInstance createInstance(VirtualNetwork virtualNetwork) {
		String id = virtualNetwork.getId();
		String name = virtualNetwork.getName();
		String address = virtualNetwork.xpath(TEMPLATE_NETWORK_ADDRESS_PATH);
		String gateway = virtualNetwork.xpath(TEMPLATE_NETWORK_GATEWAY_PATH);
		String vLan = virtualNetwork.xpath(TEMPLATE_VLAN_ID_PATH);
		String networkInterface = null;
		String macInterface = null;
		String interfaceState = null;
		
		InstanceState instanceState = InstanceState.READY;
		NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;
		
		NetworkInstance networkInstance = new NetworkInstance(
				id, 
				instanceState, 
				name, 
				address, 
				gateway, 
				vLan, 
				allocationMode, 
				networkInterface, 
				macInterface, 
				interfaceState);
		
		return networkInstance;
	}
	
}
