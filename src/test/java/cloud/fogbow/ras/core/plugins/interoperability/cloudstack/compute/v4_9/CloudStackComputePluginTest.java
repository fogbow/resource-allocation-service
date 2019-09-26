package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.util.*;

@PrepareForTest({SharedOrderHolders.class, CloudStackUrlUtil.class, GetVolumeResponse.class,
        DefaultLaunchCommandGenerator.class, PropertiesUtil.class, GetVirtualMachineResponse.class,
        DatabaseManager.class, DeployVirtualMachineResponse.class})
public class CloudStackComputePluginTest extends BaseUnitTests {

    private static final CloudStackUser CLOUD_STACK_USER =
            new CloudStackUser("", "", "", "", new HashMap<>());
    private static final String BAD_REQUEST_MSG = "Bad Request";
    private final int AMOUNT_EXTRA_SERVICE_OFFERING = 4;
    private static final String FAKE_NETWORK_ID = "fake-network-id";
    private static final String CLOUDSTACK_CLOUD_NAME = "cloudstack";

    private CloudStackComputePlugin plugin;
    private CloudStackHttpClient client;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private Properties properties;
    private String defaultNetworkId;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws UnexpectedException {
        String cloudStackConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + CLOUDSTACK_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.defaultNetworkId = this.properties.getProperty(
                CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackComputePlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);
        this.plugin.setLaunchCommandGenerator(this.launchCommandGeneratorMock);
        this.testUtils.mockReadOrdersFromDataBase();
    }

