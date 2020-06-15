package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import cloud.fogbow.common.exceptions.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.UnacceptableOperationException;
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
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
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
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsComputePluginTest extends BaseUnitTests {

    private static final String ANY_VALUE = "anything";
    private static final String AWS_TAG_NAME = "Name";
    private static final String BANDWIDTH_REQUIREMENT_VALUE = "not-defined";
    private static final String CLOUD_NAME = "amazon";
    private static final String DEFAULT_SUBNET_ID = "default-subnet-id";
    private static final String FAKE_GROUP_ID = "fake-group-id";
    private static final String FAKE_IP_ADDRESS = "0.0.0.0";
    private static final String FAKE_SUBNET_ID = "fake-subnet-id";
    private static final String FLAVOR_LINE_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s";
    private static final String NETWORK_PERFORMANCE_REQUIREMENT_VALUE = "LowToModerate";
    private static final String NO_SUPPORT_REQUIREMENT_VALUE = "No Support";
    private static final String PROCESSOR_REQUIREMENT_VALUE = "Intel_Xeon_3.3GHz";
    private static final String STORAGE_REQUIREMENT_VALUE = "EBS-Only";
    private static final String VCPU_TEST_KEY = "vcpu-test-key";

    private static final int FLAVOR_CPU_VALUE = 1;
    private static final int FLAVOR_MEMORY_VALUE = 1;
    private static final int INSTANCE_TYPE_LIMIT_VALUE = 5;
    private static final int ZERO_VALUE = 0;
	
    private AwsComputePlugin plugin;
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

        this.plugin = Mockito.spy(new AwsComputePlugin(awsConfFilePath));
        this.plugin.setLaunchCommandGenerator(launchCommandGenerator);
        this.client = this.testUtils.getAwsMockedClient();
    }
	
    // test case: When calling the requestInstance method, with a compute order and
    // cloud user valid, a client is invoked to run instances, returning its ID.
    @Test
    public void testRequestInstance() throws Exception {
        // set up
        List networkIds = createNetworkIds();
        ComputeOrder order = this.testUtils.createLocalComputeOrder(networkIds);
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        AwsHardwareRequirements flavor = createFlavor(null);
        Mockito.doReturn(flavor).when(this.plugin).findSmallestFlavor(Mockito.eq(order), Mockito.eq(cloudUser));

        Subnet subnet = buildSubnet();
        Mockito.doReturn(subnet).when(this.plugin).getNetworkSelected(Mockito.eq(order), Mockito.eq(this.client));

        RunInstancesRequest request = buildRunInstancesResquest(order, flavor);
        Mockito.doReturn(request).when(this.plugin).buildRequestInstance(Mockito.eq(order), Mockito.eq(flavor), Mockito.eq(subnet));

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(order),
                Mockito.eq(flavor), Mockito.eq(request), Mockito.eq(this.client));

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findSmallestFlavor(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkSelected(Mockito.eq(order),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildRequestInstance(Mockito.eq(order),
                Mockito.eq(flavor), Mockito.eq(subnet));
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
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .updateHardwareRequirements(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order.getInstanceId()),
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
    public void testIsReady() {
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
    public void testIsNotReady() {
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

        String expected = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, AwsComputePlugin.RESOURCE_NAME,
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
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        DescribeInstancesResponse response = buildInstanceResponse();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.doDescribeInstanceById(Mockito.eq(instanceId), Mockito.eq(this.client)))
                .thenReturn(response);

        Instance instance = buildInstance();
        Mockito.when(AwsV2CloudUtil.getInstanceFrom(Mockito.eq(response))).thenReturn(instance);

        List volumes = createVolumesCollection();
        Mockito.when(AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client)))
                .thenReturn(volumes);
        
        ComputeInstance computeInstance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstance).when(this.plugin).buildComputeInstance(Mockito.eq(instance),
                Mockito.eq(volumes));

        // exercise
        this.plugin.doGetInstance(instanceId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeInstanceById(Mockito.eq(instanceId), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getInstanceFrom(Mockito.eq(response));

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

        List<Volume> volumes = createVolumesCollection();
        Mockito.doReturn(AwsComputePlugin.ONE_GIGABYTE).when(this.plugin).getAllDisksSize(Mockito.eq(volumes));

        List<String> ipAddresses = buildIpAdressesCollection();
        Mockito.doReturn(ipAddresses).when(this.plugin).getIpAddresses(Mockito.eq(instance));

        // exercise
        this.plugin.buildComputeInstance(instance, volumes);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getMemoryValueFrom(Mockito.eq(instance.instanceType()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAllDisksSize(Mockito.eq(volumes));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getIpAddresses(Mockito.eq(instance));
    }
	
    // test case: When calling the getIpAddresses method, it must verify
    // that is call was successful.
    @Test
    public void testGetIpAddresses() {
        // set up
        Instance instance = buildInstance();

        List ipAddresses = buildIpAdressesCollection();
        Mockito.doReturn(ipAddresses).when(this.plugin).getPrivateIpAddresses(Mockito.eq(instance), Mockito.anyInt());
        Mockito.doReturn(FAKE_IP_ADDRESS).when(this.plugin).getPublicIpAddresses(Mockito.eq(instance),
                Mockito.anyInt());

        // exercise
        this.plugin.getIpAddresses(instance);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getPrivateIpAddresses(Mockito.eq(instance),
                Mockito.anyInt());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getPublicIpAddresses(Mockito.eq(instance),
                Mockito.anyInt());
    }
    
    // test case: When calling the getPublicIpAddresses method, it must verify that
    // the obtained IP address is the same as expected.
    @Test
    public void testGetPublicIpAddresses() {
        // set up
        Instance instance = buildInstance();
        int index = ZERO_VALUE;

        String expected = FAKE_IP_ADDRESS;

        // exercise
        String ipAdress = this.plugin.getPublicIpAddresses(instance, index);

        // verify
        Assert.assertSame(expected, ipAdress);
    }
    
    // test case: When calling the getPrivateIpAddresses method, it must verify that
    // the obtained IPs addresses is the equals as expected.
    @Test
    public void testGetPrivateIpAddresses() {
        // set up
        Instance instance = buildInstance();
        int index = ZERO_VALUE;

        List expected = buildIpAdressesCollection();

        // exercise
        List ipAdresses = this.plugin.getPrivateIpAddresses(instance, index);

        // verify
        Assert.assertEquals(expected, ipAdresses);
    }
    
    // test case: When calling the getAllDisksSize method, it must verify that
    // the obtained size is the equals as expected.
    @Test
    public void testGetAllDisksSize() {
        // set up
        List volumes = createVolumesCollection();
        int expected = AwsComputePlugin.ONE_GIGABYTE;
        
        // exercise
        int size = this.plugin.getAllDisksSize(volumes);
        
        // verify
        Assert.assertEquals(expected, size);
    }
    
    // test case: When calling the getMemoryValueFrom method, it must verify that
    // the obtained memory is the equals as expected.
    @Test
    public void testGetMemoryValueFrom() {
        // set up
        AwsHardwareRequirements flavor = createFlavor(null);
        TreeSet flavors = createFlavorsCollection(flavor);
        Mockito.doReturn(flavors).when(this.plugin).getFlavors();

        int expected = TestUtils.MEMORY_VALUE;

        // exercise
        int memory = this.plugin.getMemoryValueFrom(InstanceType.T2_MICRO);

        // verify
        Assert.assertEquals(expected, memory);
    }
    
    // test case: When calling the doRequestInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoRequestInstance() throws Exception {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsHardwareRequirements flavor = createFlavor(null);
        Instance instance = mockRunningInstance();
        String tagName = AwsV2CloudUtil.AWS_TAG_NAME;

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD,
                Mockito.eq(instance.instanceId()), Mockito.eq(tagName), Mockito.eq(order.getName()),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).updateInstanceAllocation(Mockito.eq(order), Mockito.eq(flavor),
                Mockito.eq(instance), Mockito.eq(this.client));

        RunInstancesRequest request = buildRunInstancesResquest(order, flavor);

        // exercise
        this.plugin.doRequestInstance(order, flavor, request, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).runInstances(Mockito.eq(request));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class);
        AwsV2CloudUtil.createTagsRequest(Mockito.eq(instance.instanceId()), Mockito.eq(tagName),
                Mockito.eq(order.getName()), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateInstanceAllocation(Mockito.eq(order),
                Mockito.eq(flavor), Mockito.eq(instance), Mockito.eq(this.client));
    }
    
    // test case: When calling the doRequestInstance method, and an unexpected error
    // occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoRequestInstanceFail() throws Exception {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsHardwareRequirements flavor = createFlavor(null);
        RunInstancesRequest request = buildRunInstancesResquest(order, flavor);
        
        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.runInstances(request)).thenThrow(exception);
        
        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doRequestInstance(order, flavor, request, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the updateInstanceAllocation method, it must verify
    // that is call was successful.
    @Test
    public void testUpdateInstanceAllocation() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsHardwareRequirements flavor = createFlavor(null);
        Instance instance = mockRunningInstance();

        String imageId = TestUtils.FAKE_IMAGE_ID;
        Image image = buildImage();

        Mockito.doReturn(image).when(this.plugin).getImageById(Mockito.eq(imageId), Mockito.eq(client));
        Mockito.doReturn(AwsComputePlugin.ONE_GIGABYTE).when(this.plugin).getImageSize(Mockito.eq(image));

        // exercise
        this.plugin.updateInstanceAllocation(order, flavor, instance, client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageById(Mockito.eq(imageId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageSize(Mockito.eq(image));
    }
    
    // test case: When calling the getImageById method, it must verify
    // that is call was successful.
    @Test
    public void testGetImageById() throws Exception {
        // set up
        String imageId = TestUtils.FAKE_IMAGE_ID;

        DescribeImagesRequest request = DescribeImagesRequest.builder()
                .imageIds(imageId)
                .build();

        DescribeImagesResponse response = buildDescribeImages();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(response).when(AwsV2CloudUtil.class, TestUtils.DO_DESCRIBE_IMAGES_REQUEST_METHOD,
                Mockito.eq(request), Mockito.eq(this.client));

        Image expected = buildImage();

        // exercise
        Image image = this.plugin.getImageById(imageId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class);
        AwsV2CloudUtil.doDescribeImagesRequest(Mockito.eq(request), Mockito.eq(this.client));

        Assert.assertEquals(expected, image);
    }
    
    // test case: When calling the getImageById method, with a null response or
    // empty image, it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testGetImageByIdFail() throws Exception {
        // set up
        String imageId = TestUtils.FAKE_IMAGE_ID;

        DescribeImagesRequest request = DescribeImagesRequest.builder()
                .imageIds(imageId)
                .build();

        DescribeImagesResponse response = DescribeImagesResponse.builder().build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(response).when(AwsV2CloudUtil.class, TestUtils.DO_DESCRIBE_IMAGES_REQUEST_METHOD,
                Mockito.eq(request), Mockito.eq(this.client));

        String expected = Messages.Exception.IMAGE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getImageById(imageId, this.client);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the buildRequestInstance method, it must verify
    // that is call was successful.
    @Test
    public void testBuildRequestInstance() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsHardwareRequirements flavor = createFlavor(null);

        Subnet subnet = buildSubnet();
        InstanceNetworkInterfaceSpecification networkInterface = buildNetworkInterface();
        Mockito.doReturn(networkInterface).when(this.plugin).buildNetworkInterface(Mockito.eq(subnet));

        RunInstancesRequest expected = buildRunInstancesResquest(order, flavor);

        // exercise
        RunInstancesRequest request = this.plugin.buildRequestInstance(order, flavor, subnet);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildNetworkInterface(Mockito.eq(subnet));

        Assert.assertEquals(expected, request);
    }
    
    // test case: When calling the buildNetworkInterfaces method, it must verify
    // that the obtained network interface is the equals as expected.
    @Test
    public void testBuildNetworkInterfaces() throws Exception {
        // set up
        Subnet subnet = buildSubnet();

        String groupId = FAKE_GROUP_ID;
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.GET_GROUP_ID_FROM_METHOD,
                Mockito.anyCollectionOf(Tag.class));

        InstanceNetworkInterfaceSpecification expected = buildNetworkInterface();

        // exercise
        InstanceNetworkInterfaceSpecification networkInterface = this.plugin.buildNetworkInterface(subnet);

        // verify
        Assert.assertEquals(expected, networkInterface);
    }
    
    // test case: When calling the getNetworkSelected method, it must verify
    // that is call was successful.
    @Test
    public void testGetNetworkSelectedSuccessfully() throws Exception {
        // set up
        List networkIds = createNetworkIds();
        ComputeOrder order = Mockito.mock(ComputeOrder.class);
        Mockito.when(order.getNetworkIds()).thenReturn(networkIds);

        String subnetId = FAKE_SUBNET_ID;
        Mockito.doReturn(subnetId).when(this.plugin).selectNetworkId(Mockito.eq(networkIds));

        Subnet subnet = buildSubnet();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(subnet).when(AwsV2CloudUtil.class, TestUtils.GET_SUBNET_BY_ID_METHOD,
                Mockito.eq(subnetId), Mockito.eq(this.client));

        // exercise
        this.plugin.getNetworkSelected(order, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).selectNetworkId(Mockito.eq(networkIds));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getSubnetById(Mockito.eq(subnetId), Mockito.eq(this.client));
    }
    
    // test case: When calling the selectNetworkId method, with a non-empty
    // list, it must verify that the list item has been returned.
    @Test
    public void testSelectNetworkIdFromList() throws FogbowException {
        // set up
        List networkIds = createNetworkIds();
        String expected = FAKE_SUBNET_ID;
        
        // exercise
        String networkId = this.plugin.selectNetworkId(networkIds);

        // verify
        Assert.assertEquals(expected, networkId);
    }

    // test case: When calling the selectNetworkId method, with an empty list,
    // it must verify that the default subnet ID has been returned.
    @Test
    public void testSelectNetworkIdWithEmptyList() throws FogbowException {
        // set up
        List networkIds = new ArrayList();
        String expected = DEFAULT_SUBNET_ID;

        // exercise
        String defaultSubnetId = this.plugin.selectNetworkId(networkIds);

        // verify
        Assert.assertEquals(expected, defaultSubnetId);
    }

    // test case: When calling the checkNetworkIdListIntegrity method, with a
    // list of network IDs containing more than one element, it should check
    // that an InvalidParameterException was thrown.
    @Test
    public void testCheckNetworkIdListIntegrity() throws FogbowException {
        // set up
        String[] networkIds = { FAKE_SUBNET_ID, DEFAULT_SUBNET_ID };
        List<String> networkIdList = Arrays.asList(networkIds);

        String expected = Messages.Exception.MANY_NETWORKS_NOT_ALLOWED;

        try {
            // exercise
            this.plugin.checkNetworkIdListIntegrity(networkIdList);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the findSmallestFlavor method, it must verify
    // that is call was successful.
    @Test
    public void testFindSmallestFlavor() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        Mockito.doNothing().when(this.plugin).updateHardwareRequirements(cloudUser);

        Map requirements = new HashMap();
        requirements.put(VCPU_TEST_KEY, String.valueOf(TestUtils.CPU_VALUE));
        AwsHardwareRequirements greatestFlavor = createFlavor(requirements);
        AwsHardwareRequirements smallestFlavor = createFlavor(null);
        
        TreeSet flavors = createFlavorsCollection(smallestFlavor, greatestFlavor);
        Mockito.doReturn(flavors).when(this.plugin).getFlavorsByRequirements(Mockito.any());

        // exercise
        this.plugin.findSmallestFlavor(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .updateHardwareRequirements(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFlavorsByRequirements(Mockito.any());
    }
    
    // test case: When calling the findSmallestFlavor method, with a null response,
    // it must verify if an UnacceptableOperationException has been thrown.
    @Test
    public void testFindSmallestFlavorFail() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        Mockito.doNothing().when(this.plugin).updateHardwareRequirements(cloudUser);
        
        AwsHardwareRequirements flavor = createFlavor(null);
        TreeSet flavors = createFlavorsCollection(flavor);
        Mockito.doReturn(flavors).when(this.plugin).getFlavorsByRequirements(Mockito.any());

        String expected = Messages.Exception.NO_MATCHING_FLAVOR;

        try {
            // exercise
            this.plugin.findSmallestFlavor(order, cloudUser);
            Assert.fail();
        } catch (UnacceptableOperationException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getFlavorsByRequirements method, with a specific
    // requirements map, it must filter the possibilities that match the resources
    // in the flavor file, returning the corresponding results.
    @Test
    public void testGetFlavorsByRequirements() {
        // set up
        Map<String, String> requirements = new HashMap<String, String>();
        requirements.put(AwsComputePlugin.STORAGE_REQUIREMENT, STORAGE_REQUIREMENT_VALUE);

        AwsHardwareRequirements flavor = createFlavor(null);
        AwsHardwareRequirements flavorWithRequirements = createFlavor(requirements);
        TreeSet<AwsHardwareRequirements> flavors = createFlavorsCollection(flavor, flavorWithRequirements);

        Mockito.doReturn(flavors).when(this.plugin).getFlavors();
        Mockito.doReturn(Arrays.asList(flavor)).when(this.plugin).filterFlavors(Mockito.any(), Mockito.any());

        // exercise
        this.plugin.getFlavorsByRequirements(requirements);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFlavors();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).filterFlavors(Mockito.any(), Mockito.any());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).parseToTreeSet(Mockito.any());
    }
    
    // test case: When calling the getFlavorsByRequirements method, with a null map,
    // there will be no filter to limit the results, returning all the flavors
    // obtained in the last update.
    @Test
    public void testGetFlavorsByRequirementsWithNullMap() {
        // set up
        Map<String, String> requirements = null;
        
        TreeSet<AwsHardwareRequirements> flavors = createFlavorsCollection();
        Mockito.doReturn(flavors).when(this.plugin).getFlavors();
        
        int expected = flavors.size();
        
        // exercise
        TreeSet<AwsHardwareRequirements> resultSet = this.plugin.getFlavorsByRequirements(requirements);
        
        // verify
        Assert.assertEquals(expected, resultSet.size());
    }
    
    // test case: When calling the getFlavorsByRequirements method, with an empty
    // map, there will be no filter to limit the results, returning all the flavors
    // obtained in the last update.
    @Test
    public void testGetFlavorsByRequirementsWithEmptyMap() {
        // set up
        Map<String, String> requirements = new HashMap<String, String>();
        
        TreeSet<AwsHardwareRequirements> flavors = createFlavorsCollection();
        Mockito.doReturn(flavors).when(this.plugin).getFlavors();
        
        int expected = flavors.size();
        
        // exercise
        TreeSet<AwsHardwareRequirements> resultSet = this.plugin.getFlavorsByRequirements(requirements);
        
        // verify
        Assert.assertEquals(expected, resultSet.size());
    }
    
    // test case: When calling the filterFlavors method, it must return the flavor
    // that matches the requirements passed by the parameter.
    @Test
    public void testFilterFlavors() {
        // set up
        Map<String, String> requirements = new HashMap<String, String>();
        requirements.put(AwsComputePlugin.PROCESSOR_REQUIREMENT, PROCESSOR_REQUIREMENT_VALUE);
        requirements.put(AwsComputePlugin.GRAPHIC_PROCESSOR_REQUIREMENT, String.valueOf(TestUtils.CPU_VALUE));

        AwsHardwareRequirements flavor = createFlavor(new HashMap<String, String>());
        AwsHardwareRequirements flavorWithRequirements = createFlavor(requirements);
        TreeSet<AwsHardwareRequirements> flavors = createFlavorsCollection(flavor, flavorWithRequirements);

        AwsHardwareRequirements[] expected = { flavorWithRequirements };

        // exercise
        List<AwsHardwareRequirements> resultList = null;
        for (Entry<String, String> requirement : requirements.entrySet()) {
            resultList = this.plugin.filterFlavors(flavors, requirement);
        }
        // verify
        Assert.assertEquals(Arrays.asList(expected), resultList);
    }
    
    // test case: When calling the updateHardwareRequirements method, it must verify
    // that is call was successful.
    @Test
    public void testUpdateHardwareRequirements() throws FogbowException {
        // set up
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        String[] lines = { generateFlavorsResourceLine() };
        Mockito.doReturn(Arrays.asList(lines)).when(this.plugin).loadLinesFromFlavorFile();

        Map<String, Integer> images = new HashMap<String, Integer>();
        images.put(TestUtils.FAKE_IMAGE_ID, TestUtils.DISK_VALUE);
        Mockito.doReturn(images).when(this.plugin).generateImagesSizeMap(cloudUser);

        AwsHardwareRequirements flavor = createFlavor(null);
        Mockito.doReturn(flavor).when(this.plugin).buildHardwareRequirements(Mockito.any(), Mockito.any());

        // exercise
        this.plugin.updateHardwareRequirements(cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadLinesFromFlavorFile();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).generateImagesSizeMap(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildHardwareRequirements(Mockito.any(),
                Mockito.any());
    }
    
    // test case: When calling the loadLinesFromFlavorFile method, without a valid
    // file path, the ConfigurationErrorException will be thrown.
    @Test
    public void testLoadLinesFromFlavorFile() {
        // set up
        Mockito.doReturn(ANY_VALUE).when(this.plugin).getFlavorsFilePath();

        NoSuchFileException exception = new NoSuchFileException(ANY_VALUE);
        String expected = String.format(Messages.Error.ERROR_MESSAGE, exception);

        try {
            // exercise
            this.plugin.loadLinesFromFlavorFile();
            Assert.fail();
        } catch (ConfigurationErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the buildHardwareRequirements method, it must verify
    // that is call was successful.
    @Test
    public void testBuildHardwareRequirements() {
        // set up
        Map<String, Integer> images = new HashMap<String, Integer>();
        images.put(TestUtils.FAKE_IMAGE_ID, TestUtils.DISK_VALUE);

        String[] requirements = generateRequirements();

        Mockito.doReturn(TestUtils.FAKE_FLAVOR_ID).when(this.plugin).generateFlavorId();

        Map<String, String> requirementMap = new HashMap<String, String>();
        requirementMap.put(AwsComputePlugin.BANDWIDTH_REQUIREMENT, BANDWIDTH_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.GRAPHIC_EMULATION_REQUIREMENT, NO_SUPPORT_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.GRAPHIC_MEMORY_REQUIREMENT, NO_SUPPORT_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.GRAPHIC_PROCESSOR_REQUIREMENT, NO_SUPPORT_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.GRAPHIC_SHARING_REQUIREMENT, NO_SUPPORT_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.PERFORMANCE_REQUIREMENT, NETWORK_PERFORMANCE_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.PROCESSOR_REQUIREMENT, PROCESSOR_REQUIREMENT_VALUE);
        requirementMap.put(AwsComputePlugin.STORAGE_REQUIREMENT, STORAGE_REQUIREMENT_VALUE);
        AwsHardwareRequirements expected = createFlavor(requirementMap);

        AwsHardwareRequirements flavor = null;
        for (Entry<String, Integer> imageEntry : images.entrySet()) {
            // exercise
            flavor = this.plugin.buildHardwareRequirements(imageEntry, requirements);
        }
        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).generateFlavorId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadRequirementsMap(Mockito.eq(requirements));

        Assert.assertEquals(expected, flavor);
    }
    
    // test case: When calling the generateImagesSizeMap method, it must verify
    // that is call was successful and returned the a map of images size.
    @Test
    public void testGenerateImagesSizeMap() throws Exception {
        // set up
        AwsV2User cloudUser = new AwsV2User(TestUtils.FAKE_USER_ID, TestUtils.FAKE_USER_NAME, ANY_VALUE);

        DescribeImagesRequest request = DescribeImagesRequest.builder().owners(cloudUser.getId()).build();

        DescribeImagesResponse response = buildDescribeImages();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(response).when(AwsV2CloudUtil.class, TestUtils.DO_DESCRIBE_IMAGES_REQUEST_METHOD,
                Mockito.eq(request), Mockito.eq(this.client));

        Map expected = new HashMap();
        expected.put(buildImage().imageId(), buildEbsBlockDevice().volumeSize());

        // exercise
        Map imagesSizeMap = this.plugin.generateImagesSizeMap(cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeImagesRequest(Mockito.eq(request), Mockito.eq(this.client));

        Assert.assertEquals(expected, imagesSizeMap);
    }
    
    private String generateFlavorsResourceLine() {
        String[] requeriments = generateRequirements();
        return String.format(FLAVOR_LINE_FORMAT, requeriments);
    }
    
    private String[] generateRequirements() {
        String[] requirements = { 
                InstanceType.T2_MICRO.toString(), 
                String.valueOf(FLAVOR_CPU_VALUE), 
                String.valueOf(FLAVOR_MEMORY_VALUE), 
                STORAGE_REQUIREMENT_VALUE, 
                PROCESSOR_REQUIREMENT_VALUE,
                NETWORK_PERFORMANCE_REQUIREMENT_VALUE,
                BANDWIDTH_REQUIREMENT_VALUE,
                NO_SUPPORT_REQUIREMENT_VALUE,
                NO_SUPPORT_REQUIREMENT_VALUE,
                NO_SUPPORT_REQUIREMENT_VALUE,
                NO_SUPPORT_REQUIREMENT_VALUE,
                String.valueOf(INSTANCE_TYPE_LIMIT_VALUE)
        };
        return requirements;
    }
	
    private DescribeImagesResponse buildDescribeImages() {
        DescribeImagesResponse response = DescribeImagesResponse.builder()
                .images(buildImage())
                .build();

        return response;
    }
	
    private Image buildImage() {
        EbsBlockDevice ebs = buildEbsBlockDevice();
        BlockDeviceMapping blockDeviceMapping = buildBlockDeviceMapping(ebs);

        Image image = Image.builder()
                .imageId(TestUtils.FAKE_IMAGE_ID)
                .blockDeviceMappings(blockDeviceMapping)
                .build();

        return image;
    }

    private BlockDeviceMapping buildBlockDeviceMapping(EbsBlockDevice ebs) {
        BlockDeviceMapping blockDeviceMapping = BlockDeviceMapping.builder()
                .ebs(ebs)
                .build();
        
        return blockDeviceMapping;
    }

    private EbsBlockDevice buildEbsBlockDevice() {
        EbsBlockDevice ebsBlockDevice = EbsBlockDevice.builder()
                .volumeSize(AwsComputePlugin.ONE_GIGABYTE)
                .build();
        
        return ebsBlockDevice;
    }
	
	private Instance mockRunningInstance() {
        Instance instance = buildInstance();

        RunInstancesResponse response = RunInstancesResponse.builder()
                .instances(instance)
                .build();

        Mockito.when(this.client.runInstances(Mockito.any(RunInstancesRequest.class))).thenReturn(response);
        
        return instance;
    }
	
    private TreeSet<AwsHardwareRequirements> createFlavorsCollection(AwsHardwareRequirements...flavor) {
        TreeSet<AwsHardwareRequirements> flavors = new TreeSet<AwsHardwareRequirements>();
        for (int i = 0; i < flavor.length; i++) {
            String flavorId = flavor[i].getFlavorId();
            flavor[i].setFlavorId(flavorId + String.valueOf(i));
            flavors.add(flavor[i]);
        }
        return flavors;
    }
	
    private List<String> buildIpAdressesCollection() {
        String[] ipAdresses = { FAKE_IP_ADDRESS };
        return Arrays.asList(ipAdresses);
    }
	
    private List<Volume> createVolumesCollection() {
        Volume[] volumes = { 
                Volume.builder()
                    .volumeId(TestUtils.FAKE_VOLUME_ID)
                    .size(AwsComputePlugin.ONE_GIGABYTE)
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
                .coreCount(FLAVOR_CPU_VALUE)
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
                .maxCount(AwsComputePlugin.INSTANCES_LAUNCH_NUMBER)
                .minCount(AwsComputePlugin.INSTANCES_LAUNCH_NUMBER)
                .networkInterfaces(createNetworkInterfaceCollection())
                .userData(this.launchCommandGenerator.createLaunchCommand(order))
                .build();
        
        return request;
    }

    private List<InstanceNetworkInterfaceSpecification> createNetworkInterfaceCollection() {
        InstanceNetworkInterfaceSpecification[] networkInterfaces = { 
                buildNetworkInterface() 
                };
        return Arrays.asList(networkInterfaces);
    }

    private InstanceNetworkInterfaceSpecification buildNetworkInterface() {
        InstanceNetworkInterfaceSpecification networkInterface = InstanceNetworkInterfaceSpecification.builder()
                .subnetId(FAKE_SUBNET_ID)
                .deviceIndex(ZERO_VALUE)
                .groups(FAKE_GROUP_ID)
                .build();
        
        return networkInterface;
    }
    
    private Subnet buildSubnet() {
        Tag[] tags = {
                Tag.builder()
                .key(AwsV2CloudUtil.AWS_TAG_GROUP_ID)
                .value(FAKE_GROUP_ID)
                .build()
        };
        Subnet subnet = Subnet.builder()
                .subnetId(FAKE_SUBNET_ID)
                .tags(tags)
                .build();

        return subnet;
    }

    private AwsHardwareRequirements createFlavor(Map<String, String> requirements) {
        int cpu = (requirements != null && requirements.containsKey(VCPU_TEST_KEY))
              ? Integer.parseInt(requirements.get(VCPU_TEST_KEY))
              : FLAVOR_CPU_VALUE;
        int memory = TestUtils.MEMORY_VALUE;
        int disk = TestUtils.DISK_VALUE;
        String name = InstanceType.T2_MICRO.toString();
        String flavorId = TestUtils.FAKE_FLAVOR_ID;
        String imageId = TestUtils.FAKE_IMAGE_ID;
        return new AwsHardwareRequirements(name, flavorId, cpu, memory, disk, imageId, requirements);
    }

    private List<String> createNetworkIds() {
        String[] networkOrderIds = { FAKE_SUBNET_ID };
        return Arrays.asList(networkOrderIds);
    }
    
}
