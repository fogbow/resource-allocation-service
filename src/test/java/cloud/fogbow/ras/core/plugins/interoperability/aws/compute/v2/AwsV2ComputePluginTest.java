package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CpuOptions;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceAssociation;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsV2ComputePluginTest extends BaseUnitTests {

    private static final String ANY_VALUE = "anything";
    private static final String AWS_TAG_NAME = "Name";
    private static final String CLOUD_NAME = "amazon";
    private static final String FAKE_DEFAULT_SECURITY_GROUP_ID = "fake-default-security-group-id";
    private static final String FAKE_IP_ADDRESS = "0.0.0.0";
    private static final String FAKE_SUBNET_ID = "fake-subnet-id";
    private static final String TEST_INSTANCE_TYPE = "t2.micro";

    private static final int INSTANCE_TYPE_CPU_VALUE = 1;
    private static final int FLAVOR_MEMORY_VALUE = 1;
    private static final int ZERO_VALUE = 0;
	
    private AwsV2ComputePlugin plugin;
    private Ec2Client client;
    private LaunchCommandGenerator launchCommandGenerator;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.launchCommandGenerator = Mockito.mock(LaunchCommandGenerator.class);
        String awsConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator 
                + CLOUD_NAME 
                + File.separator 
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsV2ComputePlugin(awsConfFilePath));
        this.plugin.setLaunchCommandGenerator(launchCommandGenerator);
        this.client = this.testUtils.getAwsMockedClient();
    }
	
    // test case: When calling the requestInstance method, with a compute order and
    // cloud user valid, a client is invoked to run instances, returning its ID.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        List networkOrderIds = getNetworkOrderIds();
        ComputeOrder order = this.testUtils.createLocalComputeOrder(networkOrderIds);
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        AwsHardwareRequirements flavor = createFlavor(null);
        Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(Mockito.eq(order), Mockito.eq(cloudUser));

        RunInstancesRequest request = buildRunInstancesResquest(order, flavor);
        Mockito.doReturn(request).when(this.plugin).buildResquestInstance(Mockito.eq(order), Mockito.eq(flavor));

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(order),
                Mockito.eq(flavor), Mockito.eq(request), Mockito.eq(this.client));

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findSmallestFlavor(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResquestInstance(Mockito.eq(order),
                Mockito.eq(flavor));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(order),
                Mockito.eq(flavor), Mockito.eq(request), Mockito.eq(this.client));
    }

    // test case: check if the calls are made as expected when getInstance is
    // invoked properly
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        Mockito.doNothing().when(this.plugin).updateHardwareRequirements(Mockito.eq(cloudUser));

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.eq(order), Mockito.eq(this.client));

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .updateHardwareRequirements(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(this.client));
    }
	
    // test case: When calling the deleteInstance method, with a compute order and
    // cloud user valid, the instance in the cloud must be terminated.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        
        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));
    }
	
	// test case: When calling the isReady method with the cloud state RUNNING,
	// this means that the state of compute is READY and it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.RUNNING_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}

	// test case: When calling the isReady method with the cloud states different
	// than RUNNING, this means that the state of compute is not READY and it must
	// return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String[] cloudStates = { ANY_VALUE, AwsV2StateMapper.PENDING_STATE, AwsV2StateMapper.SHUTTING_DOWN_STATE,
				AwsV2StateMapper.STOPPING_STATE };

		for (String cloudState : cloudStates) {
			// exercise
			boolean status = this.plugin.isReady(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}
	
	// test case: Whenever you call the hasFailed method, no matter the value, it
	// must return false.
	@Test
	public void testHasFailed() {
		// set up
		String cloudState = ANY_VALUE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the getMemoryValueFrom method, with a empty set
	// flavors, it must return a zero value.
	@Test
	public void testGetMemoryValueWithASetFlavorsEmpty() {
		// set up
		InstanceType instanceType = InstanceType.T1_MICRO;
		int expected = ZERO_VALUE;

		// exercise
		int memory = this.plugin.getMemoryValueFrom(instanceType);

		// verify
		Assert.assertTrue(this.plugin.getFlavors().isEmpty());
		Assert.assertEquals(expected, memory);
	}
	
    // test case: When calling the doDeleteInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteInstance() throws FogbowException {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        Mockito.when(this.client.terminateInstances(Mockito.eq(request)))
                .thenReturn(TerminateInstancesResponse.builder().build());

        // exercise
        this.plugin.doDeleteInstance(instanceId, client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).terminateInstances(Mockito.eq(request));
    }
    
    // test case: When calling the doDeleteInstance method, and an unexpected error
    // occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoDeleteInstanceFail() {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        Mockito.when(this.client.terminateInstances(Mockito.any(TerminateInstancesRequest.class)))
                .thenThrow(SdkClientException.builder().build());

        String expected = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, AwsV2ComputePlugin.RESOURCE_NAME,
                instanceId);

        try {
            // exercise
            this.plugin.doDeleteInstance(instanceId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();

        DescribeInstancesResponse response = buildInstanceResponse();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.describeInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client)))
                .thenReturn(response);

        Instance instance = buildInstance();
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.eq(response))).thenReturn(instance);

        List volumes = buildVolumesCollection();
        Mockito.when(AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client)))
                .thenReturn(volumes);
        
        ComputeInstance computeInstance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstance).when(this.plugin).buildComputeInstance(Mockito.eq(instance),
                Mockito.eq(volumes));

        // exercise
        this.plugin.doGetInstance(order, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.describeInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getInstanceReservation(Mockito.eq(response));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildComputeInstance(Mockito.eq(instance),
                Mockito.eq(volumes));
    }
    
    // test case: When calling the buildComputeInstance method, it must verify
    // that is call was successful.
    @Test
    public void testBuildComputeInstance() {
        // set up
        Instance instance = buildInstance();
        Mockito.doReturn(FLAVOR_MEMORY_VALUE).when(this.plugin).getMemoryValueFrom(Mockito.eq(instance.instanceType()));
        
        List<Volume> volumes = buildVolumesCollection();
        Mockito.doReturn(AwsV2ComputePlugin.ONE_GIGABYTE).when(this.plugin).getAllDisksSize(Mockito.eq(volumes));
        
        List<String> ipAddresses = buildIpAdressesCollection();
        Mockito.doReturn(ipAddresses).when(this.plugin).getIpAddresses(Mockito.eq(instance));

        // exercise
        this.plugin.buildComputeInstance(instance, volumes);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getMemoryValueFrom(Mockito.eq(instance.instanceType()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAllDisksSize(Mockito.eq(volumes));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getIpAddresses(Mockito.eq(instance));
    }
	
	// test case: When calling the getIpAddresses method, it must verify
    // that is call was successful.
	@Test
	public void testGetIpAddresses() {
	    // set up
	    
	    // exercise
	    
	    // verify
	}
	
//	// test case: When calling the findSmallestFlavor method, with a compute order
//	// and cloud user valid, and return the null result, the
//	// NoAvailableResourcesException will be thrown.
//	@Test(expected = NoAvailableResourcesException.class) // verify
//	public void testFindSmallestFlavorUnsuccessful() throws FogbowException {
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//
//		ComputeOrder computeOrder = createComputeOrder(null);
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//		Mockito.doNothing().when(this.plugin).updateHardwareRequirements(cloudUser);
//
//		TreeSet<AwsHardwareRequirements> flavors = new TreeSet<AwsHardwareRequirements>();
//		Map<String, String> requirements = null;
//		AwsHardwareRequirements flavor = createFlavor(requirements);
//		flavors.add(flavor);
//		Mockito.doReturn(flavors).when(this.plugin).getFlavorsByRequirements(requirements);
//
//		// exercise
//		this.plugin.findSmallestFlavor(computeOrder, cloudUser);
//	}
	
//	// test case: When calling the getFlavorsByRequirements method, with a
//	// requirements map, it must filter the possibilities according to that map,
//	// returning the corresponding results.
//	@Test
//	public void testGetFlavorsByRequirementsSuccessful() throws FogbowException {
//
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//		
//		DescribeImagesResponse response = createDescribeImage();
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//		this.plugin.updateHardwareRequirements(cloudUser);
//
//		Map<String, String> requirements = new HashMap<String, String>();
//		requirements.put(AwsV2ComputePlugin.STORAGE_REQUIREMENT, "1x75-SSD-NVMe");
//		requirements.put(AwsV2ComputePlugin.BANDWIDTH_REQUIREMENT, "<3500");
//		requirements.put(AwsV2ComputePlugin.PERFORMANCE_REQUIREMENT, "<10");
//		requirements.put(AwsV2ComputePlugin.PROCESSOR_REQUIREMENT, "Intel_Xeon_Platinum_8175_3.1GHz");
//
//		ComputeOrder computeOrder = createComputeOrder(requirements);
//		int expected = AMOUNT_SSD_STORAGE;
//
//		// exercise
//		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());
//
//		// verify
//		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
//		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
//
//		Mockito.verify(this.plugin, Mockito.times(4)).filterFlavors(Mockito.any(), Mockito.any());
//		Assert.assertEquals(expected, flavors.size());
//	}
	
//	// test case: When calling the getFlavorsByRequirements method with a
//	// requirements map containing high-level graphical attributes, it must filter
//	// the possibilities according to that map and return a set with a
//	// higher-performing instance type.
//	@Test
//	public void testGetFlavorsByRequirementsWithHighPerformanceGraphic() throws FogbowException {
//
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//		
//		DescribeImagesResponse response = createDescribeImage();
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//		this.plugin.updateHardwareRequirements(cloudUser);
//
//		Map<String, String> requirements = new HashMap<String, String>();
//		requirements.put(AwsV2ComputePlugin.GRAPHIC_SHARING_REQUIREMENT, "NVLink");
//		requirements.put(AwsV2ComputePlugin.GRAPHIC_PROCESSOR_REQUIREMENT, "8");
//		requirements.put(AwsV2ComputePlugin.GRAPHIC_MEMORY_REQUIREMENT, "256");
//
//		ComputeOrder computeOrder = createComputeOrder(requirements);
//		String expected = InstanceType.P3_DN_24_XLARGE.toString();
//
//		// exercise
//		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());
//
//		// verify
//		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
//		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
//
//		Mockito.verify(this.plugin, Mockito.times(3)).filterFlavors(Mockito.any(), Mockito.any());
//		Assert.assertEquals(expected, flavors.first().getName());
//	}
	
//	// test case: When calling the getFlavorsByRequirements method with a
//	// requirements map containing the most demanding graphics emulation attributes,
//	// it must filter the possibilities according to that map and return a set with
//	// an instance type with the highest performance of this level.
//	@Test
//	public void testGetFlavorsByRequirementsWithGraphicEmulationAtribute() throws FogbowException {
//
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//		
//		DescribeImagesResponse response = createDescribeImage();
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//		this.plugin.updateHardwareRequirements(cloudUser);
//
//		Map<String, String> requirements = new HashMap<String, String>();
//		requirements.put(AwsV2ComputePlugin.PROCESSOR_REQUIREMENT, "Intel_Xeon_E5-2686_v4_2.3GHz");
//		requirements.put(AwsV2ComputePlugin.GRAPHIC_EMULATION_REQUIREMENT, "8");
//
//		ComputeOrder computeOrder = createComputeOrder(requirements);
//		String expected = InstanceType.F1_16_XLARGE.toString();
//
//		// exercise
//		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());
//
//		// verify
//		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
//		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
//
//		Mockito.verify(this.plugin, Mockito.times(2)).filterFlavors(Mockito.any(), Mockito.any());
//		Assert.assertEquals(expected, flavors.first().getName());
//	}
	
//	// test case: When calling the getFlavorsByRequirements method, with a null map,
//	// there will be no filter to limit the results, returning all the flavors
//	// obtained in the last update.
//	@Test
//	public void testGetFlavorsByRequirementsWithNullMap() throws FogbowException {
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//		
//		DescribeImagesResponse response = createDescribeImage();
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//		this.plugin.updateHardwareRequirements(cloudUser);
//
//		Map<String, String> requirements = null;
//		ComputeOrder computeOrder = createComputeOrder(requirements);
//		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1;
//
//		// exercise
//		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());
//
//		// verify
//		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
//		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
//
//		Assert.assertEquals(expected, flavors.size());
//	}
	
//	// test case: When calling the getFlavorsByRequirements method, with an empty
//	// map, there will be no filter to limit the results, returning all the flavors
//	// obtained in the last update.
//	@Test
//	public void testGetFlavorsByRequirementsWithEmptyMap() throws FogbowException {
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//		
//		DescribeImagesResponse response = createDescribeImage();
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//		this.plugin.updateHardwareRequirements(cloudUser);
//
//		Map<String, String> requirements = new HashMap<String, String>();
//		ComputeOrder computeOrder = createComputeOrder(requirements);
//		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1;
//
//		// exercise
//		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());
//
//		// verify
//		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
//		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
//
//		Assert.assertEquals(expected, flavors.size());
//	}
		
//	// test case: When calling the loadLinesFromFlavorFile method, without a valid
//	// file path, the ConfigurationErrorException will be thrown.
//	@Test(expected = ConfigurationErrorException.class) // verify
//	public void testLoadLinesFromFlavorFileUnsuccessful() throws ConfigurationErrorException {
//		// set up
//		Mockito.doReturn(ANY_VALUE).when(this.plugin).getFlavorsFilePath();
//
//		// exercise
//		this.plugin.loadLinesFromFlavorFile();
//	}
		
//	// test case: When calling the updateHardwareRequirements method, with a valid
//	// cloud user, there will be updating the set of available flavors.
//	@Test
//	public void testUpdateHardwareRequirementsSuccessful() throws FogbowException {
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//		
//		DescribeImagesResponse response = createDescribeImage();
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
//
//		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1;
//
//		// exercise
//		this.plugin.updateHardwareRequirements(cloudUser);
//
//		// verify
//		Assert.assertEquals(expected, this.plugin.getFlavors().size());
//	}

//	// test case: When calling the getImageById method, and no image is returned ,
//	// an InstanceNotFoundException will be thrown.
//	@Test(expected = InstanceNotFoundException.class) // verify
//	public void testGetImageByIdUnsuccessful() throws FogbowException {
//		// set up
//		Ec2Client client = Mockito.mock(Ec2Client.class);
//		PowerMockito.mockStatic(AwsV2ClientUtil.class);
//		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
//
//		DescribeImagesResponse response = null;
//		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
//
//		String imageId = TestUtils.FAKE_IMAGE_ID;
//
//		// exercise
//		this.plugin.getImageById(imageId, client);
//	}
	
	private void mockRunningInstance(Ec2Client client) {
		CpuOptions cpuOptions = CpuOptions.builder()
				.coreCount(1)
				.build();
		
		Instance instance = Instance.builder()
				.cpuOptions(cpuOptions)
				.instanceId(TestUtils.FAKE_INSTANCE_ID)
				.build();

		RunInstancesResponse response = RunInstancesResponse.builder()
				.instances(instance)
				.build();

		Mockito.when(client.runInstances(Mockito.any(RunInstancesRequest.class))).thenReturn(response);
	}
	
	private DescribeVolumesResponse createVolumeResponse() {
		Volume volume = Volume.builder()
				.volumeId(TestUtils.FAKE_VOLUME_ID)
				.size(AwsV2ComputePlugin.ONE_GIGABYTE)
				.build();
		
		DescribeVolumesResponse response = DescribeVolumesResponse.builder()
				.volumes(volume)
				.build();
		
		return response;
	}
	
	private DescribeImagesResponse createDescribeImage() {
		EbsBlockDevice ebsBlockDevice = EbsBlockDevice.builder()
				.volumeSize(AwsV2ComputePlugin.ONE_GIGABYTE)
				.build();
		
		BlockDeviceMapping blockDeviceMapping = BlockDeviceMapping.builder()
				.ebs(ebsBlockDevice)
				.build();
		
        Image image = Image.builder()
        		.imageId(TestUtils.FAKE_IMAGE_ID)
        		.blockDeviceMappings(blockDeviceMapping)
        		.build();
        
		return DescribeImagesResponse.builder()
				.images(image)
				.build();
	}
	
//	private ComputeOrder createComputeOrder(Map<String, String> requirements) {
//		int cpu = 2;
//		int memory = 627;
//		int disk = 8;
//		
//		String imageId = TestUtils.FAKE_IMAGE_ID;
//		String name = null, providingMember = null, requestingMember = null, cloudName = null;
//		String publicKey = TestUtils.FAKE_PUBLIC_KEY;
//		
//		SystemUser systemUser = null;
//		List<String> networksId = null;
//		ArrayList<UserData> userData = testUtils.createUserDataList();
//		
//		ComputeOrder computeOrder = new ComputeOrder(
//				systemUser,
//				requestingMember, 
//				providingMember,
//				cloudName,
//				name, 
//				cpu, 
//				memory, 
//				disk, 
//				imageId,
//				userData,
//				publicKey, 
//				networksId);
//		
//		computeOrder.setCloudName(CLOUD_NAME);
//		computeOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
//		computeOrder.setRequirements(requirements);
//		this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
//		
//		return computeOrder;
//	}
	
	private List<String> buildIpAdressesCollection() {
        String[] ipAdresses = { FAKE_IP_ADDRESS }; 
        return Arrays.asList(ipAdresses);
    }
	
    private List<Volume> buildVolumesCollection() {
        Volume[] volumes = { 
                Volume.builder()
                    .volumeId(TestUtils.FAKE_VOLUME_ID)
                    .size(AwsV2ComputePlugin.ONE_GIGABYTE)
                    .build() 
                };
        return Arrays.asList(volumes);
    }
	
    private Instance buildInstance() {
        EbsInstanceBlockDevice ebs = EbsInstanceBlockDevice.builder()
                .volumeId(TestUtils.FAKE_VOLUME_ID)
                .build();
        
        InstanceBlockDeviceMapping blockDeviceMapping = InstanceBlockDeviceMapping.builder()
                .ebs(ebs)
                .build();
        
        CpuOptions cpuOptions = CpuOptions.builder()
                .coreCount(1)
                .build();
        
        InstancePrivateIpAddress instancePrivateIpAddress = InstancePrivateIpAddress.builder()
                .privateIpAddress(FAKE_IP_ADDRESS)
                .build();
        
        InstanceNetworkInterfaceAssociation association = InstanceNetworkInterfaceAssociation.builder()
                .publicIp(FAKE_IP_ADDRESS)
                .build();
        
        InstanceNetworkInterface instanceNetworkInterface = InstanceNetworkInterface.builder()
                .privateIpAddresses(instancePrivateIpAddress)
                .association(association)
                .build();
        
        InstanceState instanceState = InstanceState.builder()
                .name(AwsV2StateMapper.AVAILABLE_STATE)
                .build();
        
        Tag tag = Tag.builder()
                .key(AWS_TAG_NAME)
                .value(TestUtils.FAKE_INSTANCE_NAME)
                .build();
        
        Instance instance = Instance.builder()
                .blockDeviceMappings(blockDeviceMapping)
                .cpuOptions(cpuOptions)
                .imageId(TestUtils.FAKE_INSTANCE_ID)
                .instanceId(TestUtils.FAKE_INSTANCE_ID)
                .instanceType(InstanceType.T1_MICRO)
                .networkInterfaces(instanceNetworkInterface)
                .state(instanceState)
                .tags(tag)
                .build();
        
        return instance;
    }
    
    private DescribeInstancesResponse buildInstanceResponse() {
        Instance instance = buildInstance();
        
        Reservation reservation = Reservation.builder()
                .instances(instance)
                .build();
        
        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(reservation)
                .build();
        
        return response;
    }
    
    private RunInstancesRequest buildRunInstancesResquest(ComputeOrder order, AwsHardwareRequirements flavor) {
        RunInstancesRequest request = RunInstancesRequest.builder()
                .imageId(flavor.getImageId())
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(AwsV2ComputePlugin.INSTANCES_LAUNCH_NUMBER)
                .minCount(AwsV2ComputePlugin.INSTANCES_LAUNCH_NUMBER)
                .networkInterfaces(buildNetworkInterfaceCollection())
                .userData(this.launchCommandGenerator.createLaunchCommand(order))
                .build();
        
        return request;
    }

    private List<InstanceNetworkInterfaceSpecification> buildNetworkInterfaceCollection() {
        InstanceNetworkInterfaceSpecification[] networkInterfaces = { 
                InstanceNetworkInterfaceSpecification.builder()
                    .subnetId(FAKE_SUBNET_ID)
                    .deviceIndex(ZERO_VALUE)
                    .groups(FAKE_DEFAULT_SECURITY_GROUP_ID)
                    .build() 
                };
        return Arrays.asList(networkInterfaces);
    }
    
    private AwsHardwareRequirements createFlavor(Map<String, String> requirements) {
        String name = TEST_INSTANCE_TYPE;
        String flavorId = TestUtils.FAKE_INSTANCE_ID;
        int cpu = INSTANCE_TYPE_CPU_VALUE;
        int memory = TestUtils.MEMORY_VALUE;
        int disk = TestUtils.DISK_VALUE;
        String imageId = TestUtils.FAKE_IMAGE_ID;
        return new AwsHardwareRequirements(name, flavorId, cpu, memory, disk, imageId, requirements);
    }
	
    private List<String> getNetworkOrderIds() {
        String[] networkOrderIds = { FAKE_SUBNET_ID };
        return Arrays.asList(networkOrderIds);
    }
    
}
