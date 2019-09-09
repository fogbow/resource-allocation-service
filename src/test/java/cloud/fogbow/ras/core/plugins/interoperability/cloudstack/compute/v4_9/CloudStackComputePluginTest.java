package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, CloudStackUrlUtil.class,
        DefaultLaunchCommandGenerator.class, PropertiesUtil.class, GetVirtualMachineResponse.class})
public class CloudStackComputePluginTest {

    private static final String BAD_REQUEST_MSG = "BAD Request";

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_INSTANCE_NAME = "fake-name";
    public static final String FAKE_STATE = "ready";
    public static final String FAKE_CPU_NUMBER = "4";
    public static final String FAKE_MEMORY = "2024";
    private static final HashMap<String, String> FAKE_COOKIE_HEADER = new HashMap<>();
    public static final String FAKE_DISK = "25";
    public static final String FAKE_TAGS = "tag1:value1,tag2:value2";
    public static final String FAKE_ADDRESS = "10.0.0.0/24";
    public static final String FAKE_NETWORK_ID = "fake-network-id";
    public static final String FAKE_TYPE = "ROOT";
    public static final String FAKE_EXPUNGE = "true";
    public static final String FAKE_MEMBER = "fake-member";
    public static final String FAKE_CLOUD_NAME = "fake-cloud-name";
    public static final String FAKE_PUBLIC_KEY = "fake-public-key";

    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-name";
    private static final String FAKE_ID_PROVIDER = "fake-id-provider";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";

    public static final CloudStackUser FAKE_TOKEN =  new CloudStackUser(FAKE_USER_ID, FAKE_USERNAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);

    public static final String JSON = "json";
    public static final String RESPONSE_KEY = "response";
    public static final String ID_KEY = "id";
    public static final String VIRTUAL_MACHINE_ID_KEY = "virtualmachineid";
    public static final String TYPE_KEY = "type";
    public static final String EXPUNGE_KEY = "expunge";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String CLOUD_NAME = "cloudstack";


