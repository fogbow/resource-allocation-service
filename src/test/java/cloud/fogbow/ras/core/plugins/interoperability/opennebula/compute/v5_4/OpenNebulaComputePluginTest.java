package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.common.util.CloudInitUserDataBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
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
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4.OpenNebulaComputePlugin.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, VirtualMachine.class, DatabaseManager.class})
public class OpenNebulaComputePluginTest extends OpenNebulaBaseTests {
	private static final String FAKE_NAME = "fake-name";
	private static final String USER_NAME = PropertiesHolder.getInstance().getProperty(
			ConfigurationPropertyKeys.SSH_COMMON_USER_KEY, ConfigurationPropertyDefaults.SSH_COMMON_USER);
	private static final String FAKE_BASE64_SCRIPT = "fake-base64-script";

	private static final String ANOTHER_FAKE_NAME = "another-fake-name";
	private static final String DECIMAL_STRING_VALUE = "0.1";
	private static final String EMPTY_STRING = "";
	private static final String FAKE_HOST_NAME = "fake-host-name";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_IMAGE = "fake-image";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_PRIVATE_NETWORK_ID = "fake-private-network-id";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_TAG = "fake-tag";
	private static final String FAKE_USER_DATA = "fake-user-data";
	private static final String FIELD_RESPONSE_LIMIT = "limit";
	private static final String FIELD_RESPONSE_QUOTA = "quota";
	private static final String FLAVOR_KIND_NAME = "smallest-flavor";
	private static final String ONE_STRING_VALUE = "1";
	private static final String IMAGE_SIZE_PATH = OpenNebulaComputePlugin.IMAGE_SIZE_PATH;
	private static final String IMAGE_SIZE_STRING_VALUE = "8192";
	private static final String IP_ADDRESS_ONE = "172.16.100.201";
	private static final String IP_ADDRESS_TWO = "172.16.100.202";
	private static final String LCM_STATE_RUNNING = "running";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String MEMORY_STRING_VALUE = "1024";
	private static final String MESSAGE_RESPONSE_ANYTHING = "anything";
	private static final String RESPONSE_NOT_ENOUGH_FREE_MEMORY = "Not enough free memory";
	private static final String SEPARATOR = " ";
	private static final String TEMPLATE_CPU_PATH = OpenNebulaComputePlugin.TEMPLATE_CPU_PATH;
	private static final String TEMPLATE_CPU_VALUE = "2";
	private static final String TEMPLATE_MEMORY_PATH = OpenNebulaComputePlugin.TEMPLATE_MEMORY_PATH;
	private static final String TEMPLATE_MEMORY_VALUE = "1024";
	private static final String TEMPLATE_IMAGE_ID_PATH = OpenNebulaComputePlugin.TEMPLATE_IMAGE_ID_PATH;

	private static final UserData[] FAKE_USER_DATA_ARRAY = new UserData[] {
			new UserData(FAKE_USER_DATA, CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, FAKE_TAG) };

	private static final ArrayList<UserData> FAKE_LIST_USER_DATA = new ArrayList<>(Arrays.asList(FAKE_USER_DATA_ARRAY));

	private static final int CPU_VALUE_1 = 1;
	private static final int CPU_VALUE_2 = 2;
	private static final int CPU_VALUE_8 = 8;
	private static final int MEMORY_VALUE_1024 = 1024;
	private static final int MEMORY_VALUE_2048 = 2048;
	private static final int DISK_VALUE_6GB = 6144;
	private static final int DISK_VALUE_30GB = 30720;
	private static final int ZERO_VALUE = 0;
	private static final int DEFAULT_NETWORK_ID = ZERO_VALUE;

