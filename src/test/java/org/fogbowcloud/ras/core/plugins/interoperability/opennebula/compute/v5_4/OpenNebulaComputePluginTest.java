package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.HardwareRequirements;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaComputePluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	
	private OpenNebulaClientFactory factory;
	private OpenNebulaComputePlugin plugin;
	private TreeSet<HardwareRequirements> flavors;
	
	@Before
	public void setUp() {
		this.factory = Mockito.mock(OpenNebulaClientFactory.class);
		this.plugin = Mockito.spy(new OpenNebulaComputePlugin());
		this.flavors = Mockito.spy(new TreeSet<>());
		
	}
	
	// Test case: ...
	@Test (expected = UnexpectedException.class) // verify
	public void testRequestInstanceThrowUnespectedExceptionWhenCallCreateClient() throws UnexpectedException, FogbowRasException {
		// set up
		ComputeOrder computeOrder = new ComputeOrder();
		Token token = new Token(LOCAL_TOKEN_VALUE);
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);
		
		// exercise
		this.plugin.requestInstance(computeOrder, token);
	}
	
	// Test case: ...
	@Test
	public void testRequestInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		Token token = new Token(LOCAL_TOKEN_VALUE);
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		ComputeOrder computeOrder = createComputeOrder();
		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
				Mockito.any(Token.class));
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
	}

	// Test case: ...
	@Test
	public void testDeleteInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		Token token = new Token(LOCAL_TOKEN_VALUE);
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = "fake-instance-id";
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).terminate();
	}
	
	// Test case: ...
	@Test
	public void testGetInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		Token token = new Token(LOCAL_TOKEN_VALUE);
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);
		
		HardwareRequirements flavor = createHardwareRequirements();
		this.flavors.add(flavor);
		this.plugin.setFlavors(flavors);
		
		String instanceId = "fake-instance-id";
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);
		
		ComputeInstance computeInstance = CreateComputeInstance();
		Mockito.doReturn(computeInstance).when(this.plugin).createVirtualMachineInstance(virtualMachine);
		
		// exercise
		this.plugin.getInstance(instanceId, token);
		
		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).createVirtualMachineInstance(virtualMachine);
	}

	private ComputeInstance CreateComputeInstance() {
		int cpu = 2;
		int ram = 1024;
		int disk = 8;
		
		String id = "fake-instance-id";
		String hostName = "fake-host-name";
		String image = "fake-image";
		String publicKey = "fake-public-key";
		String userData = "fake-user-data";
		
		InstanceState state = InstanceState.READY;
		List<String> ipAddresses = null;
		
		ComputeInstance computeInstance = new ComputeInstance(
				id, 
				state, 
				hostName, 
				cpu, 
				ram, 
				disk, 
				ipAddresses, 
				image, 
				publicKey, 
				userData);
		
		return computeInstance;
	}
	
	private HardwareRequirements createHardwareRequirements() {
		String name = "smallest-flavor";
		
		int cpu = 2;
		int ram = 1024;
		int disk = 8;
		
		return new HardwareRequirements(name, null, cpu, ram, disk);
	}

	private ComputeOrder createComputeOrder() {
		int cpu = 2;
		int memory = 1024;
		int disk = 8;
		
		String imageId = "fake-image-id";
		String publicKey = "fake-public-key";
		String privateNetworkId = "fake-private-network-id";
		
		UserData userData = new UserData();
		List<String> networksId = new ArrayList<>();
		networksId.add(privateNetworkId);
		
		ComputeOrder computeOrder = new ComputeOrder(
				null, 
				null, 
				null, 
				null, 
				cpu, 
				memory, 
				disk, 
				imageId, 
				userData, 
				publicKey, 
				networksId);
		
		return computeOrder;
	}
		
}