    private CloudStackComputePlugin plugin;
    private CloudStackHttpClient client;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private SharedOrderHolders sharedOrderHolders;
    private Properties properties;
    private String defaultNetworkId;
    private String fakeZoneId;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.defaultNetworkId = this.properties.getProperty(CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackComputePlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);
        this.plugin.setLaunchCommandGenerator(this.launchCommandGeneratorMock);
        this.fakeZoneId = this.properties.getProperty(CloudStackComputePlugin.ZONE_ID_KEY);

        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
    }

    // Test case: Trying to get all ServiceOfferings in the Cloudstack, but it occurs an error
    @Test
    public void testGetServiceOfferingsErrorInCloudstack() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;

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

    // Test case: The cloudStackUser parameter is null and this throw a exception
    @Test
    public void testGetServiceOfferingsCloudStackUserNull() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = null;

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudStackComputePlugin.
                CLOUDUSER_NULL_EXCEPTION_MSG);

        // exercise
        this.plugin.getServiceOfferings(cloudStackUser);
    }

    // Test case: Getting all ServiceOfferings in the Cloudstack successfully
    @Test
    public void testGetServiceOfferings() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;
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
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise
        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse = this.plugin.getServiceOfferings(cloudStackUser);

        // verify
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings = getAllServiceOfferingsResponse.getServiceOfferings();
        GetAllServiceOfferingsResponse.ServiceOffering firstServiceOffering = serviceOfferings.get(0);

        Assert.assertNotNull(serviceOfferings);
        Assert.assertEquals(idExpected, firstServiceOffering.getId());
        Assert.assertEquals(nameExpected, firstServiceOffering.getName());
        Assert.assertEquals(cpuNumberExpected, firstServiceOffering.getCpuNumber());
        Assert.assertEquals(memoryExpected, firstServiceOffering.getMemory());
        Assert.assertEquals(tagsExpected, firstServiceOffering.getTags());
    }

    // Test case: The cloudStackUser parameter is null and this throw a exception
    @Test
    public void testGetDiskOfferingsParameterNull() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = null;

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudStackComputePlugin.CLOUDUSER_NULL_EXCEPTION_MSG);

        // exercise
        this.plugin.getDiskOfferings(cloudStackUser);
    }

    // Test case: Trying to get all DiskOfferings in the Cloudstack, but it occurs an error
    @Test
    public void testGetDiskOfferingsErrorInCloudstack() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;

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
        this.plugin.getDiskOfferings(cloudStackUser);
    }

    // Test case: Getting all DiskOfferings in the Cloudstack successfully
    @Test
    public void testGetDiskOfferings() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;
        GetAllDiskOfferingsRequest getAllDiskOfferingRequest = new GetAllDiskOfferingsRequest
                .Builder().build(this.plugin.getCloudStackUrl());
        String getAllDiskOfferingRequestUrl = getAllDiskOfferingRequest.getUriBuilder().toString();

        String idExpected = "id";
        int diskExpected = 3;
        boolean customizedExpected = true;
        String getAllDiskOfferingRequestJsonStr = getListDiskOfferrings(
                idExpected, diskExpected, customizedExpected);

        Mockito.when(this.client.doGetRequest(
                Mockito.eq(getAllDiskOfferingRequestUrl), Mockito.eq(cloudStackUser)))
                .thenReturn(getAllDiskOfferingRequestJsonStr);

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

    // test case: initialization CloudstackComputePLugin with empty zoneid in the properties
    @Test
    public void testGetCheckParametersWithEmptyZoneId() {
        // set up
        Properties properties = new Properties();
        properties.put(CloudStackComputePlugin.ZONE_ID_KEY, "");

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.when(PropertiesUtil.readProperties("")).thenReturn(properties);

        // verify
        this.expectedException.expect(FatalErrorException.class);
        this.expectedException.expectMessage(CloudStackComputePlugin.ZONE_ID_REQUIRED_ERROR_MESSAGE);

        // exercise
        new CloudStackComputePlugin("");
    }

    // test case: initialization CloudstackComputePLugin with null zoneid in the properties
    @Test
    public void testGetCheckParametersWithNullZoneId() {
        // set up
        Properties properties = new Properties();

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.when(PropertiesUtil.readProperties("")).thenReturn(properties);

        // verify
        this.expectedException.expect(FatalErrorException.class);
        this.expectedException.expectMessage(CloudStackComputePlugin.ZONE_ID_REQUIRED_ERROR_MESSAGE);

        // exercise
        new CloudStackComputePlugin("");
    }

    // test case: initialization CloudstackComputePLugin with null defaultnetworkid in the properties
    @Test
    public void testGetCheckParametersWithNullDefaultNetworkId() {
        // set up
        Properties properties = new Properties();
        properties.put(CloudStackComputePlugin.ZONE_ID_KEY, "1");

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.when(PropertiesUtil.readProperties("")).thenReturn(properties);

        // verify
        this.expectedException.expect(FatalErrorException.class);
        this.expectedException.expectMessage(CloudStackComputePlugin.DEFAULT_NETWORK_ID_REQUIRED_ERROR_MESSAGE);

        // exercise
        new CloudStackComputePlugin("");
    }

    // test case: initialization CloudstackComputePLugin with empty defaultnetworkid in the properties
    @Test
    public void testGetCheckParametersWithEmptyDefaultNetworkId() {
        // set up
        Properties properties = new Properties();
        properties.put(CloudStackComputePlugin.ZONE_ID_KEY, "1");
        properties.put(CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY, "");

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.when(PropertiesUtil.readProperties("")).thenReturn(properties);

        // verify
        this.expectedException.expect(FatalErrorException.class);
        this.expectedException.expectMessage(CloudStackComputePlugin.DEFAULT_NETWORK_ID_REQUIRED_ERROR_MESSAGE);

        // exercise
        new CloudStackComputePlugin("");
    }

    // test case: initialization CloudstackComputePLugin and checkparameters successfully
    @Test
    public void testGetCheckParametersSuccessfully() {
        // set up
        Properties properties = new Properties();
        properties.put(CloudStackComputePlugin.ZONE_ID_KEY, "1");
        properties.put(CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY, "1");

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.when(PropertiesUtil.readProperties("")).thenReturn(properties);

        // exercise and verify
        new CloudStackComputePlugin("");
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

    // TODO(chico): check with Fogbow team the requirements. To finish the implementation
    @Ignore
    // test case: get service offering successfuly with use requirements
    @Test
    public void testGetServiceOfferingWithRequirements() throws FogbowException { }

    // TODO(chico): check with Fogbow team the requirements. To finish the implementation
    // test case: get service offering successfuly without use requirements
    @Test
    public void testGetServiceOfferingWithoutRequirements() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;

        ComputeOrder computeOrder = createComputeOrder(
                new ArrayList<UserData>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        int amoutExtraServicesOffering = 5;
        String idServiceOfferingExpected = "idServiceOfferingExpected";
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected =
                createServicesOfferingObjects(idServiceOfferingExpected, amoutExtraServicesOffering);
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

    // test case: get service offering and return null because there are not services offerings in the cloud
    @Test
    public void testGetServiceOfferingAndEmptyServicesOffering() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;

        ComputeOrder computeOrder = createComputeOrder(
                new ArrayList<UserData>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected = new ArrayList<>();
        Mockito.when(getAllServiceOfferingsResponse.getServiceOfferings())
                .thenReturn(servicesOfferingExpected);
        Mockito.doReturn(getAllServiceOfferingsResponse)
                .when(this.plugin).getServiceOfferings(Mockito.eq(cloudStackUser));

        // exercise
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                this.plugin.getServiceOffering(computeOrder, cloudStackUser);

        // verify
        Assert.assertNull(serviceOffering);
    }

    // test case: get service offering and return null because services offering comes null
    @Test
    public void testGetServiceOfferingAndNullServicesOffering() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = FAKE_TOKEN;

        ComputeOrder computeOrder = createComputeOrder(
                new ArrayList<UserData>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected = null;
        Mockito.when(getAllServiceOfferingsResponse.getServiceOfferings())
                .thenReturn(servicesOfferingExpected);
        Mockito.doReturn(getAllServiceOfferingsResponse)
                .when(this.plugin).getServiceOfferings(Mockito.eq(cloudStackUser));

        // exercise
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                this.plugin.getServiceOffering(computeOrder, cloudStackUser);

        // verify
        Assert.assertNull(serviceOffering);
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
    public void testRequestInstance() throws FogbowException, IOException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        String serviceOfferingIdExpected = "serviceOfferingId";
        int serviceOfferingCpuExpected = 10;
        int serviceOfferingMemoryExpected = 20;
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        Mockito.when(serviceOffering.getId()).thenReturn(serviceOfferingIdExpected);
        Mockito.when(serviceOffering.getMemory()).thenReturn(serviceOfferingMemoryExpected);
        Mockito.when(serviceOffering.getCpuNumber()).thenReturn(serviceOfferingCpuExpected);
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        String diskOfferingIdExpected = "diskOfferingId";
        int diskOfferingSizeExpected = 10;
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOffering.getId()).thenReturn(diskOfferingIdExpected);
        Mockito.when(diskOffering.getDiskSize()).thenReturn(diskOfferingSizeExpected);
        Mockito.doReturn(diskOffering).when(this.plugin).getDiskOffering(
                Mockito.eq(order.getDisk()), Mockito.any(CloudStackUser.class));

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeUserDataString = "anystring";
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class)))
                .thenReturn(fakeUserDataString);

        String idVirtualMachineExpected = "1";
        String serviceOfferingResponse = getDeployVirtualMachineResponse(idVirtualMachineExpected);
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);

        // exercise
        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        // verify
        Assert.assertEquals(idVirtualMachineExpected, createdVirtualMachineId);
        Assert.assertEquals(diskOfferingSizeExpected, order.getActualAllocation().getDisk());
        Assert.assertEquals(serviceOfferingMemoryExpected, order.getActualAllocation().getRam());
        Assert.assertEquals(serviceOfferingCpuExpected, order.getActualAllocation().getvCPU());
    }

    // Test case: request instance but the templateId comes null and throw a exception
    @Test
    public void testRequestInstanceTemplateIdNull() throws FogbowException {
        // set up
        ComputeOrder order = Mockito.mock(ComputeOrder.class);
        Mockito.when(order.getImageId()).thenReturn(null);

        // verify
        this.expectedException.expect(InvalidParameterException.class);
        this.expectedException.expectMessage(Messages.Error.UNABLE_TO_COMPLETE_REQUEST_CLOUDSTACK);

        // exercise
        this.plugin.requestInstance(order, FAKE_TOKEN);
    }

    // Test case: request instance but the service offering is null and throw a exception
    @Test
    public void testRequestInstanceServiceOfferingNull() throws FogbowException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering = null;
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);

        // exercise
        this.plugin.requestInstance(order, FAKE_TOKEN);
    }

    // Test case: request instance but the disk offering is null and throw a exception
    @Test
    public void testRequestInstanceDiskOfferingNull() throws FogbowException {
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
        Mockito.doReturn(diskOffering).when(this.plugin).getDiskOffering(
                Mockito.eq(order.getDisk()), Mockito.any(CloudStackUser.class));
        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK);

        // exercise
        this.plugin.requestInstance(order, FAKE_TOKEN);
    }

    // Test case: request instance but it occurs a error in the request to the cloud and throw a exception
    @Test
    public void testRequestInstanceErrorInRequest() throws FogbowException, HttpResponseException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        String serviceOfferingIdExpected = "serviceOfferingId";
        int serviceOfferingCpuExpected = 10;
        int serviceOfferingMemoryExpected = 20;
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        Mockito.when(serviceOffering.getId()).thenReturn(serviceOfferingIdExpected);
        Mockito.when(serviceOffering.getMemory()).thenReturn(serviceOfferingMemoryExpected);
        Mockito.when(serviceOffering.getCpuNumber()).thenReturn(serviceOfferingCpuExpected);
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        String diskOfferingIdExpected = "diskOfferingId";
        int diskOfferingSizeExpected = 10;
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOffering.getId()).thenReturn(diskOfferingIdExpected);
        Mockito.when(diskOffering.getDiskSize()).thenReturn(diskOfferingSizeExpected);
        Mockito.doReturn(diskOffering).when(this.plugin).getDiskOffering(
                Mockito.eq(order.getDisk()), Mockito.any(CloudStackUser.class));

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeUserDataString = "anystring";
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class)))
                .thenReturn(fakeUserDataString);

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestInstance(order, FAKE_TOKEN);
    }

    // TODO(chico) - Finish the implementation
    @Ignore
    // test case:
    @Test
    public void testGetComputeInstance() { }

    // TODO(chico) - Finish the implementation
    @Ignore
    // test case:
    @Test
    public void testGetVirtualMachineDiskSize() { }

    // test case: get instance successfully
    @Test
    public void testGetInstance() throws FogbowException, IOException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = FAKE_TOKEN;

        String id = "id";
        String name = "name";
        String state = "state";
        int cpu = 1;
        int memory = 2;
        String idAddress = "10.10.0.10";
        String getVirtualMachineResponse = getVirtualMachineResponse(
                id, name, state, cpu, memory, idAddress);
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(getVirtualMachineResponse);

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.plugin)
                .getComputeInstance(Mockito.any(), Mockito.any());

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise
        ComputeInstance computeInstance = this.plugin.getInstance(computeOrder, cloudStackUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
    }

    // test case: get instance not found
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceNoFound() throws FogbowException, IOException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = FAKE_TOKEN;

        String responseRequest = new String();
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(responseRequest);

        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = null;
        Mockito.when(getVirtualMachineResponse.getVirtualMachines()).thenReturn(virtualMachines);

        PowerMockito.mockStatic(GetVirtualMachineResponse.class);
        PowerMockito.when(GetVirtualMachineResponse.fromJson(Mockito.eq(responseRequest)))
                .thenReturn(getVirtualMachineResponse);

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise and verify
        this.plugin.getInstance(computeOrder, cloudStackUser);
    }

    // test case: get instance and occurs a bad request
    @Test(expected = FogbowException.class)
    public void testGetInstanceBadRequest() throws FogbowException, IOException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = FAKE_TOKEN;

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(createBadRequestHttpResponse());

        // ignoring CloudStackUrlUtil
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise and verify
        this.plugin.getInstance(computeOrder, cloudStackUser);
    }

    // Test case: deleting an instance successfully
    @Test
    public void deleteInstance() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(
                Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.eq(FAKE_TOKEN))).thenReturn("");

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        this.plugin.deleteInstance(computeOrder, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN));
    }

    // Test case: failing to delete an instance
    @Test(expected = FogbowException.class)
    public void deleteInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(createBadRequestHttpResponse());

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        this.plugin.deleteInstance(computeOrder, FAKE_TOKEN);

        // verify
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(Mockito.anyString(), FAKE_TOKEN);
    }

    private String getVirtualMachineResponse(String id, String name, String state,
            int cpunumber, int memory, String ipaddress) throws IOException {

        List<GetVirtualMachineResponse.Nic> nics = new ArrayList<>();
        nics.add(new GetVirtualMachineResponse().new Nic(ipaddress));
        return CloudstackTestUtils.createGetVirtualMachineResponseJson(
                id, name, state, cpunumber, memory, nics);
    }

    private String getVirtualMachineResponse() {
        String response = "{\"listvirtualmachinesresponse\":{}}";

        return response;
    }

    private String getVolumeResponse(String id, String name, String size, String state) {
        String response = "{\"id\":\"%s\","
                + "\"name\":\"%s\","
                + "\"size\":\"%s\","
                + "\"state\":\"%s\""
                + "}";

        return String.format(response, id, name, size, state);
    }

    private String getListVolumesResponse(String volume) {
        String response = "{\"listvolumesresponse\":{\"volume\":[%s]}}";

        return String.format(response, volume);
    }

    private String getListVolumesResponse() {
        String response = "{\"listvolumesresponse\":{}}";

        return response;
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

    private String getDeployVirtualMachineResponse(String id) throws IOException {
        return CloudstackTestUtils.createDeployVirtualMachineResponseJson(id);
    }

    private ComputeOrder createComputeOrder(ArrayList<UserData> fakeUserData, String fakeImageId) {
        SystemUser requester = new SystemUser(FAKE_USER_ID, FAKE_USERNAME, FAKE_ID_PROVIDER);
        NetworkOrder networkOrder = new NetworkOrder(FAKE_NETWORK_ID);
        networkOrder.setSystemUser(requester);
        networkOrder.setProvider(FAKE_MEMBER);
        networkOrder.setCloudName(CLOUD_NAME);
        networkOrder.setInstanceId(FAKE_NETWORK_ID);
        networkOrder.setOrderStateInTestMode(OrderState.FULFILLED);
        this.sharedOrderHolders.getActiveOrdersMap().put(networkOrder.getId(), networkOrder);
        List<String> networkOrderIds = new ArrayList<>();
        networkOrderIds.add(networkOrder.getId());
        ComputeOrder computeOrder = new ComputeOrder(requester, FAKE_MEMBER, FAKE_MEMBER, CLOUD_NAME, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, networkOrderIds);
        computeOrder.setInstanceId(FAKE_ID);
        this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
        return computeOrder;
    }

    private List<GetAllServiceOfferingsResponse.ServiceOffering> createServicesOfferingObjects(
            String idServiceOfferingToTest, int amoutExtraServicesOffering) {

        if (amoutExtraServicesOffering < 0) {
            throw new IllegalArgumentException("Value not allowed");
        }
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOffering = new ArrayList<>();
        servicesOffering.add(new GetAllServiceOfferingsResponse().new ServiceOffering(
                idServiceOfferingToTest, Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY)));

        for (int i = 0; i < amoutExtraServicesOffering; i++) {
            String randonId = UUID.randomUUID().toString();
            servicesOffering.add(new GetAllServiceOfferingsResponse().new ServiceOffering(
                    randonId, Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY)));
        }

        int amoutExpected = amoutExtraServicesOffering + 1;
        Assert.assertEquals(amoutExpected, servicesOffering.size());

        return servicesOffering;
    }

    private HttpResponseException createBadRequestHttpResponse() {
        return new HttpResponseException(HttpStatus.SC_BAD_REQUEST, BAD_REQUEST_MSG);
    }
}
