package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4.OpenNebulaComputePlugin.*;

@PrepareForTest({OpenNebulaClientUtil.class, VirtualMachine.class, DatabaseManager.class})
public class OpenNebulaComputePluginTest extends OpenNebulaBaseTests {
	private static final String FAKE_NAME = "fake-name";
	private static final String USER_NAME = PropertiesHolder.getInstance().getProperty(
			ConfigurationPropertyKeys.SSH_COMMON_USER_KEY, ConfigurationPropertyDefaults.SSH_COMMON_USER);
	private static final String FAKE_BASE64_SCRIPT = "fake-base64-script";

	private static final String ZERO_STRING_VALUE = "0";
	private static final String EMPTY_STRING = "";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_PRIVATE_NETWORK_ID = "fake-private-network-id";
	private static final String FLAVOR_KIND_NAME = "smallest-flavor";
	private static final String IMAGE_SIZE_PATH = OpenNebulaComputePlugin.IMAGE_SIZE_PATH;
	private static final String NO_AVAILABLE_RESOURCES_MSG = "No available resources.";
	private static final String TEMPLATE_CPU_PATH = OpenNebulaComputePlugin.TEMPLATE_CPU_PATH;
	private static final String TEMPLATE_MEMORY_PATH = OpenNebulaComputePlugin.TEMPLATE_MEMORY_PATH;
	private static final String TEMPLATE_IMAGE_ID_PATH = OpenNebulaComputePlugin.TEMPLATE_IMAGE_ID_PATH;

	private static final int CPU_VALUE_1 = 1;
	private static final int CPU_VALUE_8 = 8;
	private static final int MEMORY_VALUE_1024 = 1024;
	private static final int MEMORY_VALUE_2048 = 2048;
	private static final int DISK_VALUE_6GB = 6144;
	private static final int DISK_VALUE_30GB = 30720;
	private static final int ZERO_VALUE = 0;
	private static final int ONE_VALUE = 1;
	private static final int TWO_VALUE = 2;

