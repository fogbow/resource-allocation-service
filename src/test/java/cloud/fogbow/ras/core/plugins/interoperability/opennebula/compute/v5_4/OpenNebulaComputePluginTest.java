package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.vm.VirtualMachine;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaUnmarshallerContents;
import cloud.fogbow.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, VirtualMachine.class})
public class OpenNebulaComputePluginTest {

	private static final String DEFAULT_VIRTUAL_MACHINE_STATE = "Running";
	private static final String FAKE_HOST_NAME = "fake-host-name";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_IMAGE = "fake-image";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_IMAGE_KEY = "fake-image-key";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_IP_ADDRESS_ONE = "fake-ip-address-one";
	private static final String FAKE_IP_ADDRESS_TWO = "fake-ip-address-two";
	private static final String FAKE_NAME = "fake-name";
	private static final String FAKE_PRIVATE_NETWORK_ID = "fake-private-network-id";
	private static final String FAKE_PRIVATE_IP = "0.0.0.0";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FLAVOR_KIND_NAME = "smallest-flavor";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String IMAGE_SIZE_VALUE = "8";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String NIC_IP_EXPRESSION = "//NIC/IP";
	private static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	private static final String TEMPLATE_CPU_VALUE = "2";
	private static final String TEMPLATE_DISK_INDEX_IMAGE_PATH = "TEMPLATE/DISK[%s]/IMAGE";
	private static final String TEMPLATE_DISK_INDEX_PATH = "TEMPLATE/DISK[%s]";
	private static final String TEMPLATE_DISK_INDEX_SIZE_PATH = "TEMPLATE/DISK[%s]/SIZE";
	private static final String TEMPLATE_DISK_SIZE_PATH = "TEMPLATE/DISK/SIZE";
	private static final String TEMPLATE_MEMORY_PATH = "TEMPLATE/MEMORY";
	private static final String TEMPLATE_MEMORY_VALUE = "1024";
	private static final String TEMPLATE_NAME_PATH = "TEMPLATE/NAME";
	private static final String TEMPLATE_NIC_IP_PATH = "TEMPLATE/NIC/IP";
	private static final String UNCHECKED_VALUE = "unchecked";

	private static final String FAKE_NIC_IP_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
			+ "<TEMPLATE>\n" 
			+ "    <NIC>\n" 
			+ "        <IP>fake-ip-address-1</IP>\n" 
			+ "    </NIC>\n" 
			+ "    <NIC>\n" 
			+ "        <IP>fake-ip-address-2</IP>\n" 
			+ "    </NIC>\n" 
			+ "</TEMPLATE>\n";
	
