package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.HardwareRequirements;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.*;

public class OpenNebulaComputePlugin implements ComputePlugin<Token>{

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);
	
	private static final String USERDATA_ENCODING_CONTEXT = "base64";
	private static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	private static final String NETWORK_GRAPHIC_ADDRESS = "0.0.0.0";
	private static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	private static final String DEFAULT_VOLUME_TYPE = "fs";
	private static final String DEFAULT_NETWORK_ID = "";
	
	private OpenNebulaClientFactory factory;
	private List<HardwareRequirements> hardwareRequirements;
	
	public OpenNebulaComputePlugin() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public String requestInstance(ComputeOrder computeOrder, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.debug("Requesting instance with token=" + localUserAttributes.getTokenValue());
		Client client = factory.createClient(localUserAttributes.getTokenValue(), "endpoint");
		
		ComputeRequest request = new ComputeRequest.Builder()
				.contextEncoding(USERDATA_ENCODING_CONTEXT)
				.contextUserdata(computeOrder.getUserData().toString())
				.contextNetwork(NETWORK_CONFIRMATION_CONTEXT)
				.cpu(String.valueOf(computeOrder.getvCPU()))
				.graphicsListen(NETWORK_GRAPHIC_ADDRESS)
				.graphicsType(DEFAULT_GRAPHIC_TYPE)
				.imageId(computeOrder.getImageId())
				.volumeSize(String.valueOf(computeOrder.getDisk()))
				.volumeType(DEFAULT_VOLUME_TYPE)
				.memory(String.valueOf(computeOrder.getMemory()))
				.networkId(DEFAULT_NETWORK_ID)
				.build();
		
		String template = request.getVirtualMachine().generateTemplate();
		return factory.allocateVirtualMachine(client, template);
	}

	@Override
	public ComputeInstance getInstance(String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.debug("Getting instance " + computeInstanceId + " of token: " + localUserAttributes.getTokenValue());
		if (this.hardwareRequirements == null || this.hardwareRequirements.isEmpty() ) {
			updateHardwareRequirements(localUserAttributes);
		}
		Client client = factory.createClient(localUserAttributes.getTokenValue(), "endpoint");
		VirtualMachine virtualMachine = factory.createVirtualMachine(client, computeInstanceId);
		return createVirtualMachineInstance(virtualMachine);
	}

	@Override
	public void deleteInstance(String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.debug("Removing instanceId " + computeInstanceId + " with token " + localUserAttributes);
		Client client = factory.createClient(localUserAttributes.getTokenValue(), "endpoint");
		VirtualMachine virtualMachine = factory.createVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.terminate();
		if (response.isError()) {			
			LOGGER.error("Error while removing vm: " + response.getErrorMessage());
		}
	}

	private void updateHardwareRequirements(Token localUserAttributes) {
		// TODO Implement flavors... Auto-generated method stub	
	}
	
	private ComputeInstance createVirtualMachineInstance(VirtualMachine virtualMachine) {
		String id = virtualMachine.getId();
		String cpu = virtualMachine.xpath("TEMPLATE/CPU");
		String disk = virtualMachine.xpath("TEMPLATE/DISK/SIZE");
		String memory = virtualMachine.xpath("TEMPLATE/MEMORY");
		String privateIp = virtualMachine.xpath("TEMPLATE/NIC/IP");

		String state = virtualMachine.lcmStateStr();

		LOGGER.debug("Mounting instance structure of instanceId: " + id);
		ComputeInstance computeInstance = new ComputeInstance(id, InstanceState.valueOf(state), "host-name",
				Integer.parseInt(cpu), Integer.parseInt(memory), Integer.parseInt(disk), privateIp);
		
		return computeInstance;
	}
}