	private OpenNebulaComputePlugin plugin;
	private ComputeOrder computeOrder;
	private HardwareRequirements hardwareRequirements;
	private List<String> networkIds;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaComputePlugin(this.openNebulaConfFilePath));
		this.computeOrder = this.getComputeOrder();
		this.hardwareRequirements = this.createHardwareRequirements();
		this.networkIds = this.listNetworkIds();
	}

	// test case: when invoking the requestInstance method with valid compute order and
	// cloud user, the plugin should mount an appropriate ONe create compute request,
	// run it via the ONe client and return the instance id.
	@Test
	public void testRequestInstance() throws FogbowException {
		// set up
		CreateComputeRequest createComputeRequest = this.getCreateComputeRequest();

		Mockito.doReturn(createComputeRequest).when(this.plugin).getCreateComputeRequest(
				Mockito.any(Client.class), Mockito.any(ComputeOrder.class));
		Mockito.doReturn(FAKE_ID).when(this.plugin).doRequestInstance(
				Mockito.any(Client.class), Mockito.any(CreateComputeRequest.class));
		Mockito.doNothing().when(this.plugin).setOrderAllocation(Mockito.eq(computeOrder),
				Mockito.eq(createComputeRequest.getVirtualMachine()));

		// exercise
		this.plugin.requestInstance(this.computeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getCreateComputeRequest(
				Mockito.eq(this.client), Mockito.eq(this.computeOrder));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.eq(this.client), Mockito.eq(createComputeRequest));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).setOrderAllocation(Mockito.eq(computeOrder),
				Mockito.eq(createComputeRequest.getVirtualMachine()));
	}

	// test case: verify if allocation is being set to the order properly
	@Test
	public void testSetOrderAllocation() {
		// setup
		ComputeOrder order = Mockito.mock(ComputeOrder.class);
		VirtualMachineTemplate vm = Mockito.mock(VirtualMachineTemplate.class);
		VirtualMachineTemplate.Disk disk = Mockito.mock(VirtualMachineTemplate.Disk.class);

		int expectedCPU = 1;
		int expectedRam = 512;
		int expectedInstances = 1;
		int expectedDisk = 3;

		Mockito.doReturn(disk).when(vm).getDisk();
		Mockito.doReturn(Integer.toString(expectedCPU)).when(vm).getCpu();
		Mockito.doReturn(Integer.toString(expectedRam)).when(vm).getMemory();
		Mockito.doReturn(Integer.toString(expectedDisk*ONE_GIGABYTE_IN_MEGABYTES)).when(disk).getSize();

		Mockito.doCallRealMethod().when(order).setActualAllocation(Mockito.any(ComputeAllocation.class));
		Mockito.doCallRealMethod().when(order).getActualAllocation();

		// exercise
		this.plugin.setOrderAllocation(order, vm);

		// verify
		Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).setActualAllocation(Mockito.any(ComputeAllocation.class));

		Assert.assertEquals(expectedCPU, order.getActualAllocation().getvCPU());
		Assert.assertEquals(expectedRam, order.getActualAllocation().getRam());
		Assert.assertEquals(expectedInstances, order.getActualAllocation().getInstances());
		Assert.assertEquals(expectedDisk, order.getActualAllocation().getDisk());
	}

	// test case: when invoking the getCreateRequestInstance method with a valid client and compute order,
	// the plugin should create the base64 script using the appropriate launch command generator; create
	// the network ids list; and, find an appropriate 'flavor' for the order.
	@Test
	public void testGetCreateRequestInstance() throws UnexpectedException, NoAvailableResourcesException {
		// set up
		LaunchCommandGenerator launchCommandGenerator = Mockito.mock(LaunchCommandGenerator.class);
		this.plugin.setLaunchCommandGenerator(launchCommandGenerator);

		Mockito.when(launchCommandGenerator.createLaunchCommand(Mockito.any(ComputeOrder.class))).thenReturn(FAKE_BASE64_SCRIPT);
		Mockito.doReturn(this.networkIds).when(this.plugin).getNetworkIds(Mockito.anyList());
		Mockito.doReturn(this.hardwareRequirements).when(this.plugin).findSmallestFlavor(
				Mockito.any(Client.class), Mockito.any(ComputeOrder.class));

		// exercise
		this.plugin.getCreateComputeRequest(this.client, this.computeOrder);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkIds(Mockito.eq(this.computeOrder.getNetworkIds()));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findSmallestFlavor(
				Mockito.eq(this.client), Mockito.eq(this.computeOrder));
	}

	// test case: when invoking the doRequestInstance method with valid client and create compute
	// request, the plugin should allocate a new virtual machine in ONe and return its instance id.
	@Test
	public void testDoRequestInstance() throws InvalidParameterException, NoAvailableResourcesException, QuotaExceededException {
		// set up
		CreateComputeRequest createComputeRequest = Mockito.spy(this.getCreateComputeRequest());
		Mockito.when(OpenNebulaClientUtil.allocateVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(this.computeOrder.getInstanceId());

		// exercise
		this.plugin.doRequestInstance(this.client, createComputeRequest);

		// verify
		Mockito.verify(createComputeRequest, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachine();

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.allocateVirtualMachine(this.client, createComputeRequest.getVirtualMachine().marshalTemplate());
	}

	// test case: when invoking listNetworkIds with a non-empty list of network ids,
    // a new network ids list should be returned containing all of the passed list ids
	// and the default network id.
	@Test
	public void testListNetworkIds() {
		// set up
		this.networkIds.add(FAKE_PRIVATE_NETWORK_ID);

		// exercise
		List<String> networkIds = this.plugin.getNetworkIds(this.networkIds);

		// verify
		Assert.assertEquals(TWO_VALUE, networkIds.size());
		Assert.assertEquals(ZERO_STRING_VALUE, networkIds.get(ZERO_VALUE));
		Assert.assertEquals(FAKE_PRIVATE_NETWORK_ID, networkIds.get(ONE_VALUE));
	}

	// test case: when invoking listNetworkIds with an empty list of network ids,
	// a new network ids list should be returned containing only default network id.
	@Test
	public void testListNetworkIdsEmpty() {
		// exercise
		List<String> networkIds = this.plugin.getNetworkIds(this.networkIds);

		// verify
		Assert.assertEquals(ONE_VALUE, networkIds.size());
		Assert.assertEquals(ZERO_STRING_VALUE, networkIds.get(ZERO_VALUE));
	}

	// test case: when invoking findSmallestFlavor with valid client and compute order, the plugin
	// should return an appropriate flavor in case one fits the order hardware requirements
	@Test
	public void testFindSmallestFlavor() throws UnexpectedException, NoAvailableResourcesException {
		// set up
		Mockito.doReturn(this.hardwareRequirements).when(this.plugin).getBestFlavor(
				Mockito.any(Client.class), Mockito.any(ComputeOrder.class));

		// exercise
		this.plugin.findSmallestFlavor(this.client, this.computeOrder);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getBestFlavor(
				Mockito.eq(this.client), Mockito.eq(this.computeOrder));
	}

	// test case: when invoking findSmallestFlavor with valid client and compute order, the plugin
	// should throw a NoAvailableResourcesException when no appropriate flavor fits the order
	// hardware requirements
	@Test
	public void testFindSmallestFlavorFail() throws UnexpectedException {
		// set up
		Mockito.doReturn(null).when(this.plugin).getBestFlavor(
				Mockito.any(Client.class), Mockito.any(ComputeOrder.class));

		// exercise
		try {
			this.plugin.findSmallestFlavor(this.client, this.computeOrder);
			Assert.fail();
		} catch (NoAvailableResourcesException e) {
		    Assert.assertEquals(NO_AVAILABLE_RESOURCES_MSG, e.getMessage());
		}

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getBestFlavor(
				Mockito.eq(this.client), Mockito.eq(this.computeOrder));
	}

	// test case: when invoking getBestFlavor with valid client and compute order, the plugin
	// should return the first flavor in the flavors cache that fits the order hardware
	// requirements
	@Test
	public void testGetBestFlavor() throws UnexpectedException {
		// set up
		this.plugin.setFlavors(new TreeSet<>(Arrays.asList(this.hardwareRequirements)));
		Mockito.doNothing().when(this.plugin).updateHardwareRequirements(Mockito.any(Client.class));

		// exercise
		HardwareRequirements hardwareRequirements = this.plugin.getBestFlavor(this.client, this.computeOrder);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateHardwareRequirements(Mockito.eq(this.client));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFlavors();
		Assert.assertEquals(this.hardwareRequirements, hardwareRequirements);
	}

	// test case: when invoking getBestFlavor with valid client and compute order, the plugin
	// should return null when no flavor in the flavors cache fits the order hardware requirements
	@Test
	public void testGetBestFlavorNull() throws UnexpectedException {
		// set up
		this.hardwareRequirements.setCpu(CPU_VALUE_1);
		this.plugin.setFlavors(new TreeSet<>(Arrays.asList(this.hardwareRequirements)));

		Mockito.doNothing().when(this.plugin).updateHardwareRequirements(Mockito.any(Client.class));

		// exercise
		HardwareRequirements hardwareRequirements = this.plugin.getBestFlavor(this.client, this.computeOrder);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateHardwareRequirements(Mockito.eq(this.client));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFlavors();
		Assert.assertEquals(null, hardwareRequirements);
	}

	// test case: When calling the updateHardwareRequirements method with a set of
	// outdated flavors, a collection of valid resources must be obtained in the
	// cloud and compared to existing flavors, adding new flavors to the set when
	// they do not exist.
	@Test
	public void testUpdateHardwareRequirements() throws UnexpectedException {
		// set up
		TemplatePool templatePool = Mockito.mock(TemplatePool.class);
		Template template = Mockito.mock(Template.class);
		Iterator<Template> templateIterator = Mockito.mock(Iterator.class);

		Mockito.doReturn(new HashMap<String, String>()).when(this.plugin).getImagesSizes(Mockito.any(Client.class));
		PowerMockito.when(OpenNebulaClientUtil.getTemplatePool(Mockito.any(Client.class))).thenReturn(templatePool);

		Mockito.when(templatePool.iterator()).thenReturn(templateIterator);
		Mockito.when(templateIterator.hasNext()).thenReturn(true, false);
		Mockito.when(templateIterator.next()).thenReturn(template);
		Mockito.when(template.getId()).thenReturn(this.hardwareRequirements.getFlavorId());
		Mockito.when(template.getName()).thenReturn(this.hardwareRequirements.getName());
		Mockito.when(template.xpath(TEMPLATE_CPU_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getCpu()));
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getRam()));
		Mockito.when(template.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getDisk()));

		TreeSet<HardwareRequirements> expected = new TreeSet<>(Arrays.asList(this.hardwareRequirements));

		// exercise
		this.plugin.updateHardwareRequirements(this.client);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getTemplatePool(Mockito.eq(this.client));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImagesSizes(Mockito.eq(this.client));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_TWICE)).convertToInteger(Mockito.anyString());
		Mockito.verify(template, Mockito.times(TestUtils.RUN_ONCE)).getId();
		Mockito.verify(template, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(template, Mockito.times(TestUtils.RUN_THRICE)).xpath(Mockito.anyString());

		Assert.assertEquals(expected, this.plugin.getFlavors());
	}

	// test case: when invoking updateHardwareRequirements with a set of no new flavors, then
	// the plugin flavor should have no update
	@Test
	public void testUpdateHardwareRequirementsNoNewFlavors() throws UnexpectedException {
		// set up
		TemplatePool templatePool = Mockito.mock(TemplatePool.class);
		Template template = Mockito.mock(Template.class);
		Iterator<Template> templateIterator = Mockito.mock(Iterator.class);
		String flavorId = this.hardwareRequirements.getFlavorId();

		Mockito.doReturn(new HashMap<String, String>()).when(this.plugin).getImagesSizes(Mockito.any(Client.class));
		Mockito.doReturn(DISK_VALUE_30GB).when(this.plugin).getDiskSizeFromImageSizeMap(Mockito.any(Map.class), Mockito.anyString());
		Mockito.doReturn(true).when(this.plugin).containsFlavor(Mockito.any(HardwareRequirements.class));
		PowerMockito.when(OpenNebulaClientUtil.getTemplatePool(Mockito.any(Client.class))).thenReturn(templatePool);

		Mockito.when(templatePool.iterator()).thenReturn(templateIterator);
		Mockito.when(templateIterator.hasNext()).thenReturn(true, false);
		Mockito.when(templateIterator.next()).thenReturn(template);
		Mockito.when(template.getId()).thenReturn(flavorId);
		Mockito.when(template.getName()).thenReturn(this.hardwareRequirements.getName());
		Mockito.when(template.xpath(TEMPLATE_CPU_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getCpu()));
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getRam()));
		Mockito.when(template.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn(ZERO_STRING_VALUE);
		Mockito.when(template.xpath(TEMPLATE_IMAGE_ID_PATH)).thenReturn(flavorId);

		// exercise
		this.plugin.updateHardwareRequirements(this.client);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getTemplatePool(Mockito.eq(this.client));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImagesSizes(Mockito.eq(this.client));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_TWICE)).convertToInteger(Mockito.anyString());
		Mockito.verify(template, Mockito.times(TestUtils.RUN_ONCE)).getId();
		Mockito.verify(template, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(template, Mockito.times(TestUtils.RUN_FOUR_TIMES)).xpath(Mockito.anyString());

		Assert.assertTrue(this.plugin.getFlavors().isEmpty());
	}

	// test case: when invoking getImagesSizes with a valid client, a map of all image ids and respective sizes
	// should be returned
	@Test
	public void testGetImagesSizes() throws UnexpectedException {
	    // setup
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		String fakeImageSize = String.valueOf(DISK_VALUE_6GB);

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(this.computeOrder.getImageId());
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(fakeImageSize);

		// exercise
		Map<String, String> imagesSizes = this.plugin.getImagesSizes(this.client);

		// verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getImagePool(Mockito.eq(this.client));

        Assert.assertEquals(ONE_VALUE, imagesSizes.size());
        Assert.assertEquals(fakeImageSize, imagesSizes.get(this.computeOrder.getImageId()));
	}

	// test case: when invoking convertToInteger with a parsable string, its respective int value should
	// be returned; otherwise 0 should be returned.
	@Test
	public void testConvertToInteger() {
	    // set up
		int expectedConvertedFail = 0;

		// exercise
		int converted = this.plugin.convertToInteger(String.valueOf(DISK_VALUE_6GB));
		int convertedFail = this.plugin.convertToInteger(EMPTY_STRING);

		// verify
		Assert.assertEquals(DISK_VALUE_6GB, converted);
		Assert.assertEquals(expectedConvertedFail, convertedFail);
	}

	// test case: when invoking getDiskSizeFromImageSizeMap with a valid disk sizes map and
	// image id, return the respective disk int-converted disk size; otherwise, return 0
	@Test
	public void testGetDiskSizeFromImageSizeMap() {
		// set up
		Map<String, String> diskSizeMap = new HashMap<>();
		diskSizeMap.put(FAKE_IMAGE_ID, String.valueOf(DISK_VALUE_6GB));
		int expectedDiskSizeFail = 0;

		// exercise
		int diskSize = this.plugin.getDiskSizeFromImageSizeMap(diskSizeMap, FAKE_IMAGE_ID);
		int diskSizeFail = this.plugin.getDiskSizeFromImageSizeMap(diskSizeMap, FAKE_ID);

		// verify
		Assert.assertEquals(DISK_VALUE_6GB, diskSize);
		Assert.assertEquals(expectedDiskSizeFail, diskSizeFail);
	}

	// test case: when invoking containsFlavor with a valid hardware requirements object, return
	// true in case it is found on the flavors cache
	@Test
	public void testContainsFlavor() {
	    // set up
		this.plugin.setFlavors(new TreeSet<>(Arrays.asList(this.hardwareRequirements)));

		// exercise
		boolean hasFlavor = this.plugin.containsFlavor(this.hardwareRequirements);

		// verify
		Assert.assertTrue(hasFlavor);
	}

	// test case: when invoking containsFlavor with a valid hardware requirements object, return
	// false in case it is not found on the flavors cache
	@Test
	public void testContainsFlavorFail() {
		// set up
		HardwareRequirements fakeFlavor = new HardwareRequirements(
				FAKE_NAME, FAKE_ID, CPU_VALUE_1, MEMORY_VALUE_1024, DISK_VALUE_6GB);
		this.plugin.setFlavors(new TreeSet<>(Arrays.asList(this.hardwareRequirements)));

		// exercise
		boolean hasFlavor = this.plugin.containsFlavor(fakeFlavor);

		// verify
		Assert.assertFalse(hasFlavor);
	}

	// test case: when invoking getInstance with a valid compute order and cloud user, the plugin
	// should retrieve the vm info from ONe and return it as a compute instance.
	@Test
	public void testGetInstance() throws FogbowException {
	    // set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		ComputeInstance computeInstance = new ComputeInstance(this.computeOrder.getInstanceId());

        Mockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);
        Mockito.doReturn(computeInstance).when(this.plugin).doGetInstance(Mockito.any(VirtualMachine.class));

        // exercise
		this.plugin.getInstance(this.computeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.computeOrder.getInstanceId()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(virtualMachine));
	}

	// test case: when invoking doGetInstance with a valid virtual machine retrieved from ONe,
	// the plugin should mount a ComputeInstance object based on the vm attributes.
	@Test
	public void testDoGetInstance() {
		// set up
	    VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
	    OneResponse response = Mockito.mock(OneResponse.class);

		Mockito.when(virtualMachine.info()).thenReturn(response);
	    Mockito.when(virtualMachine.getId()).thenReturn(this.computeOrder.getInstanceId());
		Mockito.when(virtualMachine.getName()).thenReturn(this.computeOrder.getName());
		Mockito.when(virtualMachine.lcmStateStr()).thenReturn(OrderState.FULFILLED.toString());
		Mockito.when(virtualMachine.xpath(TEMPLATE_CPU_PATH)).thenReturn(String.valueOf(this.computeOrder.getvCPU()));
		Mockito.when(virtualMachine.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(String.valueOf(this.computeOrder.getRam()));
		Mockito.when(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn(String.valueOf(this.computeOrder.getDisk()));
		Mockito.when(response.getMessage()).thenReturn(this.getVirtualMachineResponse());

		Mockito.doNothing().when(this.plugin).setComputeInstanceNetworks(Mockito.any(ComputeInstance.class));

		// exercise
		ComputeInstance computeInstance = this.plugin.doGetInstance(virtualMachine);

		// verify
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).info();
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).getId();
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).lcmStateStr();
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(TEMPLATE_CPU_PATH));
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(TEMPLATE_MEMORY_PATH));
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(TEMPLATE_DISK_SIZE_PATH));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).setComputeInstanceNetworks(Mockito.any(ComputeInstance.class));

		Assert.assertEquals(this.computeOrder.getDisk() / ONE_GIGABYTE_IN_MEGABYTES, computeInstance.getDisk());
	}

	// test case: when invoking deleteInstance with a valid compute order and cloud user,
	// the plugin should retrieve the respective vm from ONe and terminate it.
	@Test
	public void testDeleteInstance() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		OneResponse response = Mockito.mock(OneResponse.class);

		Mockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);
		Mockito.when(virtualMachine.terminate(Mockito.eq(SHUTS_DOWN_HARD))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		// exercise
		this.plugin.deleteInstance(this.computeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.computeOrder.getInstanceId()));

		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).terminate(Mockito.eq(SHUTS_DOWN_HARD));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking deleteInstance with an invalid compute order, or when the terminate
	// action does not succeed the plugin should throw an UnexpectedException
	@Test
	public void testDeleteInstanceFail() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		OneResponse response = Mockito.mock(OneResponse.class);
		String message = String.format(Messages.Error.ERROR_WHILE_REMOVING_VM, this.computeOrder.getInstanceId(),
                response.getMessage());

		Mockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);
		Mockito.when(virtualMachine.terminate(Mockito.eq(SHUTS_DOWN_HARD))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		try {
			this.plugin.deleteInstance(this.computeOrder, this.cloudUser);
			Assert.fail();
		} catch (UnexpectedException e) {
			Assert.assertEquals(e.getMessage(), message);
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.computeOrder.getInstanceId()));

		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).terminate(Mockito.eq(SHUTS_DOWN_HARD));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	private HardwareRequirements createHardwareRequirements() {
		String id = FAKE_ID;
		String name = FLAVOR_KIND_NAME;
		
		int cpu = CPU_VALUE_8;
		int ram = MEMORY_VALUE_2048;
		int disk = DISK_VALUE_30GB;
		
		return new HardwareRequirements(name, id, cpu, ram, disk);
	}

	private List<String> listNetworkIds() {
		List<String> networksId = new ArrayList<>();
		networksId.addAll(this.computeOrder.getNetworkIds());
		return networksId;
	}

	private String getVirtualMachineResponse() {
		String xml = "<VM>\n"
    			+ "  <ID>fake-id</ID>\n"
    			+ "  <NAME>fake-name</NAME>\n"
    			+ "  <LCM_STATE>3</LCM_STATE>\n"
    			+ "  <TEMPLATE>\n"
    			+ "    <CPU>1</CPU>\n"
    			+ "    <DISK>\n"
    			+ "      <SIZE>8192</SIZE>\n"
    			+ "    </DISK>\n"
    			+ "    <MEMORY>1024</MEMORY>\n"
    			+ "    <NIC>\n"
    			+ "      <IP>172.16.100.201</IP>\n"
    			+ "    </NIC>\n"
    			+ "    <NIC>\n"
    			+ "      <IP>172.16.100.202</IP>\n"
    			+ "    </NIC>\n"
    			+ "  </TEMPLATE>\n"
    			+ "</VM>";
		
		return xml;
	}

	private ComputeOrder getComputeOrder() {
		ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
		return computeOrder;
	}

	private CreateComputeRequest getCreateComputeRequest() {
		CreateComputeRequest request = new CreateComputeRequest.Builder()
				.name(this.computeOrder.getName())
				.contextNetwork(NETWORK_CONFIRMATION_CONTEXT)
				.publicKey(this.computeOrder.getPublicKey())
				.userName(USER_NAME)
				.startScriptBase64(FAKE_BASE64_SCRIPT)
				.cpu(String.valueOf(this.hardwareRequirements.getCpu()))
				.graphicsAddress(DEFAULT_GRAPHIC_ADDRESS)
				.graphicsType(DEFAULT_GRAPHIC_TYPE)
				.imageId(this.computeOrder.getImageId())
				.diskSize(String.valueOf(this.hardwareRequirements.getDisk()))
				.memory(String.valueOf(this.hardwareRequirements.getRam()))
				.networks(this.networkIds)
				.architecture(DEFAULT_ARCHITECTURE)
				.build();

		return request;
	}
}