	private OpenNebulaComputePlugin plugin;
	private TreeSet<HardwareRequirements> flavors;
	private ComputeOrder computeOrder;
	private HardwareRequirements hardwareRequirements;
	private List<String> networkIds;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaComputePlugin(this.openNebulaConfFilePath));
		this.flavors = Mockito.spy(new TreeSet<>());
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

		// exercise
		this.plugin.requestInstance(this.computeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getCreateComputeRequest(
				Mockito.eq(this.client), Mockito.eq(this.computeOrder));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.eq(this.client), Mockito.eq(createComputeRequest));
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
		Mockito.doReturn(this.getCreateComputeRequest()).when(this.plugin).createComputeRequest(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(),
				Mockito.anyString());

		// exercise
		this.plugin.getCreateComputeRequest(this.client, this.computeOrder);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkIds(Mockito.eq(this.computeOrder.getNetworkIds()));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findSmallestFlavor(
				Mockito.eq(this.client), Mockito.eq(this.computeOrder));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createComputeRequest(
				Mockito.eq(this.computeOrder.getName()), Mockito.eq(NETWORK_CONFIRMATION_CONTEXT),
				Mockito.eq(this.computeOrder.getPublicKey()), Mockito.eq(USER_NAME), Mockito.eq(FAKE_BASE64_SCRIPT),
				Mockito.eq(String.valueOf(this.hardwareRequirements.getCpu())), Mockito.eq(DEFAULT_GRAPHIC_ADDRESS),
				Mockito.eq(DEFAULT_GRAPHIC_TYPE), Mockito.eq(this.computeOrder.getImageId()),
				Mockito.eq(String.valueOf(this.hardwareRequirements.getDisk())),
				Mockito.eq(String.valueOf(this.hardwareRequirements.getMemory())), Mockito.eq(this.networkIds),
				Mockito.eq(DEFAULT_ARCHITECTURE));
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
		String defaultNetworkId = "0";
		this.networkIds.add(FAKE_PRIVATE_NETWORK_ID);

		// exercise
		List<String> networkIds = this.plugin.getNetworkIds(this.networkIds);

		// verify
		Assert.assertEquals(2, networkIds.size());
		Assert.assertEquals(defaultNetworkId, networkIds.get(0));
		Assert.assertEquals(FAKE_PRIVATE_NETWORK_ID, networkIds.get(1));
	}

	// test case: when invoking listNetworkIds with an empty list of network ids,
	// a new network ids list should be returned containing only default network id.
	@Test
	public void testListNetworkIdsEmptyNetworkIds() {
		// set up
		String defaultNetworkId = "0";

		// exercise
		List<String> networkIds = this.plugin.getNetworkIds(this.networkIds);

		// verify
		Assert.assertEquals(1, networkIds.size());
		Assert.assertEquals(defaultNetworkId, networkIds.get(0));
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
		    Assert.assertEquals("No available resources.", e.getMessage());
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
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getMemory()));
		Mockito.when(template.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getDisk()));

		TreeSet<HardwareRequirements> expected = new TreeSet<>(Arrays.asList(this.hardwareRequirements));

		// exercise
		this.plugin.updateHardwareRequirements(this.client);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getTemplatePool(Mockito.eq(this.client));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImagesSizes(Mockito.eq(this.client));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_THRICE)).convertToInteger(Mockito.anyString());
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
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(String.valueOf(this.hardwareRequirements.getMemory()));
		Mockito.when(template.xpath(TEMPLATE_DISK_SIZE_PATH)).thenReturn("0");
		Mockito.when(template.xpath(TEMPLATE_IMAGE_ID_PATH)).thenReturn(flavorId);

		// exercise
		this.plugin.updateHardwareRequirements(this.client);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getTemplatePool(Mockito.eq(this.client));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImagesSizes(Mockito.eq(this.client));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_THRICE)).convertToInteger(Mockito.anyString());
		Mockito.verify(template, Mockito.times(TestUtils.RUN_ONCE)).getId();
		Mockito.verify(template, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(template, Mockito.times(4)).xpath(Mockito.anyString());

		Assert.assertTrue(this.plugin.getFlavors().isEmpty());
	}

	// test case: When calling the getComputeInstance method passing a valid virtual
	// machine, it must obtain the data to mount an instance of this resource.
	@Test
	public void testGetComputeInstanceSuccessfully() {
		// set up
		String id = FAKE_ID;
		String name = FAKE_NAME;
		int cpu = CPU_VALUE_1;
		int memory = MEMORY_VALUE_1024;
		int disk = DISK_VALUE_30GB;
		List<String> ipAddresses = new ArrayList<String>();
		ipAddresses.add(IP_ADDRESS_ONE);
		ipAddresses.add(IP_ADDRESS_TWO);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.when(virtualMachine.getId()).thenReturn(id);
		Mockito.when(virtualMachine.getName()).thenReturn(name);
		Mockito.when(virtualMachine.lcmStateStr()).thenReturn(LCM_STATE_RUNNING);
		Mockito.when(virtualMachine.xpath(OpenNebulaComputePlugin.TEMPLATE_CPU_PATH)).thenReturn(ONE_STRING_VALUE);
		Mockito.when(virtualMachine.xpath(OpenNebulaComputePlugin.TEMPLATE_DISK_SIZE_PATH))
				.thenReturn(IMAGE_SIZE_STRING_VALUE);
		Mockito.when(virtualMachine.xpath(OpenNebulaComputePlugin.TEMPLATE_MEMORY_PATH))
				.thenReturn(MEMORY_STRING_VALUE);

		String xml = getVirtualMachineResponse();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(xml);

		ComputeInstance expected = new ComputeInstance(id, OpenNebulaStateMapper.COMPUTE_RUNNING_STATE, name, cpu, memory, disk, ipAddresses);

		// exercise
		ComputeInstance computeInstance = this.plugin.doGetComputeInstance(virtualMachine);

		// verify
		Mockito.verify(virtualMachine, Mockito.times(1)).info();
		Mockito.verify(virtualMachine, Mockito.times(1)).getId();
		Mockito.verify(virtualMachine, Mockito.times(1)).getName();
		Mockito.verify(virtualMachine, Mockito.times(1)).lcmStateStr();
		Mockito.verify(virtualMachine, Mockito.times(3)).xpath(Mockito.anyString());
		Mockito.verify(response, Mockito.times(1)).getMessage();

		Assert.assertEquals(expected, computeInstance);
	}

	// test case: When calling the getDisckSizeFromImages method with valid
	// parameters, it must return a valid disk size.
	@Test
	public void testGetDiskSizeFromImagesSuccessfully() {
		// set up
		String imageId = ONE_STRING_VALUE;
		String imageSize = IMAGE_SIZE_STRING_VALUE;
		Map<String, String> imageSizeMap = new HashMap<String, String>();
		imageSizeMap.put(imageId, imageSize);

		int expected = Integer.parseInt(imageSize);

		// exercise
		int value = this.plugin.getDiskSizeFromImageSizeMap(imageSizeMap, imageId);

		// verify
		Assert.assertEquals(expected, value);
	}
    
	// test case: When calling the getDisckSizeFromImages method with the null
	// imageSizeMap parameter, it must log the error that occurred and return the
	// value zero.
	@Test
	public void testGetDiskSizeFromImagesWithImageSizeMapNull() {
		// set up
		String imageId = FAKE_IMAGE_ID;
		Map<String, String> imageSizeMap = null;
		int expected = ZERO_VALUE;

		// exercise
		int value = this.plugin.getDiskSizeFromImageSizeMap(imageSizeMap, imageId);

		// verify
		Assert.assertEquals(expected, value);
	}
    
	// test case: When calling the getDisckSizeFromImages method with the null
	// imageId parameter, it must log the error that occurred and return the value
	// zero.
	@Test
	public void testGetDiskSizeFromImagesWithImageIdNull() {
		// set up
		String imageId = null;
		Map<String, String> imageSizeMap = new HashMap<String, String>();
		int expected = ZERO_VALUE;

		// exercise
		int value = this.plugin.getDiskSizeFromImageSizeMap(imageSizeMap, imageId);

		// verify
		Assert.assertEquals(expected, value);
	}
    
	// test case: When calling the getDisckSizeFromImages method with the two null
	// parameters, it must log the error that occurred and return the value zero.
	@Test
	public void testGetDiskSizeFromImagesWithTwoParametersNull() {
		// set up
		String imageId = null;
		Map<String, String> imageSizeMap = null;
		int expected = ZERO_VALUE;

		// exercise
		int value = this.plugin.getDiskSizeFromImageSizeMap(imageSizeMap, imageId);

		// verify
		Assert.assertEquals(expected, value);
	}
    
	// test case: When calling the convertToInteger method with a valid numeric
	// string, it will be converted to a valid integer value.
	@Test
	public void testConvertToIntegerSuccessfully() {
		// set up
		String number = TEMPLATE_CPU_VALUE;
		int expected = CPU_VALUE_2;

		// exercise
		int value = this.plugin.convertToInteger(number);

		// verify
		Assert.assertEquals(expected, value);
	}
    
	// test case: When calling the convertToInteger method with an invalid numeric
	// string, it must register the error and return the value zero.
	@Test
	public void testConvertToIntegerUnsuccessfully() {
		// set up
		String number = EMPTY_STRING;
		int expected = ZERO_VALUE;

		// exercise
		int value = this.plugin.convertToInteger(number);

		// verify
		Assert.assertEquals(expected, value);
	}
    
	// test case: When calling the containsFlavor method as an existing flavor in
	// the collection, it must return true.
	@Test
	public void testContainsFlavor() {
		// set up
		int cpu = CPU_VALUE_1;
		int memory = MEMORY_VALUE_1024;
		int disk = DISK_VALUE_6GB;
		String flavorId = FAKE_ID;
		String name = FAKE_NAME;
		HardwareRequirements flavor = new HardwareRequirements(name, flavorId, cpu, memory, disk);
		List<HardwareRequirements> flavors = new ArrayList<>();
		flavors.add(flavor);

		// exercise
		boolean condition = this.plugin.containsFlavor(flavor);

		// verify
		Assert.assertTrue(condition);
	}
    
	// test case: When calling the containsFlavor method as a flavor that does not
	// exist in the collection, it must return false.
	@Test
	public void testNotContainsFlavor() {
		// set up
		int cpu = CPU_VALUE_1;
		int memory = MEMORY_VALUE_1024;
		int disk = DISK_VALUE_6GB;
		String flavorId = FAKE_ID;
		String name = FAKE_NAME;
		String anotherName = ANOTHER_FAKE_NAME;
		HardwareRequirements flavor = new HardwareRequirements(name, flavorId, cpu, memory, disk);
		HardwareRequirements anotherFlavor = new HardwareRequirements(anotherName, flavorId, cpu, memory, disk);
		List<HardwareRequirements> flavors = new ArrayList<>();
		flavors.add(flavor);

		// exercise
		boolean condition = this.plugin.containsFlavor(anotherFlavor);

		// verify
		Assert.assertFalse(condition);
	}

	// test case: When calling the getInstance method of a resource without the volatile disk size passing
	// a valid client of a token value and an instance ID, it must return an instance of a virtual machine.
	@Test
	public void testGetInstanceSuccessfulWithoutVolatileDiskResource() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		HardwareRequirements flavor = createHardwareRequirements();
		this.flavors.add(flavor);
		this.plugin.setFlavors(this.flavors);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualMachine);

		ComputeInstance computeInstance = CreateComputeInstance();
		Mockito.doReturn(computeInstance).when(this.plugin).doGetComputeInstance(virtualMachine);

		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;

		ComputeOrder computeOrder = new ComputeOrder();
		computeOrder.setInstanceId(instanceId);

		// exercise
		this.plugin.getInstance(computeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(1)).doGetComputeInstance(virtualMachine);
	}
	
	// Test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of virtual machine will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualMachine);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate(OpenNebulaComputePlugin.SHUTS_DOWN_HARD);
		Mockito.doReturn(true).when(response).isError();

		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;

		ComputeOrder computeOrder = new ComputeOrder();
		computeOrder.setInstanceId(instanceId);

		// exercise
		this.plugin.deleteInstance(computeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString());

		Mockito.verify(virtualMachine, Mockito.times(1)).terminate(Mockito.eq(OpenNebulaComputePlugin.SHUTS_DOWN_HARD));
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// Test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualMachine);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualMachine).terminate(OpenNebulaComputePlugin.SHUTS_DOWN_HARD);
		Mockito.doReturn(false).when(response).isError();

		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;

		ComputeOrder computeOrder = new ComputeOrder();
		computeOrder.setInstanceId(instanceId);

		// exercise
		this.plugin.deleteInstance(computeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString());

		Mockito.verify(virtualMachine, Mockito.times(1)).terminate(Mockito.eq(OpenNebulaComputePlugin.SHUTS_DOWN_HARD));
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	private Template mockTemplatePoolIterator() throws UnexpectedException {
		TemplatePool templatePool = Mockito.mock(TemplatePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getTemplatePool(Mockito.any(Client.class))).willReturn(templatePool);

		Template template = Mockito.mock(Template.class);
		Iterator<Template> templateIterator = Mockito.mock(Iterator.class);
		Mockito.when(templateIterator.hasNext()).thenReturn(true, false);
		Mockito.when(templateIterator.next()).thenReturn(template);
		Mockito.when(templatePool.iterator()).thenReturn(templateIterator);
		Mockito.when(template.getId()).thenReturn(FAKE_ID);
		Mockito.when(template.getName()).thenReturn(FLAVOR_KIND_NAME);
		Mockito.when(template.xpath(TEMPLATE_CPU_PATH)).thenReturn(TEMPLATE_CPU_VALUE);
		Mockito.when(template.xpath(TEMPLATE_MEMORY_PATH)).thenReturn(TEMPLATE_MEMORY_VALUE);
		return template;
	}

	private ComputeInstance CreateComputeInstance() {
		int cpu = CPU_VALUE_8;
		int ram = MEMORY_VALUE_2048;
		int disk = DISK_VALUE_30GB;
		
		String id = FAKE_INSTANCE_ID;
		String hostName = FAKE_HOST_NAME;
		String image = FAKE_IMAGE;
		String publicKey = FAKE_PUBLIC_KEY;
		List<UserData> userData = FAKE_LIST_USER_DATA;
		
		List<String> ipAddresses = null;
		
		ComputeInstance computeInstance = new ComputeInstance(
				id,
				OpenNebulaStateMapper.COMPUTE_RUNNING_STATE,
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
	
	private ComputeOrder createComputeOrder(List<String> networksId, int ...values) {
		int cpu = values[0];
		int memory = values[1];
		int disk = values[2];
		
		String imageId = FAKE_IMAGE_ID;
		String name = null, providingMember = null, requestingMember = null, cloudName = null;
		String publicKey = FAKE_PUBLIC_KEY;
		
		SystemUser systemUser = null;
		ArrayList<UserData> userData = FAKE_LIST_USER_DATA;
		
		ComputeOrder computeOrder = new ComputeOrder(
				systemUser,
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
	
	private CloudUser createCloudUser() {
		String userId = FAKE_ID;
		String userName = FAKE_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;

		return new CloudUser(userId, userName, tokenValue);
	}
	
	private String generateTemplate(int choice, String ...args) {
		String cpu;
		String memory;
		String defaultNetworkId;
		String privateNetworkId;
		String size;
		String template;
		
		switch (choice) {
		case 0:
			cpu = args[0];
			memory = args[1];
			defaultNetworkId = args[2] != null ? args[2] : String.valueOf(DEFAULT_NETWORK_ID);
			privateNetworkId = args[3];
			size = args[4];
			template = getTemplateWithTwoNetworkIds();
			return String.format(template, cpu, memory, defaultNetworkId, privateNetworkId, size);
			
		case 1:
			cpu = args[0];
			memory = args[1];
			defaultNetworkId = args[2] != null ? args[2] : String.valueOf(DEFAULT_NETWORK_ID);
			size = args[3];
			template = getTemplateWithOneNetworkId();
			return String.format(template, cpu, memory, defaultNetworkId, size);
			
		case 2:
			cpu = args[0];
			memory = args[1];
			defaultNetworkId = args[2] != null ? args[2] : String.valueOf(DEFAULT_NETWORK_ID);
			size = args[3];
			template = getTemplateWithVolatileDisk();
			return String.format(template, cpu, memory, defaultNetworkId, size);

		default:
			return null;
		}
	}

	private String getTemplateWithVolatileDisk() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
				+ "<TEMPLATE>\n"
				+ "    <CONTEXT>\n" 
				+ "        <USERDATA_ENCODING>base64</USERDATA_ENCODING>\n"
				+ "        <NETWORK>YES</NETWORK>\n"
				+ "        <USERDATA>fake-user-data</USERDATA>\n"
				+ "    </CONTEXT>\n" 
				+ "    <CPU>%s</CPU>\n" 
				+ "    <DISK>\n"
				+ "        <FORMAT>ext3</FORMAT>\n"
				+ "        <SIZE>6144</SIZE>\n"
				+ "        <TYPE>fs</TYPE>\n"
				+ "    </DISK>\n"
				+ "    <GRAPHICS>\n" 
				+ "        <LISTEN>0.0.0.0</LISTEN>\n" 
				+ "        <TYPE>vnc</TYPE>\n"
				+ "    </GRAPHICS>\n"
				+ "    <MEMORY>%s</MEMORY>\n"
				+ "    <NIC>\n"
				+ "        <NETWORK_ID>%s</NETWORK_ID>\n"
				+ "    </NIC>\n"
				+ "    <OS>\n" 
				+ "        <ARCH>x86_64</ARCH>\n"
				+ "    </OS>\n"
				+ "</TEMPLATE>\n";
		
		return template;
	}
	
	private String getTemplateWithTwoNetworkIds() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
				+ "<TEMPLATE>\n"
				+ "    <CONTEXT>\n" 
				+ "        <USERDATA_ENCODING>base64</USERDATA_ENCODING>\n"
				+ "        <NETWORK>YES</NETWORK>\n"
				+ "        <USERDATA>fake-user-data</USERDATA>\n"
				+ "    </CONTEXT>\n" 
				+ "    <CPU>%s</CPU>\n" 
				+ "    <DISK>\n"
				+ "        <IMAGE_ID>fake-image-id</IMAGE_ID>\n" 
				+ "    </DISK>\n"
				+ "    <GRAPHICS>\n" 
				+ "        <LISTEN>0.0.0.0</LISTEN>\n" 
				+ "        <TYPE>vnc</TYPE>\n"
				+ "    </GRAPHICS>\n"
				+ "    <MEMORY>%s</MEMORY>\n"
				+ "    <NIC>\n"
				+ "        <NETWORK_ID>%s</NETWORK_ID>\n"
				+ "    </NIC>\n"
				+ "    <NIC>\n"
				+ "        <NETWORK_ID>%s</NETWORK_ID>\n"
				+ "    </NIC>\n"
				+ "    <OS>\n" 
				+ "        <ARCH>x86_64</ARCH>\n"
				+ "    </OS>\n"
				+ "</TEMPLATE>\n";
		
		return template;
	}
	
	private String getTemplateWithOneNetworkId() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <CONTEXT>\n"
				+ "        <USERDATA_ENCODING>base64</USERDATA_ENCODING>\n" 
				+ "        <NETWORK>YES</NETWORK>\n"
				+ "        <USERDATA>fake-user-data</USERDATA>\n"
				+ "    </CONTEXT>\n"
				+ "    <CPU>%s</CPU>\n"
				+ "    <DISK>\n"
				+ "        <IMAGE_ID>fake-image-id</IMAGE_ID>\n"
				+ "    </DISK>\n"
				+ "    <GRAPHICS>\n" 
				+ "        <LISTEN>0.0.0.0</LISTEN>\n"
				+ "        <TYPE>vnc</TYPE>\n"
				+ "    </GRAPHICS>\n"
				+ "    <MEMORY>%s</MEMORY>\n"
				+ "    <NIC>\n"
				+ "        <NETWORK_ID>%s</NETWORK_ID>\n"
				+ "    </NIC>\n"
				+ "    <OS>\n" 
				+ "        <ARCH>x86_64</ARCH>\n"
				+ "    </OS>\n"
				+ "</TEMPLATE>\n";
		
		return template;
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
				.memory(String.valueOf(this.hardwareRequirements.getMemory()))
				.networks(this.networkIds)
				.architecture(DEFAULT_ARCHITECTURE)
				.build();

		return request;
	}
}
