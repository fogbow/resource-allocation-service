package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.io.File;
import java.util.*;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.NoAvailableResourcesException;
import org.fogbowcloud.ras.core.exceptions.QuotaExceededException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.HardwareRequirements;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.UserData;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualMachine.class})
public class OpenNebulaComputePluginTest {

	private static final String CLOUD_NAME = "opennebula";
	private static final String DEFAULT_VIRTUAL_MACHINE_STATE = "Running";
	private static final String FAKE_HOST_NAME = "fake-host-name";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_IMAGE = "fake-image";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_IMAGE_KEY = "fake-image-key";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String FAKE_PRIVATE_NETWORK_ID = "fake-private-network-id";
	private static final String FAKE_PRIVATE_IP = "0.0.0.0";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FLAVOR_KIND_NAME = "smallest-flavor";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String IMAGE_SIZE_VALUE = "8";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String MESSAGE_RESPONSE_ANYTHING = "anything";
	private static final String MESSAGE_RESPONSE_LIMIT_QUOTA = "limit quota";
	private static final String MESSAGE_RESPONSE_WITHOUT_MEMORY = "Not enough free memory";
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

	private static final UserData[] fakeUserDataArray= new UserData[]{new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag")};
	private static final ArrayList<UserData> FAKE_USER_DATA = new ArrayList<>(Arrays.asList(fakeUserDataArray));

	private static final int CPU_VALUE = 4;
	private static final int MEMORY_VALUE = 2048;
	private static final int DISK_VALUE = 8;

	private OpenNebulaClientFactory factory;
	private OpenNebulaComputePlugin plugin;
	private TreeSet<HardwareRequirements> flavors;
    private String openenbulaConfFilePath;

    @Before
	public void setUp() {
		this.factory = Mockito.mock(OpenNebulaClientFactory.class);
		this.openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
				File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.plugin = Mockito.spy(new OpenNebulaComputePlugin(openenbulaConfFilePath));
		this.flavors = Mockito.spy(new TreeSet<>());
	}
	
	// test case: When calling the requestInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testRequestInstanceThrowUnespectedExceptionWhenCallCreateClient()
			throws UnexpectedException, FogbowRasException {
		
		// set up
		ComputeOrder computeOrder = new ComputeOrder();
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.requestInstance(computeOrder, token);
	}
	
	// test case: When calling the requestInstance method, with the valid client and
	// template with a list of networks ID, a virtual network will be allocated,
	// returned a instance ID.
	@Test
	public void testRequestInstanceSuccessfulWithNetworksId() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
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

		Template template = mockTemplatePoolIterator(client);

		HardwareRequirements flavor = createHardwareRequirements();
		this.flavors.add(flavor);
		this.plugin.setFlavors(this.flavors);

		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.eq(flavor), Mockito.eq(this.flavors));

		Mockito.doReturn(disk).when(this.plugin).loadImageSizeDisk(imageSizeMap, template);

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
	
	// test case: When calling the requestInstance method, with the valid client and
	// template without a list of networks ID, a virtual network will be allocated,
	// returned a instance ID.
	@Test
	public void testRequestInstanceSuccessfulWithoutNetworksId() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = null;
		int cpu = 1;
		int memory = 2048;
		int disk = 8;

