package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.NoAvailableResourcesException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.HardwareRequirements;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;

public class OpenNebulaComputePluginTest {

	private static final String FAKE_HOST_NAME = "fake-host-name";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_IMAGE = "fake-image";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_IMAGE_KEY = "fake-image-key";
	private static final String FAKE_IMAGE_SIZE_VALUE = "fake-image-size-value";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String FAKE_PRIVATE_NETWORK_ID = "fake-private-network-id";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_USER_DATA = "fake-user-data";
	private static final String FLAVOR_KIND_NAME = "smallest-flavor";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String TEMPLATE_CPU = "2";
	private static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	private static final String TEMPLATE_DISK = "8";
	private static final String TEMPLATE_MEMORY = "1024";
	private static final String TEMPLATE_MEMORY_PATH = "TEMPLATE/MEMORY";
	
	private static final int CPU_VALUE = 4;
	private static final int MEMORY_VALUE = 2048;
	private static final int DISK_VALUE = 8;
	
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
	@Test
	public void testRequestInstanceSuccessfulWithNetworksId() throws UnexpectedException, FogbowRasException {
		// set up
		Token token = new Token(LOCAL_TOKEN_VALUE);
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = listNetworksId();
		int cpu = 2;
		int memory = 1024;
		int disk = 8;
		ComputeOrder computeOrder = createComputeOrder(networksId, cpu, memory, disk);
		
		Map<String, String> imageSizeMap = createImageSizeMap();
		Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		Template template = mockIteratorTemplatePool(client);

		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());
		
		Mockito.doReturn(DISK_VALUE).when(this.plugin).loadImageSizeDisk(imageSizeMap, template);
		
		String networkId = FAKE_PRIVATE_NETWORK_ID;
		String valueOfCpu = String.valueOf(2);
		String valueOfRam = String.valueOf(1024);
		String valueOfDisk = String.valueOf(8);
		String vmTemplate = generateTemplate(valueOfCpu, valueOfRam, networkId, valueOfDisk);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(3)).createClient(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
				Mockito.any(Token.class));
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client),
				Mockito.eq(vmTemplate));
	}
	
	// Test case: ...
	@Test
	public void testRequestInstanceSuccessfulWithoutNetworksId() throws UnexpectedException, FogbowRasException {
		// set up
		Token token = new Token(LOCAL_TOKEN_VALUE);
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = null;
		int cpu = 1;
		int memory = 2048;
		int disk = 8;
		
		ComputeOrder computeOrder = createComputeOrder(networksId,cpu, memory, disk);
		
		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		String networkId = null;
		String template = generateTemplate(String.valueOf(4), String.valueOf(2048), networkId, String.valueOf(8));

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
				Mockito.any(Token.class));
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
	}
	
	// Test case: ...
		@Test
		public void testRequestInstance() throws UnexpectedException, FogbowRasException {
			// set up
			Token token = new Token(LOCAL_TOKEN_VALUE);
			Client client = this.factory.createClient(token.getTokenValue());
			Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
			this.plugin.setFactory(this.factory);

			List<String> networksId = null;
			int cpu = 1;
			int memory = 2048;
			int disk = 8;
			
			ComputeOrder computeOrder = createComputeOrder(networksId,cpu, memory, disk);
			HardwareRequirements flavor = null;
			Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

//			String networkId = null;
//			String template = generateTemplate(String.valueOf(4), String.valueOf(2048), networkId, String.valueOf(8));

			// exercise
			this.plugin.requestInstance(computeOrder, token);

			// verify
			Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
			Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
					Mockito.any(Token.class));