	private static final String FAKE_USER_DATA = "fake-user-data";
	private static final UserData[] FAKE_USER_DATA_ARRAY = new UserData[] {
			new UserData(FAKE_USER_DATA, CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag") };
	
	private static final ArrayList<UserData> FAKE_LIST_USER_DATA = new ArrayList<>(Arrays.asList(FAKE_USER_DATA_ARRAY));

	private static final int CPU_VALUE_1 = 1;
	private static final int CPU_VALUE_2 = 2;
	private static final int CPU_VALUE_4 = 4;
	private static final int MEMORY_VALUE_1024 = 1024;
	private static final int MEMORY_VALUE_2048 = 2048;
	private static final int DISK_VALUE_8GB = 8;
	private static final int DEFAULT_NETWORK_ID = 0;

	private OpenNebulaComputePlugin plugin;
	private TreeSet<HardwareRequirements> flavors;

    @Before
	public void setUp() {
		this.plugin = Mockito.spy(new OpenNebulaComputePlugin());
		this.flavors = Mockito.spy(new TreeSet<>());
	}
	
	// test case: When calling the requestInstance method, with the valid client and
	// template with a list of networks ID, a virtual network will be allocated,
	// returned a instance ID.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testRequestInstanceSuccessfulWithNetworkIds() throws FogbowException, UnexpectedException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(IMAGE_SIZE_VALUE);
		
		HardwareRequirements flavor = createHardwareRequirements();
		this.flavors.add(flavor);
		this.plugin.setFlavors(this.flavors);
		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.eq(flavor), Mockito.eq(this.flavors));

		Template template = mockTemplatePoolIterator();
		Mockito.doReturn(DISK_VALUE_8GB).when(this.plugin).loadImageSizeDisk(Mockito.anyMap(), Mockito.eq(template));

		LaunchCommandGenerator mockLaunchCommandGenerator = Mockito.spy(new DefaultLaunchCommandGenerator());
		Mockito.doReturn(FAKE_USER_DATA).when(mockLaunchCommandGenerator).createLaunchCommand(Mockito.any());
		this.plugin.setLaunchCommandGenerator(mockLaunchCommandGenerator);
		
		List<String> networkIds = listNetworkIds();
		ComputeOrder computeOrder = createComputeOrder(networkIds, CPU_VALUE_2, MEMORY_VALUE_1024, DISK_VALUE_8GB);
		CloudToken token = createCloudToken();
		
		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(2));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
		
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
				Mockito.any(CloudToken.class));
		
		int choice = 0;
		String valueOfCpu = String.valueOf(CPU_VALUE_2);
		String valueOfRam = String.valueOf(MEMORY_VALUE_1024);
		String valueOfDisk = String.valueOf(DISK_VALUE_8GB);
		String defaultNetworkId = String.valueOf(DEFAULT_NETWORK_ID);
		String privateNetworkId = FAKE_PRIVATE_NETWORK_ID;
		String virtualMachineTemplate = generateTemplate(
				choice, 
				valueOfCpu, 
				valueOfRam, 
				defaultNetworkId, 
				privateNetworkId,
				valueOfDisk);
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateVirtualMachine(Mockito.eq(client),	Mockito.eq(virtualMachineTemplate));
	}
	
	// test case: When calling the requestInstance method, with the valid client and
	// template without a list of networks ID, a virtual network will be allocated,
	// returned a instance ID.
	@Test
	public void testRequestInstanceSuccessfulWithoutNetworksId() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString())).willReturn(client);


		List<String> networkIds = null;
		ComputeOrder computeOrder = createComputeOrder(networkIds, CPU_VALUE_1, MEMORY_VALUE_2048, DISK_VALUE_8GB);

		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		LaunchCommandGenerator mockLaunchCommandGenerator = Mockito.spy(new DefaultLaunchCommandGenerator());
		Mockito.doReturn(FAKE_USER_DATA).when(mockLaunchCommandGenerator).createLaunchCommand(Mockito.any());
		this.plugin.setLaunchCommandGenerator(mockLaunchCommandGenerator);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
				Mockito.any(CloudToken.class));
		
		int choice = 1;
		String networkId = null;
		String valueOfCpu = String.valueOf(CPU_VALUE_4);
		String valueOfRam = String.valueOf(MEMORY_VALUE_2048);
		String valueOfDisk = String.valueOf(DISK_VALUE_8GB);
		String template = generateTemplate(choice, valueOfCpu, valueOfRam, networkId, valueOfDisk);
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When calling getBestFlavor during requestInstance method, and it
	// returns a null instance, a NoAvailableResourcesException will be thrown.
	@SuppressWarnings(UNCHECKED_VALUE)
	@Test(expected = NoAvailableResourcesException.class) // verify
	public void testRequestInstanceThrowNoAvailableResourcesExceptionWhenCallGetBestFlavor()
			throws FogbowException {

		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(IMAGE_SIZE_VALUE);
		
		CloudToken token = createCloudToken();
		List<String> networkIds = null;
		ComputeOrder computeOrder = createComputeOrder(networkIds, CPU_VALUE_1, MEMORY_VALUE_2048, DISK_VALUE_8GB);

		// exercise
		this.plugin.requestInstance(computeOrder, token);
	}
	
	@Ignore
	// test case: When calling the getInstance method of a resource with the volatile disk size passing 
	// a valid client of a token value and an instance ID, it must return an instance of a virtual machine.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetInstanceSuccessfulWithVolatileDiskResource() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		//Client client = this.factory.createClient(token.getTokenValue());
		//Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		//this.plugin.setFactory(this.factory);
		this.plugin.setFlavors(this.flavors);

		Image image = Mockito.mock(Image.class);
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		//Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(IMAGE_SIZE_VALUE);

		Template template = mockTemplatePoolIterator();

		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());

		int index = 1;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_SIZE_PATH, index))).thenReturn(IMAGE_SIZE_VALUE);
		index++;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_PATH, index))).thenReturn(FAKE_NAME);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		//Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);

		Mockito.when(virtualMachine.getId()).thenReturn(FAKE_INSTANCE_ID);
		Mockito.when(virtualMachine.xpath(TEMPLATE_NAME_PATH)).thenReturn(FAKE_NAME);
		Mockito.when(virtualMachine.xpath(TEMPLATE_CPU_PATH)).thenReturn(TEMPLATE_CPU_VALUE);
		Mockito.when(virtualMachine.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(TEMPLATE_MEMORY_VALUE);
		Mockito.when(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn(IMAGE_SIZE_VALUE);
		Mockito.when(virtualMachine.lcmStateStr()).thenReturn(DEFAULT_VIRTUAL_MACHINE_STATE);
		Mockito.when(virtualMachine.xpath(TEMPLATE_NIC_IP_PATH)).thenReturn(FAKE_PRIVATE_IP);

		String xml = FAKE_NIC_IP_TEMPLATE;
		
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.getMessage()).thenReturn(xml);
		
		OpenNebulaUnmarshallerContents unmarshallerContents = Mockito.mock(OpenNebulaUnmarshallerContents.class);
		List<String> ipAddresses = new ArrayList<String>();
		ipAddresses.add(FAKE_IP_ADDRESS_ONE);
		ipAddresses.add(FAKE_IP_ADDRESS_TWO);
		Mockito.when(unmarshallerContents.getContextListOf(NIC_IP_EXPRESSION)).thenReturn(ipAddresses);
		
		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		//Mockito.verify(this.factory, Mockito.times(3)).createClient(Mockito.anyString());
		//Mockito.verify(this.plugin, Mockito.times(1)).getImageSizes(client);
		//Mockito.verify(this.factory, Mockito.times(1)).createTemplatePool(client);
		Mockito.verify(template, Mockito.times(1)).getId();
		Mockito.verify(template, Mockito.times(1)).getName();
		Mockito.verify(template, Mockito.times(9)).xpath(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(2)).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());
		//Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				//Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getComputeInstance(virtualMachine);
		Mockito.verify(virtualMachine, Mockito.times(1)).getId();
		Mockito.verify(virtualMachine, Mockito.times(4)).xpath(Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).lcmStateStr();
	}

	@Ignore
	// test case: When calling the getInstance method of a resource without the volatile disk size passing 
	// a valid client of a token value and an instance ID, it must return an instance of a virtual machine.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetInstanceSuccessfulWithoutVolatileDiskResource() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		//Client client = this.factory.createClient(token.getTokenValue());
		//Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		//this.plugin.setFactory(this.factory);
		this.plugin.setFlavors(this.flavors);

		Map<String, String> imageSizeMap = createImageSizeMap();
		//Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());

		Template template = mockTemplatePoolIterator();
		int index = 1;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_IMAGE_PATH, index))).thenReturn(FAKE_IMAGE_KEY);
		index++;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_PATH, index))).thenReturn(FAKE_NAME);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		//Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);

		ComputeInstance computeInstance = CreateComputeInstance();
		Mockito.doReturn(computeInstance).when(this.plugin).getComputeInstance(virtualMachine);

		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		//Mockito.verify(this.factory, Mockito.times(3)).createClient(Mockito.anyString());
		//Mockito.verify(this.plugin, Mockito.times(1)).getImageSizes(client);
		//Mockito.verify(this.factory, Mockito.times(1)).createTemplatePool(client);
		Mockito.verify(template, Mockito.times(1)).getId();
		Mockito.verify(template, Mockito.times(1)).getName();
		Mockito.verify(template, Mockito.times(9)).xpath(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(2)).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());
		Mockito.verify(this.plugin, Mockito.times(1)).loadImageSizeDisk(Mockito.eq(imageSizeMap), Mockito.eq(template));
		//Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				//Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getComputeInstance(virtualMachine);
	}
	
	@Ignore
	// Test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of virtual machine will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		//Client client = this.factory.createClient(token.getTokenValue());
		//Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		//this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		//Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate();
		Mockito.doReturn(true).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		//Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		//Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				//Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).terminate();
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	@Ignore
	// Test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		//Client client = this.factory.createClient(token.getTokenValue());
		//Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		//this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		//Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate();
		Mockito.doReturn(false).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		//Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		//Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				//Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).terminate();
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	@SuppressWarnings(UNCHECKED_VALUE)
	private Template mockTemplatePoolIterator() throws UnexpectedException {
		TemplatePool templatePool = Mockito.mock(TemplatePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getTemplatePool(Mockito.any(Client.class))).willReturn(templatePool);

		Template template = Mockito.mock(Template.class);
		Iterator<Template> templateIterator = Mockito.mock(Iterator.class);
		Mockito.when(templateIterator.hasNext()).thenReturn(true, false);
		Mockito.when(templateIterator.next()).thenReturn(template);
		Mockito.when(templatePool.iterator()).thenReturn(templateIterator);
		Mockito.when(template.getId()).thenReturn(FAKE_ID);
		Mockito.when(template.getName()).thenReturn(FAKE_NAME);
		Mockito.when(template.xpath(TEMPLATE_CPU_PATH)).thenReturn(TEMPLATE_CPU_VALUE);
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(TEMPLATE_MEMORY_VALUE);
		return template;
	}

	private Map<String, String> createImageSizeMap() {
		Map<String, String> imageSizeMap = new HashMap<>();
		imageSizeMap.put(FAKE_IMAGE_KEY, IMAGE_SIZE_VALUE);
		return imageSizeMap;
	}
	
	private ComputeInstance CreateComputeInstance() {
		int cpu = CPU_VALUE_4;
		int ram = MEMORY_VALUE_2048;
		int disk = DISK_VALUE_8GB;
		
		String id = FAKE_INSTANCE_ID;
		String hostName = FAKE_HOST_NAME;
		String image = FAKE_IMAGE;
		String publicKey = FAKE_PUBLIC_KEY;
		List<UserData> userData = FAKE_LIST_USER_DATA;
		
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
		
		int cpu = CPU_VALUE_4;
		int ram = MEMORY_VALUE_2048;
		int disk = DISK_VALUE_8GB;
		
		return new HardwareRequirements(name, id, cpu, ram, disk);
	}

	private List<String> listNetworkIds() {
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
		String name = null, providingMember = null, requestingMember = null, cloudName = null;
		String publicKey = FAKE_PUBLIC_KEY;
		
		FederationUser federationUser = null;
		ArrayList<UserData> userData = FAKE_LIST_USER_DATA;
		
		ComputeOrder computeOrder = new ComputeOrder(
				federationUser, 
				requestingMember, 
				providingMember,
				cloudName,
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
	
	private CloudToken createCloudToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		
		CloudToken token = new CloudToken(
				provider,
				userId,
				tokenValue);
		
		return token;
	}
	
	private String generateTemplate(int choice, String ...args) {
		String cpu;
		String memory;
		String defaultNetworkId;
		String privateNetworkId;
		String size;
		String template;
		if (choice == 0) {
			cpu = args[0];
			memory = args[1];
			defaultNetworkId = args[2] != null ? args[2] : String.valueOf(DEFAULT_NETWORK_ID);
			privateNetworkId = args[3];
			size = args[4];
			template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
					"<TEMPLATE>\n" + 
					"    <CONTEXT>\n" + 
					"        <USERDATA_ENCODING>base64</USERDATA_ENCODING>\n" + 
					"        <NETWORK>YES</NETWORK>\n" +
					"        <USERDATA>fake-user-data</USERDATA>\n" +
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
					"    <NIC>\n" + 
					"        <NETWORK_ID>%s</NETWORK_ID>\n" + 
					"    </NIC>\n" + 
					"    <DISK>\n" + 
					"        <SIZE>%s</SIZE>\n" + 
					"        <TYPE>fs</TYPE>\n" + 
					"    </DISK>\n" + 
					"</TEMPLATE>\n";
			return String.format(template, cpu, memory, defaultNetworkId, privateNetworkId, size);
		} else {
			cpu = args[0];
			memory = args[1];
			defaultNetworkId = args[2] != null ? args[2] : String.valueOf(DEFAULT_NETWORK_ID);
			size = args[3];
			template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
					"<TEMPLATE>\n" + 
					"    <CONTEXT>\n" + 
					"        <USERDATA_ENCODING>base64</USERDATA_ENCODING>\n" + 
					"        <NETWORK>YES</NETWORK>\n" +
					"        <USERDATA>fake-user-data</USERDATA>\n" +
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
			return String.format(template, cpu, memory, defaultNetworkId, size);
		}
	}
	
}