		ComputeOrder computeOrder = createComputeOrder(networksId, cpu, memory, disk);

		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		String networkId = null;
		String valueOfCpu = String.valueOf(4);
		String valueOfRam = String.valueOf(2048);
		String valueOfDisk = String.valueOf(8);
		String template = generateTemplate(valueOfCpu, valueOfRam, networkId, valueOfDisk);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.any(ComputeOrder.class),
				Mockito.any(Token.class));
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When calling getBestFlavor during requestInstance method, and it
	// returns a null instance, a NoAvailableResourcesException will be thrown.
	@Test(expected = NoAvailableResourcesException.class) // verify
	public void testRequestInstanceThrowNoAvailableResourcesExceptionWhenCallGetBestFlavor()
			throws UnexpectedException, FogbowRasException {

		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = null;
		int cpu = 1;
		int memory = 2048;
		int disk = 8;

		ComputeOrder computeOrder = createComputeOrder(networksId, cpu, memory, disk);

		Map<String, String> imageSizeMap = createImageSizeMap();
		Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		this.plugin.setFlavors(this.flavors);

		// exercise
		this.plugin.requestInstance(computeOrder, token);
	}
	
	// test case: When calling the requestInstance method, and an error not
	// specified occurs while attempting to allocate a virtual machine, an
	// InvalidParameterException will be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRequestInstanceThrowInvalidParameterException() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		this.factory = Mockito.spy(new OpenNebulaClientFactory(this.openenbulaConfFilePath));
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = null;
		int cpu = 1;
		int memory = 2048;
		int disk = 8;

		ComputeOrder computeOrder = createComputeOrder(networksId, cpu, memory, disk);

		Map<String, String> imageSizeMap = createImageSizeMap();
		Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		String networkId = null;
		String valueOfCpu = String.valueOf(4);
		String valueOfRam = String.valueOf(2048);
		String valueOfDisk = String.valueOf(8);
		String template = generateTemplate(valueOfCpu, valueOfRam, networkId, valueOfDisk);

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		PowerMockito.when(VirtualMachine.allocate(client, template)).thenReturn(response);
		Mockito.doReturn(true).when(response).isError();
		Mockito.when(response.getErrorMessage()).thenReturn(MESSAGE_RESPONSE_ANYTHING);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When you attempt to allocate a virtual machine with the
	// requestInstance method call, and an insufficient free memory error message
	// occurs, a NoAvailableResourcesException will be thrown.
	@Test(expected = NoAvailableResourcesException.class) // verify
	public void testRequestInstanceThrowNoAvailableResourcesException() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		this.factory = Mockito.spy(new OpenNebulaClientFactory(this.openenbulaConfFilePath));
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = null;
		int cpu = 1;
		int memory = 2048;
		int disk = 8;

		ComputeOrder computeOrder = createComputeOrder(networksId, cpu, memory, disk);

		Map<String, String> imageSizeMap = createImageSizeMap();
		Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		String networkId = null;
		String valueOfCpu = String.valueOf(4);
		String valueOfRam = String.valueOf(2048);
		String valueOfDisk = String.valueOf(8);
		String template = generateTemplate(valueOfCpu, valueOfRam, networkId, valueOfDisk);

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		PowerMockito.when(VirtualMachine.allocate(client, template)).thenReturn(response);
		Mockito.doReturn(true).when(response).isError();
		Mockito.when(response.getErrorMessage()).thenReturn(MESSAGE_RESPONSE_WITHOUT_MEMORY);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When attempting to allocate a virtual machine with the
	// requestInstance method call, and an error message occurs with the words limit
	// and quota, a QuotaExceededException will be thrown.
	@Test(expected = QuotaExceededException.class) // verify
	public void testRequestInstanceThrowQuotaExceededException() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		this.factory = Mockito.spy(new OpenNebulaClientFactory(this.openenbulaConfFilePath));
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		List<String> networksId = null;
		int cpu = 1;
		int memory = 2048;
		int disk = 8;

		ComputeOrder computeOrder = createComputeOrder(networksId, cpu, memory, disk);

		Map<String, String> imageSizeMap = createImageSizeMap();
		Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		HardwareRequirements flavor = createHardwareRequirements();
		Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(computeOrder, token);

		String networkId = null;
		String valueOfCpu = String.valueOf(4);
		String valueOfRam = String.valueOf(2048);
		String valueOfDisk = String.valueOf(8);
		String template = generateTemplate(valueOfCpu, valueOfRam, networkId, valueOfDisk);

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		PowerMockito.when(VirtualMachine.allocate(client, template)).thenReturn(response);
		Mockito.doReturn(true).when(response).isError();
		Mockito.when(response.getErrorMessage()).thenReturn(MESSAGE_RESPONSE_LIMIT_QUOTA);

		// exercise
		this.plugin.requestInstance(computeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualMachine(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When calling the getInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetInstanceThrowUnespectedExceptionWhenCallCreateClient()
			throws UnexpectedException, FogbowRasException {

		// set up
		String instanceId = FAKE_INSTANCE_ID;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getInstance(instanceId, token);
	}
	
	// test case: When calling the getInstance method of a resource with the volatile disk size passing 
	// a valid client of a token value and an instance ID, it must return an instance of a virtual machine.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetInstanceSuccessfulWithVolatileDiskResource() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);
		this.plugin.setFlavors(this.flavors);

		Image image = Mockito.mock(Image.class);
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(IMAGE_SIZE_VALUE);

		Template template = mockTemplatePoolIterator(client);

		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());

		int index = 1;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_SIZE_PATH, index))).thenReturn(IMAGE_SIZE_VALUE);
		index++;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_PATH, index))).thenReturn(FAKE_NAME);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);

		Mockito.when(virtualMachine.getId()).thenReturn(FAKE_INSTANCE_ID);
		Mockito.when(virtualMachine.xpath(TEMPLATE_NAME_PATH)).thenReturn(FAKE_NAME);
		Mockito.when(virtualMachine.xpath(TEMPLATE_CPU_PATH)).thenReturn(TEMPLATE_CPU_VALUE);
		Mockito.when(virtualMachine.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(TEMPLATE_MEMORY_VALUE);
		Mockito.when(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn(IMAGE_SIZE_VALUE);
		Mockito.when(virtualMachine.lcmStateStr()).thenReturn(DEFAULT_VIRTUAL_MACHINE_STATE);
		Mockito.when(virtualMachine.xpath(TEMPLATE_NIC_IP_PATH)).thenReturn(FAKE_PRIVATE_IP);

		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(3)).createClient(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getImageSizes(client);
		Mockito.verify(this.factory, Mockito.times(1)).createTemplatePool(client);
		Mockito.verify(template, Mockito.times(1)).getId();
		Mockito.verify(template, Mockito.times(1)).getName();
		Mockito.verify(template, Mockito.times(9)).xpath(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(2)).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).createVirtualMachineInstance(virtualMachine);
		Mockito.verify(virtualMachine, Mockito.times(1)).getId();
		Mockito.verify(virtualMachine, Mockito.times(5)).xpath(Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).lcmStateStr();
	}

	// test case: When calling the getInstance method of a resource without the volatile disk size passing 
	// a valid client of a token value and an instance ID, it must return an instance of a virtual machine.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetInstanceSuccessfulWithoutVolatileDiskResource() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);
		this.plugin.setFlavors(this.flavors);

		Map<String, String> imageSizeMap = createImageSizeMap();
		Mockito.doReturn(imageSizeMap).when(this.plugin).getImageSizes(client);

		Template template = mockTemplatePoolIterator(client);

		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());

		int index = 1;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_IMAGE_PATH, index))).thenReturn(FAKE_IMAGE_KEY);

		index++;
		Mockito.when(template.xpath(String.format(TEMPLATE_DISK_INDEX_PATH, index))).thenReturn(FAKE_NAME);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);

		ComputeInstance computeInstance = CreateComputeInstance();
		Mockito.doReturn(computeInstance).when(this.plugin).createVirtualMachineInstance(virtualMachine);

		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(3)).createClient(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getImageSizes(client);
		Mockito.verify(this.factory, Mockito.times(1)).createTemplatePool(client);
		Mockito.verify(template, Mockito.times(1)).getId();
		Mockito.verify(template, Mockito.times(1)).getName();
		Mockito.verify(template, Mockito.times(9)).xpath(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(2)).containsFlavor(Mockito.any(HardwareRequirements.class),
				Mockito.anyCollection());
		Mockito.verify(this.plugin, Mockito.times(1)).loadImageSizeDisk(Mockito.eq(imageSizeMap), Mockito.eq(template));
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).createVirtualMachineInstance(virtualMachine);
	}
	
	// test case: When calling the deleteInstance method, if the OpenNebulaClientFactory class 
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceThrowUnespectedExceptionWhenCallCreateClient()
			throws UnexpectedException, FogbowRasException {

		// set up
		String instanceId = FAKE_INSTANCE_ID;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.deleteInstance(instanceId, token);
	}
	
	// Test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of virtual machine will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate();
		Mockito.doReturn(true).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).terminate();
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// Test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate();
		Mockito.doReturn(false).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).terminate();
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	@SuppressWarnings(UNCHECKED_VALUE)
	private Template mockTemplatePoolIterator(Client client) throws UnexpectedException {
		TemplatePool templatePool = Mockito.mock(TemplatePool.class);
		Mockito.doReturn(templatePool).when(this.factory).createTemplatePool(client);

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
		int cpu = CPU_VALUE;
		int ram = MEMORY_VALUE;
		int disk = DISK_VALUE;
		
		String id = FAKE_INSTANCE_ID;
		String hostName = FAKE_HOST_NAME;
		String image = FAKE_IMAGE;
		String publicKey = FAKE_PUBLIC_KEY;
		List<UserData> userData = FAKE_USER_DATA;
		
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
		String name = null, providingMember = null, requestingMember = null, cloudName = null;
		String publicKey = FAKE_PUBLIC_KEY;
		
		FederationUserToken federationUserToken = null;
		ArrayList<UserData> userData = FAKE_USER_DATA;
		
		ComputeOrder computeOrder = new ComputeOrder(
				federationUserToken, 
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
	
	private OpenNebulaToken createOpenNebulaToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		String userName = FAKE_USER_NAME;
		String signature = null;
		
		OpenNebulaToken token = new OpenNebulaToken(
				provider, 
				tokenValue, 
				userId, 
				userName, 
				signature);
		
		return token;
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
				"        <USERDATA>fakeuserdata</USERDATA>\n" +
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