//			Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
		}
	
	

	private Template mockIteratorTemplatePool(Client client) throws UnexpectedException {
		TemplatePool templatePool = Mockito.mock(TemplatePool.class);
		Mockito.doReturn(templatePool).when(this.factory).createTemplatePool(client);

		Template template = Mockito.mock(Template.class);
		Iterator<Template> templateIterator = Mockito.mock(Iterator.class);
		Mockito.when(templateIterator.hasNext()).thenReturn(true, false);
		Mockito.when(templateIterator.next()).thenReturn(template);
		Mockito.when(templatePool.iterator()).thenReturn(templateIterator);

		Mockito.when(template.getId()).thenReturn(FAKE_ID);
		Mockito.when(template.getName()).thenReturn(FAKE_NAME);
		Mockito.when(template.xpath(TEMPLATE_CPU_PATH)).thenReturn(TEMPLATE_CPU);
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(TEMPLATE_MEMORY);
		
		return template;
	}

	private Map<String, String> createImageSizeMap() {
		Map<String, String> imageSizeMap = new HashMap<>();
		imageSizeMap.put(FAKE_IMAGE_KEY, FAKE_IMAGE_SIZE_VALUE);
		return imageSizeMap;
	}

	private ComputeInstance CreateComputeInstance() {
		int cpu = CPU_VALUE;
		int ram = MEMORY_VALUE;
		int disk = DISK_VALUE;
		
		String id = FAKE_INSTANCE_ID;
		String hostName = FAKE_HOST_NAME;
		String image = FAKE_IMAGE;
		String publicKey = FAKE_PUBLIC_KEY;
		String userData = FAKE_USER_DATA;
		
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
		String id = FAKE_ID;
		String name = FLAVOR_KIND_NAME;
		
		int cpu = CPU_VALUE;
		int ram = MEMORY_VALUE;
		int disk = DISK_VALUE;
		
		return new HardwareRequirements(name, id, cpu, ram, disk);
	}

	private List<String> listNetworksId() {
		String privateNetworkId = FAKE_PRIVATE_NETWORK_ID;
		List<String> networksId = new ArrayList<>();
		networksId.add(privateNetworkId);
		return networksId;
	}
	
	private ComputeOrder createComputeOrder(List<String> networksId, int ...values) {
		int cpu = values[0];
		int memory = values[1];
		int disk = values[2];
		
		String imageId = FAKE_IMAGE_ID;
		String name = null, providingMember = null, requestingMember = null;
		String publicKey = FAKE_PUBLIC_KEY;
		
		FederationUserToken federationUserToken = null; 
		UserData userData = new UserData();
		
		ComputeOrder computeOrder = new ComputeOrder(
				federationUserToken, 
				requestingMember, 
				providingMember, 
				name, 
				cpu, 
				memory, 
				disk, 
				imageId, 
				userData, 
				publicKey, 
				networksId);
		
		return computeOrder;
	}
	
	private String generateTemplate(String ...args) {
		String cpu = args[0];
		String memory = args[1];
		String networkId = args[2] != null ? args[2] : String.valueOf(0);
		String size = args[3]; 
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <CONTEXT>\n" + 
				"        <USERDATA_ENCODING>base64</USERDATA_ENCODING>\n" + 
				"        <NETWORK>YES</NETWORK>\n" + 
				"    </CONTEXT>\n" + 
				"    <CPU>%s</CPU>\n" + 
				"    <GRAPHICS>\n" + 
				"        <LISTEN>0.0.0.0</LISTEN>\n" + 
				"        <TYPE>vnc</TYPE>\n" + 
				"    </GRAPHICS>\n" + 
				"    <DISK>\n" + 
				"        <IMAGE_ID>fake-image-id</IMAGE_ID>\n" + 
				"    </DISK>\n" + 
				"    <MEMORY>%s</MEMORY>\n" + 
				"    <NIC>\n" + 
				"        <NETWORK_ID>%s</NETWORK_ID>\n" + 
				"    </NIC>\n" + 
				"    <DISK>\n" + 
				"        <SIZE>%s</SIZE>\n" + 
				"        <TYPE>fs</TYPE>\n" + 
				"    </DISK>\n" + 
				"</TEMPLATE>\n";
		return String.format(template, cpu, memory, networkId, size);
	}
	
}