    // Test case: Trying to get all ServiceOfferings in the Cloudstack, but it occurs an error
    @Test
    public void testGetServiceOfferingsErrorInCloudstack() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        HttpResponseException badRequestHttpResponse = createBadRequestHttpResponse();
        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(badRequestHttpResponse);

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.getServiceOfferings(cloudStackUser);
    }

    // Test case: Getting all ServiceOfferings in the Cloudstack successfully
    @Test
    public void testGetServiceOfferings() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        GetAllServiceOfferingsRequest getAllServiceOfferingRequest = new GetAllServiceOfferingsRequest
                .Builder().build(this.plugin.getCloudStackUrl());
        String getAllServiceOfferingRequestUrl = getAllServiceOfferingRequest.getUriBuilder().toString();

        String idExpected = "id";
        String nameExpected = "name";
        String tagsExpected = "tags";
        int cpuNumberExpected = 10;
        int memoryExpected = 10;
        String getAllServiceOfferingRequestJsonStr = getListServiceOfferrings(
                idExpected, nameExpected, cpuNumberExpected, memoryExpected, tagsExpected);

        Mockito.when(this.client.doGetRequest(
                Mockito.eq(getAllServiceOfferingRequestUrl), Mockito.eq(cloudStackUser)))
                .thenReturn(getAllServiceOfferingRequestJsonStr);

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).
                thenCallRealMethod();

        // exercise
        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                this.plugin.getServiceOfferings(cloudStackUser);

        // verify
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings =
                getAllServiceOfferingsResponse.getServiceOfferings();
        GetAllServiceOfferingsResponse.ServiceOffering firstServiceOffering = serviceOfferings.get(0);

        Assert.assertNotNull(serviceOfferings);
        Assert.assertEquals(idExpected, firstServiceOffering.getId());
        Assert.assertEquals(nameExpected, firstServiceOffering.getName());
        Assert.assertEquals(cpuNumberExpected, firstServiceOffering.getCpuNumber());
        Assert.assertEquals(memoryExpected, firstServiceOffering.getMemory());
        Assert.assertEquals(tagsExpected, firstServiceOffering.getTags());
    }

    // Test case: Trying to get all DiskOfferings in the Cloudstack, but it occurs an error
    @Test
    public void testGetDiskOfferingsErrorInCloudstack() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        HttpResponseException badRequestHttpResponse = createBadRequestHttpResponse();
        Mockito.doThrow(badRequestHttpResponse).when(this.plugin).doGet(
                Mockito.anyString(), Mockito.any(CloudStackUser.class));

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.getDiskOfferings(cloudStackUser);
    }

    // Test case: Getting all DiskOfferings in the Cloudstack successfully
    @Test
    public void testGetDiskOfferings() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        GetAllDiskOfferingsRequest getAllDiskOfferingRequest = new GetAllDiskOfferingsRequest
                .Builder().build(this.plugin.getCloudStackUrl());
        String getAllDiskOfferingRequestUrl = getAllDiskOfferingRequest.getUriBuilder().toString();

        String idExpected = "id";
        int diskExpected = 3;
        boolean customizedExpected = true;
        String getAllDiskOfferingRequestJsonStr = getListDiskOfferrings(
                idExpected, diskExpected, customizedExpected);

        Mockito.doReturn(getAllDiskOfferingRequestJsonStr).when(this.plugin).doGet(
                Mockito.eq(getAllDiskOfferingRequestUrl), Mockito.eq(cloudStackUser));

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise
        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse =
                this.plugin.getDiskOfferings(cloudStackUser);

        // verify
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings =
                getAllDiskOfferingsResponse.getDiskOfferings();
        GetAllDiskOfferingsResponse.DiskOffering firstDiskOffering = diskOfferings.get(0);

        Assert.assertNotNull(diskOfferings);
        Assert.assertEquals(idExpected, firstDiskOffering.getId());
        Assert.assertEquals(diskExpected, firstDiskOffering.getDiskSize());
        Assert.assertEquals(customizedExpected, firstDiskOffering.isCustomized());
    }

    // test case: normalizing networks id successfuly
    @Test
    public void testNormalizeNetworksID() {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        String networksIDExpected = String.format("%s,%s", this.defaultNetworkId, FAKE_NETWORK_ID);

        // exercise
        String networksID = this.plugin.normalizeNetworksID(computeOrder);

        // verify
        Assert.assertEquals(networksIDExpected, networksID);
    }

    // test case: normalizing networks id only with default network
    @Test
    public void testNormalizeNetworksIDOnlyDefaultNetworkId() {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getNetworkOrderIds()).thenReturn(new ArrayList<>());
        String networksIDExpected = this.defaultNetworkId;

        // exercise
        String networksID = this.plugin.normalizeNetworksID(computeOrder);

        // verify
        Assert.assertEquals(networksIDExpected, networksID);
    }

    // test case: get service offering successfully without using requirements
    @Test
    public void testGetServiceOfferingWithoutRequirements() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        String idServiceOfferingExpected = "idServiceOfferingExpected";
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected =
                createServicesOfferingObjects(this.testUtils.MEMORY_VALUE, this.testUtils.CPU_VALUE);
        servicesOfferingExpected.add(new GetAllServiceOfferingsResponse().new ServiceOffering(
                idServiceOfferingExpected, this.testUtils.MEMORY_VALUE, this.testUtils.CPU_VALUE, ""));
        Mockito.when(getAllServiceOfferingsResponse.getServiceOfferings())
                .thenReturn(servicesOfferingExpected);
        Mockito.doReturn(getAllServiceOfferingsResponse)
                .when(this.plugin).getServiceOfferings(Mockito.eq(cloudStackUser));

        // exercise
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                this.plugin.getServiceOffering(computeOrder, cloudStackUser);

        // verify
        Assert.assertEquals(servicesOfferingExpected.get(0), serviceOffering);
    }

    // test case: get service offering and return en exception because there are not services offerings in the cloud
    @Test
    public void testGetServiceOfferingAndEmptyServicesOffering() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        ComputeOrder computeOrder = createComputeOrder(
                new ArrayList<UserData>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected = new ArrayList<>();
        Mockito.when(getAllServiceOfferingsResponse.getServiceOfferings())
                .thenReturn(servicesOfferingExpected);
        Mockito.doReturn(getAllServiceOfferingsResponse)
                .when(this.plugin).getServiceOfferings(Mockito.eq(cloudStackUser));

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);

        // exercise
        this.plugin.getServiceOffering(computeOrder, cloudStackUser);

    }

    // test case: get service offering and return an exception because the resources don't match
    @Test
    public void testGetServiceOfferingNoMatchServicesOffering() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        ComputeOrder computeOrder = createComputeOrder(
                new ArrayList<UserData>(), "fake-image-id");

        final int EXCEEDED_VALUE = 1;
        int overMemory = this.testUtils.CPU_VALUE + EXCEEDED_VALUE;
        int overCpu = this.testUtils.MEMORY_VALUE + EXCEEDED_VALUE;
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected =
                createServicesOfferingObjects(overMemory, overCpu);
        servicesOfferingExpected.add(new GetAllServiceOfferingsResponse().new ServiceOffering(
                "anyId", overCpu, overMemory, "anyTag"));

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);
        Mockito.when(getAllServiceOfferingsResponse.getServiceOfferings())
                .thenReturn(servicesOfferingExpected);
        Mockito.doReturn(getAllServiceOfferingsResponse)
                .when(this.plugin).getServiceOfferings(Mockito.eq(cloudStackUser));

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);

        // exercise
        this.plugin.getServiceOffering(computeOrder, cloudStackUser);
    }


    // test case: get service offering and return an exception because services offering comes null
    @Test
    public void testGetServiceOfferingAndExceptionServicesOffering() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        ComputeOrder computeOrder = createComputeOrder(
                new ArrayList<UserData>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected = null;
        Mockito.when(getAllServiceOfferingsResponse.getServiceOfferings())
                .thenReturn(servicesOfferingExpected);
        Mockito.doReturn(getAllServiceOfferingsResponse)
                .when(this.plugin).getServiceOfferings(Mockito.eq(cloudStackUser));

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);


        // exercise
        this.plugin.getServiceOffering(computeOrder, cloudStackUser);
    }

    // test case: instance name does not change
    @Test
    public void testNormalizeInstanceName() {
        // set up
        String instanceNameExpected = "instanceName";

        // exercise
        String instanceName = this.plugin.normalizeInstanceName(instanceNameExpected);

        // verify
        Assert.assertEquals(instanceNameExpected, instanceName);
    }

    // test case: generate a instance name because instaceName parameter is null
    @Test
    public void testNormalizeInstanceNameWhenParameterIsNull() {
        // set up
        final int SUFIX_GENERATED_INSTANCE_NAME_SIZE = 36;
        String instanceNameNull = null;

        // exercise
        String instanceName = this.plugin.normalizeInstanceName(instanceNameNull);

        // verify
        String prefix = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX;
        Assert.assertNotNull(instanceName);
        Assert.assertTrue(instanceName.startsWith(prefix));

        String sufixInstanceName = instanceName.split(prefix)[1];
        Assert.assertEquals(SUFIX_GENERATED_INSTANCE_NAME_SIZE, sufixInstanceName.length());
    }

    // Test case: request instance successfully
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        int orderDisk = order.getDisk();
        Mockito.doReturn(diskOffering).when(this.plugin).getDiskOffering(
                Mockito.eq(orderDisk), Mockito.any(CloudStackUser.class));

        String fakeUserDataString = "anystring";
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class)))
                .thenReturn(fakeUserDataString);

        DeployVirtualMachineResponse deployVirtualMachineResponse =
                Mockito.mock(DeployVirtualMachineResponse.class);
        Mockito.doReturn(deployVirtualMachineResponse).when(this.plugin)
                .doRequestInstance(Mockito.any(), Mockito.eq(cloudStackUser));

        Mockito.doNothing().when(this.plugin).updateComputeOrder(
                Mockito.eq(order), Mockito.eq(serviceOffering), Mockito.eq(diskOffering));

        // exercise
        this.plugin.requestInstance(order, cloudStackUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(1))
                .doRequestInstance(Mockito.any(), Mockito.eq(cloudStackUser));
    }

    // Test case: request instance but the service offering is not available
    @Test
    public void testRequestInstanceServiceOfferingThrowException() throws FogbowException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        Mockito.doThrow(new NoAvailableResourcesException()).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.plugin.requestInstance(order, CLOUD_STACK_USER);

        // verify
        Mockito.verify(this.plugin, Mockito.times(0))
                .getDiskOffering(Mockito.any(), Mockito.any());
    }

    // Test case: request instance but it throw an exception because the disk offering
    @Test
    public void testRequestInstanceDiskOfferingThrowException() throws FogbowException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        GetAllDiskOfferingsResponse.DiskOffering diskOffering = null;
        int orderDisk = order.getDisk();
        Mockito.doThrow(new NoAvailableResourcesException()).when(this.plugin).getDiskOffering(
                Mockito.eq(orderDisk), Mockito.any(CloudStackUser.class));
        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.plugin.requestInstance(order, CLOUD_STACK_USER);

        // verify
        Mockito.verify(this.plugin, Mockito.times(0))
                .normalizeInstanceName(Mockito.any());
    }

    // Test case: request instance but it occurs a error in the request to the cloud and throw a exception
    @Test(expected = FogbowException.class)
    public void testRequestInstanceErrorInRequest() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        Mockito.doReturn("networksId").when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order), Mockito.any(CloudStackUser.class));

        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        int orderDisk = order.getDisk();
        Mockito.doReturn(diskOffering).when(this.plugin).getDiskOffering(
                Mockito.eq(orderDisk), Mockito.any(CloudStackUser.class));

        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(
                Mockito.any(ComputeOrder.class))).thenReturn("anystring");

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doRequestInstance(Mockito.any(), Mockito.eq(cloudStackUser));

        // exercise
        this.plugin.requestInstance(order, cloudStackUser);
    }

    // test case: get compute instace successfully
    @Test
    public void testGetComputeInstance() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        String idExpected = "id";
        String nameExpected = "name";
        int vCpuExpected = 1;
        int memoryExpected = 2;
        int diskExpected = 3;
        String ipAddressExpected = "10.10.10.10";
        String networkDefaultExpected = this.defaultNetworkId;
        String cloudStateExpected = "state";
        GetVirtualMachineResponse.VirtualMachine virtualMachine =
                Mockito.mock(GetVirtualMachineResponse.VirtualMachine.class);
        Mockito.when(virtualMachine.getId()).thenReturn(idExpected);
        Mockito.when(virtualMachine.getName()).thenReturn(nameExpected);
        Mockito.when(virtualMachine.getName()).thenReturn(nameExpected);
        Mockito.when(virtualMachine.getCpuNumber()).thenReturn(vCpuExpected);
        Mockito.when(virtualMachine.getMemory()).thenReturn(memoryExpected);
        Mockito.when(virtualMachine.getState()).thenReturn(cloudStateExpected);
        GetVirtualMachineResponse.Nic nic = Mockito.mock(GetVirtualMachineResponse.Nic.class);
        Mockito.when(nic.getIpAddress()).thenReturn(ipAddressExpected);
        GetVirtualMachineResponse.Nic[] nics = new GetVirtualMachineResponse.Nic[] {
            nic
        };
        Mockito.when(virtualMachine.getNic()).thenReturn(nics);

        Mockito.doReturn(diskExpected).when(this.plugin)
                .getVirtualMachineDiskSize(Mockito.any(), Mockito.any());

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // verify
        ComputeInstance computeInstance =
                this.plugin.getComputeInstance(virtualMachine, cloudStackUser);

        // exercise
        Assert.assertEquals(idExpected, computeInstance.getId());
        Assert.assertEquals(nameExpected, computeInstance.getName());
        Assert.assertEquals(vCpuExpected, computeInstance.getvCPU());
        Assert.assertEquals(memoryExpected, computeInstance.getMemory());
        Assert.assertEquals(diskExpected, computeInstance.getDisk());
        Assert.assertEquals(ipAddressExpected, computeInstance.getIpAddresses().get(0));
        Assert.assertEquals(networkDefaultExpected, computeInstance.getNetworks().get(0).getId());
    }

    // test case: getting compute instace and occur a error when is getting the disk size
    @Test
    public void testGetComputeInstanceErrorToGetDisk() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        String idExpected = "id";
        String nameExpected = "name";
        int vCpuExpected = 1;
        int memoryExpected = 2;
        int diskExpected = CloudStackComputePlugin.UNKNOWN_DISK_VALUE;
        String ipAddressExpected = "10.10.10.10";
        String networkDefaultExpected = this.defaultNetworkId;
        String cloudStateExpected = "state";
        GetVirtualMachineResponse.VirtualMachine virtualMachine =
                Mockito.mock(GetVirtualMachineResponse.VirtualMachine.class);
        Mockito.when(virtualMachine.getId()).thenReturn(idExpected);
        Mockito.when(virtualMachine.getName()).thenReturn(nameExpected);
        Mockito.when(virtualMachine.getName()).thenReturn(nameExpected);
        Mockito.when(virtualMachine.getCpuNumber()).thenReturn(vCpuExpected);
        Mockito.when(virtualMachine.getMemory()).thenReturn(memoryExpected);
        Mockito.when(virtualMachine.getState()).thenReturn(cloudStateExpected);
        GetVirtualMachineResponse.Nic nic = Mockito.mock(GetVirtualMachineResponse.Nic.class);
        Mockito.when(nic.getIpAddress()).thenReturn(ipAddressExpected);
        GetVirtualMachineResponse.Nic[] nics = new GetVirtualMachineResponse.Nic[] {
                nic
        };
        Mockito.when(virtualMachine.getNic()).thenReturn(nics);

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .getVirtualMachineDiskSize(Mockito.any(), Mockito.any());

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // verify
        ComputeInstance computeInstance =
                this.plugin.getComputeInstance(virtualMachine, cloudStackUser);

        // exercise
        Assert.assertEquals(idExpected, computeInstance.getId());
        Assert.assertEquals(nameExpected, computeInstance.getName());
        Assert.assertEquals(vCpuExpected, computeInstance.getvCPU());
        Assert.assertEquals(memoryExpected, computeInstance.getMemory());
        Assert.assertEquals(diskExpected, computeInstance.getDisk());
        Assert.assertEquals(ipAddressExpected, computeInstance.getIpAddresses().get(0));
        Assert.assertEquals(networkDefaultExpected, computeInstance.getNetworks().get(0).getId());
    }

    // test case: get virtual machine disk size from cloud successfully
    @Test
    public void testGetVirtualMachineDiskSize() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        String virtualMachineId = "id";

        String idExpected = "";
        String nameExpected = "name";
        int sizeExpected = 10;
        double sizeInBytes = CloudStackComputePlugin.GIGABYTE_IN_BYTES * sizeExpected;
        String stateExpected = "READY";
        String getVolumeResponse = getVolumeResponse(
                idExpected, nameExpected, sizeInBytes, stateExpected);
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(CLOUD_STACK_USER)))
                .thenReturn(getVolumeResponse);

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise
        int virtualMachineDiskSize = this.plugin.getVirtualMachineDiskSize(
                virtualMachineId, cloudStackUser);

        // verify
        Assert.assertEquals(sizeExpected, virtualMachineDiskSize);
    }

    // test case: getting virtual machine disk size and do not found volumes
    @Test(expected = InstanceNotFoundException.class)
    public void testGetVirtualMachineDiskSizeNotFoundVolumes() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        String virtualMachineId = "id";

        String anyResponse = "";
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(CLOUD_STACK_USER)))
                .thenReturn(anyResponse);

        GetVolumeResponse volumeResponse = Mockito.mock(GetVolumeResponse.class);
        Mockito.when(volumeResponse.getVolumes()).thenReturn(null);
        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(Mockito.eq(anyResponse)))
                .thenReturn(volumeResponse);

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise
        this.plugin.getVirtualMachineDiskSize(virtualMachineId, cloudStackUser);
    }

    // test case: get virtual machine disks and occour an exception in the cloud
    @Test
    public void testGetVirtualMachineDiskSizeException() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        String virtualMachineId = "id";

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(CLOUD_STACK_USER)))
                .thenThrow(createBadRequestHttpResponse());

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.getVirtualMachineDiskSize(virtualMachineId, cloudStackUser);
    }

    // test case: getVM successfully
    @Test
    public void testGetVM() throws InstanceNotFoundException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = new ArrayList<>();
        GetVirtualMachineResponse.VirtualMachine virtualMachine =
                Mockito.mock(GetVirtualMachineResponse.VirtualMachine.class);
        virtualMachines.add(virtualMachine);
        Mockito.when(getVirtualMachineResponse.getVirtualMachines()).thenReturn(virtualMachines);

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.plugin).getComputeInstance(
                Mockito.eq(virtualMachine), Mockito.eq(cloudStackUser));

        // exercise
        ComputeInstance computeInstance = this.plugin.getVM(getVirtualMachineResponse, cloudStackUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
    }

    // test case: getVM throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetVMInstanceNotFoundException() throws InstanceNotFoundException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = new ArrayList<>();
        Mockito.when(getVirtualMachineResponse.getVirtualMachines()).thenReturn(virtualMachines);

        // exercise
        this.plugin.getVM(getVirtualMachineResponse, cloudStackUser);
    }

    // test case: doGetInstance successfully
    @Test
    public void testDoGetInstance() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .build("anything");
        URIBuilder uriRequest = getVirtualMachineRequest.getUriBuilder();

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String responseStr = "anyResponseStr";
        Mockito.doReturn(responseStr).when(this.plugin)
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));

        PowerMockito.mockStatic(GetVirtualMachineResponse.class);
        GetVirtualMachineResponse getVirtualMachineResponseExpexted =
                Mockito.mock(GetVirtualMachineResponse.class);
        PowerMockito.when(GetVirtualMachineResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(getVirtualMachineResponseExpexted);

        // exercise
        GetVirtualMachineResponse getVirtualMachineResponse =
                this.plugin.doGetInstance(getVirtualMachineRequest, cloudStackUser);

        // verify
        Assert.assertEquals(getVirtualMachineResponseExpexted, getVirtualMachineResponse);
    }

    // test case: doGetInstance treating a HttpResponseException
    @Test
    public void testDoGetInstanceThrowException() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .build("anything");
        URIBuilder uriRequest = getVirtualMachineRequest.getUriBuilder();

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.doThrow(createBadRequestHttpResponse()).when(this.plugin)
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.doGetInstance(getVirtualMachineRequest, cloudStackUser);
    }

    // test case: get instance successfully
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        Mockito.doReturn(getVirtualMachineResponse).when(this.plugin)
                .doGetInstance(Mockito.any(), Mockito.eq(CLOUD_STACK_USER));

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.plugin)
                .getVM(Mockito.eq(getVirtualMachineResponse), Mockito.eq(cloudStackUser));

        // exercise
        ComputeInstance computeInstance = this.plugin.getInstance(computeOrder, cloudStackUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
    }

    // test case: get instance and occurs a bad request
    @Test
    public void testGetInstanceBadRequest() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doGetInstance(Mockito.any(), Mockito.eq(CLOUD_STACK_USER));

        // exercise
        try {
            this.plugin.getInstance(computeOrder, cloudStackUser);
            Assert.fail();
        } catch (Exception e) {
                // verify
            Mockito.verify(this.plugin, Mockito.times(0))
                    .getVM(Mockito.any(), Mockito.any());
        }
    }

    // test case: testDoDeleteInstance successfully
    @Test
    public void testDoDeleteInstance() throws FogbowException, HttpResponseException {
        //set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        DestroyVirtualMachineRequest destroyVirtualMachineRequest = new DestroyVirtualMachineRequest
                .Builder().build("anything");
        URIBuilder uriRequest = destroyVirtualMachineRequest.getUriBuilder();
        String instanceId = "InstanceId";

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.doReturn(new String()).when(this.plugin).doGet(Mockito.eq(uriRequest.toString()),
                Mockito.eq(cloudStackUser));

        // exercise
        this.plugin.doDeleteInstance(destroyVirtualMachineRequest, cloudStackUser, instanceId);

        Mockito.verify(this.plugin, Mockito.times(1))
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));
    }

    // test case: exercising testDoDeleteInstance and it occurs a HttpResponseException
    @Test
    public void testDoDeleteInstanceHttpResponseException() throws FogbowException, HttpResponseException {
        //set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        DestroyVirtualMachineRequest destroyVirtualMachineRequest = new DestroyVirtualMachineRequest
                .Builder().build("anything");
        URIBuilder uriRequest = destroyVirtualMachineRequest.getUriBuilder();
        String instanceId = "InstanceId";

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.doThrow(createBadRequestHttpResponse()).when(this.plugin)
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDeleteInstance(destroyVirtualMachineRequest, cloudStackUser, instanceId);
    }

    // Test case: deleting an instance successfully
    @Test
    public void testDeleteInstance() throws FogbowException, HttpResponseException {
        // set up
        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.eq(CLOUD_STACK_USER))).thenReturn("");

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(this.testUtils.FAKE_INSTANCE_ID);

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(),
                Mockito.eq(CLOUD_STACK_USER), Mockito.endsWith(this.testUtils.FAKE_INSTANCE_ID));

        // exercise
        this.plugin.deleteInstance(computeOrder, CLOUD_STACK_USER);

        Mockito.verify(this.plugin, Mockito.times(1)).doDeleteInstance(Mockito.any(),
                        Mockito.eq(CLOUD_STACK_USER), Mockito.endsWith(this.testUtils.FAKE_INSTANCE_ID));
    }

    // Test case: failing to delete an instance
    @Test(expected = FogbowException.class)
    public void testDeleteInstanceFail() throws FogbowException, HttpResponseException {
        // Delete response is unused
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(CLOUD_STACK_USER)))
                .thenThrow(createBadRequestHttpResponse());

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(this.testUtils.FAKE_INSTANCE_ID);

        Mockito.doThrow(new FogbowException()).when(this.plugin).doDeleteInstance(Mockito.any(),
                Mockito.eq(CLOUD_STACK_USER), Mockito.endsWith(this.testUtils.FAKE_INSTANCE_ID));

        // exercise
        this.plugin.deleteInstance(computeOrder, CLOUD_STACK_USER);
    }

    // test case: execute the doGet and a Fogbow Exception is throw
    @Test
    public void testDoGetFogbowError() throws FogbowException, HttpResponseException {
        // set up
        String url = "anyUrl";
        String exceptionMessage = "anyMessage";
        Mockito.when(this.client.doGetRequest(
                Mockito.eq(url), Mockito.any(CloudStackUser.class)))
                .thenThrow(new FogbowException(exceptionMessage));

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(exceptionMessage);

        // exercise
        this.plugin.doGet(url, CLOUD_STACK_USER);
    }

    // test case: filter services offering by requirements. Matching with several requirements.
    @Test
    public void testFilterServicesOfferingByRequirements() {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "");
        Map<String, String> requirements = new HashMap<>();
        String keyOne = "one";
        String valueOne = "one";
        String tagOne = keyOne + CloudStackComputePlugin.FOGBOW_TAG_SEPARATOR + valueOne;
        requirements.put(keyOne, valueOne);
        String keyTwo = "two";
        String valueTwo = "two";
        String tagTwo = keyTwo + CloudStackComputePlugin.FOGBOW_TAG_SEPARATOR + valueTwo;
        requirements.put(keyTwo, valueTwo);
        computeOrder.setRequirements(requirements);
        String tagExpected = tagOne + CloudstackTestUtils.CLOUDSTACK_MULTIPLE_TAGS_SEPARATOR + tagTwo;

        int anyMemory = 1;
        int anyCpu = 1;
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOffering =
                createServicesOfferingObjects(anyMemory, anyCpu);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOfferingA =
                new GetAllServiceOfferingsResponse().new ServiceOffering(
                "anyId", anyCpu, anyMemory, tagExpected) ;
        servicesOffering.add(serviceOfferingA);

        // verify before
        Assert.assertEquals(AMOUNT_EXTRA_SERVICE_OFFERING + 1, servicesOffering.size());

        // exercise
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFilted =
                this.plugin.filterServicesOfferingByRequirements(servicesOffering, computeOrder);

        // verify
        Assert.assertEquals(1, serviceOfferingsFilted.size());
        Assert.assertEquals(serviceOfferingA, serviceOfferingsFilted.get(0));
    }

    // test case: Doesn't match at all because 1 tag doesn't match.
    @Test
    public void testFilterServicesOfferingByRequirementsOneTagMissing() {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "");
        Map<String, String> requirements = new HashMap<>();
        String keyOne = "one";
        String valueOne = "one";
        String tagOne = keyOne + CloudStackComputePlugin.FOGBOW_TAG_SEPARATOR + valueOne;
        requirements.put(keyOne, valueOne);
        String keyTwo = "two";
        String valueTwo = "two";
        String tagTwo = keyTwo + CloudStackComputePlugin.FOGBOW_TAG_SEPARATOR + valueTwo;
        requirements.put(keyTwo, valueTwo);
        computeOrder.setRequirements(requirements);
        String tagExpected = tagOne;

        int anyMemory = 1;
        int anyCpu = 1;
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOffering =
                createServicesOfferingObjects(anyMemory, anyCpu);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOfferingA =
                new GetAllServiceOfferingsResponse().new ServiceOffering(
                        "anyId", anyCpu, anyMemory, tagExpected) ;
        servicesOffering.add(serviceOfferingA);

        // verify before
        Assert.assertEquals(AMOUNT_EXTRA_SERVICE_OFFERING + 1, servicesOffering.size());

        // exercise
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFilted =
                this.plugin.filterServicesOfferingByRequirements(servicesOffering, computeOrder);

        // verify
        Assert.assertEquals(0, serviceOfferingsFilted.size());
    }

    // test case: There are no requirements
    @Test
    public void testFilterServicesOfferingWithEmptyRequirements() {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "");

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOffering =
                createServicesOfferingObjects(this.testUtils.MEMORY_VALUE, this.testUtils.CPU_VALUE);

        // exercise
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFilted =
                this.plugin.filterServicesOfferingByRequirements(servicesOffering, computeOrder);

        // verify
        Assert.assertEquals(AMOUNT_EXTRA_SERVICE_OFFERING , serviceOfferingsFilted.size());
    }
    
    // test case: get first disk that it matches with the disk size required
    @Test
    public void testGetDiskOffering() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        int diskExpected = 1;

        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse = Mockito.mock(GetAllDiskOfferingsResponse.class);
        Mockito.doReturn(getAllDiskOfferingsResponse)
                .when(this.plugin).getDiskOfferings(Mockito.eq(cloudStackUser));
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = new ArrayList<>();
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingOne.getDiskSize()).thenReturn(diskExpected - 1);
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingTwo.getDiskSize()).thenReturn(diskExpected);
        diskOfferings.add(diskOfferingOne);
        diskOfferings.add(diskOfferingTwo);
        Mockito.doReturn(diskOfferings).when(getAllDiskOfferingsResponse).getDiskOfferings();

        // exercise
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                this.plugin.getDiskOffering(diskExpected, cloudStackUser);

        // verify
        Assert.assertEquals(diskOfferingTwo.getDiskSize(), diskOffering.getDiskSize());
    }

    // test case: No one matches with the disk required
    @Test
    public void testGetDiskOfferingNotMatch() throws FogbowException {
        // set up
        int diskExpected = 1;
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse = Mockito.mock(GetAllDiskOfferingsResponse.class);
        Mockito.doReturn(getAllDiskOfferingsResponse)
                .when(this.plugin).getDiskOfferings(Mockito.eq(cloudStackUser));
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = Mockito.spy(new ArrayList<>())
                ;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingOne.getDiskSize()).thenReturn(diskExpected - 1);
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingTwo.getDiskSize()).thenReturn(diskExpected - 1);
        diskOfferings.add(diskOfferingOne);
        diskOfferings.add(diskOfferingTwo);
        Mockito.doReturn(diskOfferings).when(getAllDiskOfferingsResponse).getDiskOfferings();

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK);

        // exercise
        this.plugin.getDiskOffering(diskExpected, cloudStackUser);
    }

    // test case: There are no disks
    @Test
    public void testGetDiskOfferingDisksEmpty() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse =
                Mockito.mock(GetAllDiskOfferingsResponse.class);
        Mockito.doReturn(getAllDiskOfferingsResponse).when(this.plugin)
                .getDiskOfferings(Mockito.eq(cloudStackUser));
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = new ArrayList<>();
        Mockito.doReturn(diskOfferings).when(getAllDiskOfferingsResponse).getDiskOfferings();

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK);

        // exercise
        int anyThing = 10;
        this.plugin.getDiskOffering(anyThing, cloudStackUser);
    }

    // test case: successfully case
    @Test
    public void testDoGet() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        String url = "http://localhost";
        String responseStrExpeced = "response";

        Mockito.when(this.client.doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser)))
                .thenReturn(responseStrExpeced);

        // exercise
        String responseStr = this.plugin.doGet(url, cloudStackUser);

        // verify
        Assert.assertEquals(responseStrExpeced, responseStr);
    }

    // test case: catching a FogbowException
    @Test
    public void testDoGetFogbowException() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;
        String url = "http://localhost";

        Mockito.when(this.client.doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser)))
                .thenThrow(createBadRequestHttpResponse());

        this.expectedException.expect(HttpResponseException.class);

        // exercise
        this.plugin.doGet(url, cloudStackUser);
    }

    // test case: successfully case
    @Test
    public void testDoRequestInstance() throws FogbowException, IOException {
        // set up
        DeployVirtualMachineRequest deployVirtualMachineRequest = new DeployVirtualMachineRequest.Builder()
                .build("");
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();

        String responseStr = "";
        Mockito.when(this.client.doGetRequest(
                Mockito.any(), Mockito.eq(cloudStackUser))).thenReturn(responseStr);

        DeployVirtualMachineResponse deployVirtualMachineResponseExpected =
                Mockito.mock(DeployVirtualMachineResponse.class);
        PowerMockito.mockStatic(DeployVirtualMachineResponse.class);
        PowerMockito.when(DeployVirtualMachineResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(deployVirtualMachineResponseExpected);

        // exercise
        DeployVirtualMachineResponse deployVirtualMachineResponse =
                this.plugin.doRequestInstance(deployVirtualMachineRequest, cloudStackUser);

        // verify
        Assert.assertEquals(deployVirtualMachineResponseExpected, deployVirtualMachineResponse);
    }

    // test case: the request throws a HttpResponseException
    @Test
    public void testDoRequestInstanceAndHttpResponseException() throws FogbowException, IOException {
        // set up
        DeployVirtualMachineRequest deployVirtualMachineRequest = new DeployVirtualMachineRequest.Builder()
                .build("anything");
        CloudStackUser cloudStackUser = CLOUD_STACK_USER;

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(
                Mockito.any(), Mockito.eq(cloudStackUser))).thenThrow(createBadRequestHttpResponse());

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.doRequestInstance(deployVirtualMachineRequest, cloudStackUser);
    }

    // test case: successfully case
    @Test
    public void testUpdateComputeOrder() {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "");

        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        int memoryExpected = 2;
        int cpuExpected = 3;
        Mockito.when(serviceOffering.getMemory()).thenReturn(memoryExpected);
        Mockito.when(serviceOffering.getCpuNumber()).thenReturn(cpuExpected);
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        int diskExpected = 10;
        Mockito.when(diskOffering.getDiskSize()).thenReturn(diskExpected);

        // verify before
        ComputeAllocation actualAllocationBefore = computeOrder.getActualAllocation();
        int DEFAULT_VALUE = 0;
        Assert.assertEquals(DEFAULT_VALUE, actualAllocationBefore.getDisk());
        Assert.assertEquals(DEFAULT_VALUE, actualAllocationBefore.getInstances());
        Assert.assertEquals(DEFAULT_VALUE, actualAllocationBefore.getRam());
        Assert.assertEquals(DEFAULT_VALUE, actualAllocationBefore.getvCPU());

        // exercise
        this.plugin.updateComputeOrder(computeOrder, serviceOffering, diskOffering);

        // verify after
        ComputeAllocation actualAllocationAfter = computeOrder.getActualAllocation();
        Assert.assertEquals(diskExpected, actualAllocationAfter.getDisk());
        Assert.assertEquals(CloudStackComputePlugin.AMOUNT_INSTANCE, actualAllocationAfter.getInstances());
        Assert.assertEquals(memoryExpected, actualAllocationAfter.getRam());
        Assert.assertEquals(cpuExpected, actualAllocationAfter.getvCPU());
    }

    // test case: getTemplateId successfully case
    @Test
    public void testGetTemplateId() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();

        // exercise
        String templateId = this.plugin.getTemplateId(computeOrder);

        // verify
        Assert.assertEquals(this.testUtils.FAKE_IMAGE_ID, templateId);
    }

    // test case: getTemplateId throws an exception because there is not image id int the order
    @Test(expected = InvalidParameterException.class)
    public void testGetTemplateIdThrowException() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn(null);

        // exercise
        this.plugin.getTemplateId(computeOrder);
    }

    // test case: getTemplateId throws an exception because image id is empty in the order
    @Test(expected = InvalidParameterException.class)
    public void testGetTemplateIdThrowExceptionEmptyImageId() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn("");

        // exercise
        this.plugin.getTemplateId(computeOrder);
    }

    private String getVolumeResponse(String id, String name, double size, String state) throws IOException {
        return CloudstackTestUtils.createGetVolumesResponseJson(id, name, size, state);
    }

    private String getListDiskOfferrings(String id, int diskSize, boolean customized) throws IOException {
        return CloudstackTestUtils.createGetAllDiskOfferingsResponseJson(
                id, diskSize, customized, "");
    }

    private String getListServiceOfferrings(
            String id, String name, int cpuNumber, int memory, String tags) throws IOException {

        return CloudstackTestUtils.createGetAllServiceOfferingsResponseJson(
                id, name, cpuNumber, memory, tags);
    }

    private ComputeOrder createComputeOrder(ArrayList<UserData> fakeUserData, String fakeImageId) {
        SystemUser requester = this.testUtils.createSystemUser();

        NetworkOrder networkOrder = this.testUtils.createLocalNetworkOrder();
        networkOrder.setId(FAKE_NETWORK_ID);
        List<String> networkOrderIds = Mockito.spy(new ArrayList<>());
        networkOrderIds.add(networkOrder.getId());

        ComputeOrder computeOrder = new ComputeOrder(requester, this.testUtils.FAKE_REMOTE_MEMBER_ID,
                this.testUtils.FAKE_REMOTE_MEMBER_ID, this.testUtils.DEFAULT_CLOUD_NAME,
                "", this.testUtils.CPU_VALUE, this.testUtils.MEMORY_VALUE,
                this.testUtils.DISK_VALUE, fakeImageId, fakeUserData, "", networkOrderIds);
        computeOrder.setInstanceId(this.testUtils.FAKE_INSTANCE_ID);

        ComputeOrder computeOrderMock = Mockito.spy(computeOrder);
        Mockito.when(networkOrderIds.isEmpty()).thenReturn(false);
        Mockito.doReturn(networkOrderIds).when(computeOrderMock).getNetworkIds();
        return computeOrderMock;
    }

    private List<GetAllServiceOfferingsResponse.ServiceOffering> createServicesOfferingObjects(
            int memory, int cpu) {

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOffering = new ArrayList<>();

        for (int i = 0; i < AMOUNT_EXTRA_SERVICE_OFFERING; i++) {
            String randonId = UUID.randomUUID().toString();
            servicesOffering.add(new GetAllServiceOfferingsResponse().new ServiceOffering(
                    randonId, cpu, memory, "anyTag"));
        }

        Assert.assertEquals(AMOUNT_EXTRA_SERVICE_OFFERING, servicesOffering.size());

        return servicesOffering;
    }

    private HttpResponseException createBadRequestHttpResponse() {
        return new HttpResponseException(HttpStatus.SC_BAD_REQUEST, BAD_REQUEST_MSG);
    }
}