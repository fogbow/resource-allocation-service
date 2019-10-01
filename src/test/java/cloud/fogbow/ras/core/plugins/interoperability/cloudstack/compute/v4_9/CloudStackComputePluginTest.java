package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.Matcher;
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

@PrepareForTest({SharedOrderHolders.class, CloudStackUrlUtil.class, GetAllServiceOfferingsResponse.class,
        DefaultLaunchCommandGenerator.class, PropertiesUtil.class, GetVirtualMachineResponse.class,
        DatabaseManager.class, DeployVirtualMachineResponse.class, GetVolumeResponse.class,
        GetAllDiskOfferingsResponse.class})
public class CloudStackComputePluginTest extends BaseUnitTests {

    private static final String BAD_REQUEST_MSG = "Bad Request";
    private final int AMOUNT_EXTRA_SERVICE_OFFERING = 4;
    private static final String CLOUDSTACK_CLOUD_NAME = "cloudstack";

    private CloudStackComputePlugin plugin;
    private CloudStackHttpClient client;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private Properties properties;
    private String defaultNetworkId;
    private String expungeOnDestroy;
    private String cloudstackUrl;
    private String zoneId;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws UnexpectedException, InvalidParameterException {
        String cloudStackConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + CLOUDSTACK_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.defaultNetworkId = this.properties.getProperty(
                CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY);
        this.zoneId = this.properties.getProperty(
                CloudStackComputePlugin.ZONE_ID_KEY_CONF);
        this.cloudstackUrl = this.properties.getProperty(
                CloudStackComputePlugin.CLOUDSTACK_URL_CONF);
        this.expungeOnDestroy = this.properties.getProperty(
                CloudStackComputePlugin.EXPUNGE_ON_DESTROY_KEY_CONF);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.plugin = Mockito.spy(new CloudStackComputePlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.plugin.setLaunchCommandGenerator(this.launchCommandGeneratorMock);
        this.testUtils.mockReadOrdersFromDataBase();
        ignoringCloudStackUrl();
    }

    // test case: When calling the doGetInstance method with secondary methods mocked,
    // it must verify if It returns the computeInstance correct.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        GetVirtualMachineRequest request = Mockito.mock(GetVirtualMachineRequest.class);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        GetVirtualMachineResponse response = Mockito.mock(GetVirtualMachineResponse.class);
        Mockito.doReturn(response).when(this.plugin).requestGetVirtualMachine(
                Mockito.eq(request), Mockito.eq(cloudStackUser));

        ComputeInstance computeInstanceExpeceted = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpeceted).when(this.plugin).buildComputeInstance(
                Mockito.eq(response), Mockito.eq(cloudStackUser));

        // exercise
        ComputeInstance computeInstance = this.plugin.doGetInstance(request, cloudStackUser);

        // verify
        Assert.assertEquals(computeInstanceExpeceted, computeInstance);
    }

    // test case: When calling the doGetInstance method, it is threw a FogbowException in the
    // requestGetVirtualMachine method and it never will call the buildComputeInstanceMethod
    @Test
    public void testDoGetInstanceFail() throws FogbowException {
        // set up
        GetVirtualMachineRequest request = Mockito.mock(GetVirtualMachineRequest.class);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        GetVirtualMachineResponse response = Mockito.mock(GetVirtualMachineResponse.class);
        Mockito.doThrow(new FogbowException()).when(this.plugin).requestGetVirtualMachine(
                Mockito.eq(request), Mockito.eq(cloudStackUser));

        // exercise
        try {
            this.plugin.doGetInstance(request, cloudStackUser);
        } catch (Exception e) {
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN)).buildComputeInstance(
                    Mockito.eq(response), Mockito.eq(cloudStackUser));
        }
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked,
    // it must verify if it returns the instanceId correct.
    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffice =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        DeployVirtualMachineRequest request = Mockito.mock(DeployVirtualMachineRequest.class);
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);

        String instanceIdExpected = "instanceId";
        DeployVirtualMachineResponse response = Mockito.mock(DeployVirtualMachineResponse.class);
        Mockito.when(response.getId()).thenReturn(instanceIdExpected);
        Mockito.doReturn(response).when(this.plugin)
                .requestDeployVirtualMachine(Mockito.eq(request), Mockito.eq(cloudStackUser));

        Mockito.doNothing().when(this.plugin).updateComputeOrder(Mockito.eq(computeOrder),
                Mockito.eq(serviceOffice), Mockito.eq(diskOffering));

        // exercise
        String instanceId = this.plugin.doRequestInstance(
                request, serviceOffice, diskOffering, computeOrder, cloudStackUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateComputeOrder(
                Mockito.eq(computeOrder), Mockito.eq(serviceOffice), Mockito.eq(diskOffering));
    }

    // test case: When calling the doRequestInstance method, it is threw a FogbowException in the
    // requestDeployVirtualMachine method and It never will call the updateComputeOrder.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffice =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        DeployVirtualMachineRequest request = Mockito.mock(DeployVirtualMachineRequest.class);
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .requestDeployVirtualMachine(Mockito.any(), Mockito.any());

        // exercise
        try {
            this.plugin.doRequestInstance(request, serviceOffice, diskOffering, computeOrder, cloudStackUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN))
                    .updateComputeOrder(Mockito.eq(computeOrder), Mockito.eq(serviceOffice), Mockito.eq(diskOffering));
        }

    }

    // test case: When calling the getServiceOfferings method, a HttpResponseException occurs,
    // and it must verify if a FogbowException has been thrown.
    // note: CloudStackUrlUtil.sign() is beeing mocked in the @Before test method.
    @Test
    public void testGetServiceOfferingsFail() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        HttpResponseException badRequestHttpResponse = createBadRequestHttpResponse();
        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(badRequestHttpResponse);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.getServiceOfferings(cloudStackUser);
    }

    // test case: calling the getServiceOfferings method, secondary methods are mocked
    // , it must verify if it is returned the instanceId correct.
    // note: CloudStackUrlUtil.sign() is beeing mocked in the @Before test method.
    @Test
    public void testGetServiceOfferingsSuccessfully() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetAllServiceOfferingsRequest getAllServiceOfferingRequest = new GetAllServiceOfferingsRequest
                .Builder().build(this.cloudstackUrl);
        String getAllServiceOfferingRequestUrl = getAllServiceOfferingRequest.getUriBuilder().toString();

        String responseStr = "anyString";
        Mockito.when(this.client.doGetRequest(
                Mockito.eq(getAllServiceOfferingRequestUrl), Mockito.eq(cloudStackUser)))
                .thenReturn(responseStr);

        PowerMockito.mockStatic(GetAllServiceOfferingsResponse.class);
        GetAllServiceOfferingsResponse responseExpected = Mockito.mock(GetAllServiceOfferingsResponse.class);
        PowerMockito.when(GetAllServiceOfferingsResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(responseExpected);

        // exercise
        GetAllServiceOfferingsResponse response =
                this.plugin.getServiceOfferings(cloudStackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
    }

    // test case: When calling the getDiskOfferings method and a HttpResponseException occurs,
    // , it must verify if a FogbowException has been thrown.
    // note: CloudStackUrlUtil.sign() is beeing mocked in the @Before test method.
    @Test
    public void testGetDiskOfferingsFail() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        HttpResponseException badRequestHttpResponse = createBadRequestHttpResponse();
        Mockito.doThrow(badRequestHttpResponse).when(this.plugin).doGet(
                Mockito.anyString(), Mockito.any(CloudStackUser.class));

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.getDiskOfferings(cloudStackUser);
    }

    // ,it must verify if it is returned the GetAllDiskOfferingsResponse correct
    // note: CloudStackUrlUtil.sign() is beeing mocked in the @Before test method.
    @Test
    public void testGetDiskOfferingsSuccessfully() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetAllDiskOfferingsRequest getAllDiskOfferingRequest = new GetAllDiskOfferingsRequest
                .Builder().build(this.cloudstackUrl);
        String getAllDiskOfferingRequestUrl = getAllDiskOfferingRequest.getUriBuilder().toString();

        String responseJson = "anySthing";
        Mockito.doReturn(responseJson).when(this.plugin).doGet(
                Mockito.eq(getAllDiskOfferingRequestUrl), Mockito.eq(cloudStackUser));

        PowerMockito.mockStatic(GetAllDiskOfferingsResponse.class);
        GetAllDiskOfferingsResponse responseExpected = Mockito.mock(GetAllDiskOfferingsResponse.class);
        PowerMockito.when(GetAllDiskOfferingsResponse.fromJson(Mockito.eq(responseJson)))
                .thenReturn(responseExpected);

        // exercise
        GetAllDiskOfferingsResponse response = this.plugin.getDiskOfferings(cloudStackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
    }

    // test case: calling the normalizeNetworksID method and the order comes with extra network,
    // it must verify if there are the default network and the extra network.
    @Test
    public void testNormalizeNetworksIDWithExtraNetwork() {
        // set up
        ComputeOrder computeOrderWithExtraNetwork = createComputeOrder(
                new ArrayList<>(), "fake-image-id");
        String networksIDExpected = String.format(
                "%s,%s", this.defaultNetworkId, TestUtils.FAKE_NETWORK_ID);

        // exercise
        String networksID = this.plugin.normalizeNetworksID(computeOrderWithExtraNetwork);

        // verify
        Assert.assertEquals(networksIDExpected, networksID);
    }

    // test case: calling the normalizeNetworksID method and the order comes without extra network,
    // it must verify if there is only the default network.
    @Test
    public void testNormalizeNetworksIDWithoutExtraNetwork() {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getNetworkOrderIds()).thenReturn(new ArrayList<>());
        String networksIDExpected = this.defaultNetworkId;

        // exercise
        String networksID = this.plugin.normalizeNetworksID(computeOrder);

        // verify
        Assert.assertEquals(networksIDExpected, networksID);
    }

    // test case: calling the getServiceOffering method and the order capabilities(memory and cpu)
    // matchs with the services offerings existing, it must verify if It returns the first right service offering.
    @Test
    public void testGetServiceOfferingSuccessfully() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");

        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                Mockito.mock(GetAllServiceOfferingsResponse.class);

        String idServiceOfferingExpected = "idServiceOfferingExpected";
        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOfferingExpected =
                createServicesOfferingObjects(this.testUtils.MEMORY_VALUE , this.testUtils.CPU_VALUE);
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

    // test case: calling the getServiceOffering method and there are not services offerings
    // , it must verify if It a NoAvailableResourcesException has been thrown.
    @Test
    public void testGetServiceOfferingFail() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

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

    // test case: calling the getServiceOffering method and the order capabilities(memory and cpu)
    // does not matchs with the services offerings existing, it must verify if
    // a NoAvailableResourcesException has been thrown.
    @Test
    public void testGetServiceOfferingWithoutMatching() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

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

    // test case: calling the normalizeInstanceName method with a not null parameter,
    // it must verify if It returns the right value.
    @Test
    public void testNormalizeInstanceNameWithNotNullValue() {
        // set up
        String instanceNameExpected = "instanceName";

        // exercise
        String instanceName = this.plugin.normalizeInstanceName(instanceNameExpected);

        // verify
        Assert.assertEquals(instanceNameExpected, instanceName);
    }

    // test case: calling the normalizeInstanceName method with a null paramenter,
    // it must verify if It returns the Fogbow default value.
    @Test
    public void testNormalizeInstanceNameWithNullValue() {
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

    // test case: When calling the requestInstance method with secondary methods mocked,
    // it must verify if the requestDeployVirtualMachine is called with the right parameters;
    // this includes the checking in the Cloudstack request.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
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
                .requestDeployVirtualMachine(Mockito.any(), Mockito.eq(cloudStackUser));

        Mockito.doNothing().when(this.plugin).updateComputeOrder(
                Mockito.eq(order), Mockito.eq(serviceOffering), Mockito.eq(diskOffering));

        DeployVirtualMachineRequest requestExpected = new DeployVirtualMachineRequest.Builder()
                .serviceOfferingId(serviceOffering.getId())
                .templateId(order.getImageId())
                .zoneId(this.zoneId)
                .name(order.getName())
                .diskOfferingId(diskOffering.getId())
                .userData(fakeUserDataString)
                .networksId(networksIds)
                .build(this.cloudstackUrl);

        // exercise
        this.plugin.requestInstance(order, cloudStackUser);

        // verify
        Matcher<DeployVirtualMachineRequest> matcher = new RequestMatcher.DeployVirtualMachine(requestExpected);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                Mockito.argThat(matcher),
                Mockito.eq(serviceOffering),
                Mockito.eq(diskOffering),
                Mockito.eq(order),
                Mockito.eq(cloudStackUser));
    }

    // test case: When calling the requestInstance method with occcurs an exception in the
    // getServiceOffering, it must verify if a NoAvailableResourcesException was threw and
    // it was interrupted the method execution.
    @Test
    public void testRequestInstanceFailOnServiceOffering() throws FogbowException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        Mockito.doThrow(new NoAvailableResourcesException()).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        // exercise
        try {
            this.plugin.requestInstance(order, cloudStackUser);
            Assert.fail();
        } catch (NoAvailableResourcesException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN))
                    .getDiskOffering(Mockito.anyInt(), Mockito.eq(cloudStackUser));
        }

    }

    // test case: When calling the requestInstance method with occcurs an exception in the
    // getDiskOffering, it must verify if a NoAvailableResourcesException was threw and
    // it was interrupted the method execution.
    @Test
    public void testRequestInstanceFailOnDiskOffering() throws FogbowException {
        // set up
        ComputeOrder order = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        String networksIds = "networksId";
        Mockito.doReturn(networksIds).when(this.plugin)
                .normalizeNetworksID(Mockito.any(ComputeOrder.class));

        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                Mockito.mock(GetAllServiceOfferingsResponse.ServiceOffering.class);
        Mockito.doReturn(serviceOffering).when(this.plugin).getServiceOffering(
                Mockito.eq(order) , Mockito.any(CloudStackUser.class));

        int orderDisk = order.getDisk();
        Mockito.doThrow(new NoAvailableResourcesException()).when(this.plugin).getDiskOffering(
                Mockito.eq(orderDisk), Mockito.eq(cloudStackUser));

        // exercise
        try {
            this.plugin.requestInstance(order, cloudStackUser);
        } catch (NoAvailableResourcesException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .getServiceOffering(Mockito.eq(order), Mockito.eq(cloudStackUser));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN))
                    .normalizeInstanceName(Mockito.any());
        }
    }

    // test case: When calling the requestInstance method with occcurs an exception in the
    // requestDeployVirtualMachine, it must verify if a FogbowException was threw
    @Test
    public void testRequestInstanceFailInRequest() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
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
                .requestDeployVirtualMachine(Mockito.any(), Mockito.eq(cloudStackUser));

        // exercise
        try {
            this.plugin.requestInstance(order, cloudStackUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                    Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any(),Mockito.any());
        }
    }

    // test case: calling the createComputeInstance method,
    // it must verify if It returns the right ComputeInstance.
    @Test
    public void testCreateComputeInstanceSuccessfully() {
        // set up
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

        // verify
        ComputeInstance computeInstance =
                this.plugin.createComputeInstance(virtualMachine, diskExpected);

        // exercise
        Assert.assertEquals(idExpected, computeInstance.getId());
        Assert.assertEquals(nameExpected, computeInstance.getName());
        Assert.assertEquals(vCpuExpected, computeInstance.getvCPU());
        Assert.assertEquals(memoryExpected, computeInstance.getMemory());
        Assert.assertEquals(diskExpected, computeInstance.getDisk());
        Assert.assertEquals(ipAddressExpected, computeInstance.getIpAddresses().get(0));
        Assert.assertEquals(networkDefaultExpected, computeInstance.getNetworks().get(0).getId());
    }

    // test case: calling the getVirtualMachineDiskSize method with secondary methods mocked,
    // it must verify if It returns the right disk size.
    @Test
    public void testGetVirtualMachineDiskSizeSuccessfully() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        String virtualMachineIdExpected = "id";

        String getVolumeResponseStr = "anyString";
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(cloudStackUser)))
                .thenReturn(getVolumeResponseStr);

        PowerMockito.mockStatic(GetVolumeResponse.class);
        GetVolumeResponse getVolumeResponse = Mockito.mock(GetVolumeResponse.class);

        List<GetVolumeResponse.Volume> volumes = new ArrayList<>();
        int sizeGBExpected = 5;
        long sizeBytes = (long) (sizeGBExpected * CloudStackComputePlugin.GIGABYTE_IN_BYTES);
        GetVolumeResponse.Volume volume = Mockito.mock(GetVolumeResponse.Volume.class);
        Mockito.when(volume.getSize()).thenReturn(sizeBytes);
        volumes.add(volume);

        Mockito.when(getVolumeResponse.getVolumes()).thenReturn(volumes);

        PowerMockito.when(GetVolumeResponse.fromJson(Mockito.eq(getVolumeResponseStr)))
                .thenReturn(getVolumeResponse);

        GetVolumeRequest requestExpected = new GetVolumeRequest.Builder()
                .virtualMachineId(virtualMachineIdExpected)
                .type(CloudStackComputePlugin.DEFAULT_VOLUME_TYPE_VALUE)
                .build(this.cloudstackUrl);
        String requestUrlExpexted = requestExpected.getUriBuilder().toString();

        // exercise
        int virtualMachineDiskSize = this.plugin.getVirtualMachineDiskSize(
                virtualMachineIdExpected, cloudStackUser);

        // verify
        Assert.assertEquals(sizeGBExpected, virtualMachineDiskSize);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGet(Mockito.eq(requestUrlExpexted), Mockito.eq(cloudStackUser));
    }

    // test case: calling the getVirtualMachineDiskSize method with secondary methods mocked and
    // the cloud returns 0 volumes, it must verify if It returns the Fogbow default value
    @Test
    public void testGetVirtualMachineDiskFailWhenNoVolumes() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        String virtualMachineIdExpected = "id";

        String anyResponse = "";
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(cloudStackUser)))
                .thenReturn(anyResponse);

        GetVolumeResponse volumeResponse = Mockito.mock(GetVolumeResponse.class);
        List<GetVolumeResponse.Volume> volumesEmpty = new ArrayList<>();
        Mockito.when(volumeResponse.getVolumes()).thenReturn(volumesEmpty);
        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(Mockito.eq(anyResponse)))
                .thenReturn(volumeResponse);

        GetVolumeRequest requestExpected = new GetVolumeRequest.Builder()
                .virtualMachineId(virtualMachineIdExpected)
                .type(CloudStackComputePlugin.DEFAULT_VOLUME_TYPE_VALUE)
                .build(this.cloudstackUrl);
        String requestUrlExpexted = requestExpected.getUriBuilder().toString();

        // exercise
        int virtualMachineDiskSize = this.plugin.getVirtualMachineDiskSize(
                virtualMachineIdExpected, cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackComputePlugin.UNKNOWN_DISK_VALUE, virtualMachineDiskSize);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGet(Mockito.eq(requestUrlExpexted), Mockito.eq(cloudStackUser));
    }

    // test case: calling the getVirtualMachineDiskSize method with secondary methods mocked and
    // it occurs a Exception, it must verify if It returns the Fogbow default value
    @Test
    public void testGetVirtualMachineDiskSizeFail() throws FogbowException, IOException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        String virtualMachineId = "id";

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(cloudStackUser)))
                .thenThrow(createBadRequestHttpResponse());

        // exercise
        int virtualMachineDiskSize = this.plugin.getVirtualMachineDiskSize(virtualMachineId, cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackComputePlugin.UNKNOWN_DISK_VALUE, virtualMachineDiskSize);
    }

    // test case: When calling the buildComputeInstance method with secondary methods mocked,
    // it must verify if it returns the ComputeInstance correct.
    @Test
    public void testBuildComputeInstanceSuccessfully() throws InstanceNotFoundException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = new ArrayList<>();
        GetVirtualMachineResponse.VirtualMachine virtualMachine =
                Mockito.mock(GetVirtualMachineResponse.VirtualMachine.class);
        String virtualMachineId = "virtualMachineId";
        Mockito.when(virtualMachine.getId()).thenReturn(virtualMachineId);
        virtualMachines.add(virtualMachine);
        Mockito.when(getVirtualMachineResponse.getVirtualMachines()).thenReturn(virtualMachines);

        int disk = 5;
        Mockito.doReturn(disk).when(this.plugin).getVirtualMachineDiskSize(
                Mockito.eq(virtualMachineId), Mockito.eq(cloudStackUser));

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.plugin).createComputeInstance(
                Mockito.eq(virtualMachine), Mockito.eq(disk));

        // exercise
        ComputeInstance computeInstance = this.plugin.buildComputeInstance(
                getVirtualMachineResponse, cloudStackUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
    }

    // test case: calling the buildComputeInstanceFail method and it occurs a Exception,
    // it must verify if it was threw a InstanceNotFoundException
    @Test
    public void testBuildComputeInstanceFail() throws InstanceNotFoundException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = new ArrayList<>();
        Mockito.when(getVirtualMachineResponse.getVirtualMachines()).thenReturn(virtualMachines);

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.plugin.buildComputeInstance(getVirtualMachineResponse, cloudStackUser);
    }

    // test case: When calling the requestGetVirtualMachine method with secondary methods mocked,
    // it must verify if it returns the GetVirtualMachineResponse correct.
    @Test
    public void testRequestGetVirtualMachineSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .build("anything");
        URIBuilder uriRequest = getVirtualMachineRequest.getUriBuilder();

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
                this.plugin.requestGetVirtualMachine(getVirtualMachineRequest, cloudStackUser);

        // verify
        Assert.assertEquals(getVirtualMachineResponseExpexted, getVirtualMachineResponse);
    }

    // test case: calling the requestGetVirtualMachineFail method and it occurs a Exception,
    // it must verify if it was threw a FogbowException
    @Test
    public void testRequestGetVirtualMachineFail() throws FogbowException, HttpResponseException {

        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .build("anything");
        URIBuilder uriRequest = getVirtualMachineRequest.getUriBuilder();

        Mockito.doThrow(createBadRequestHttpResponse()).when(this.plugin)
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestGetVirtualMachine(getVirtualMachineRequest, cloudStackUser);
    }

    // test case: When calling the getInstance method with secondary methods mocked,
    // it must verify if it returns the right ComputeInstance;
    // this includes the checking in the Cloudstack request.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        GetVirtualMachineResponse getVirtualMachineResponse = Mockito.mock(GetVirtualMachineResponse.class);
        Mockito.doReturn(getVirtualMachineResponse).when(this.plugin)
                .requestGetVirtualMachine(Mockito.any(), Mockito.eq(cloudStackUser));

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.plugin)
                .buildComputeInstance(Mockito.eq(getVirtualMachineResponse), Mockito.eq(cloudStackUser));

        CloudStackRequest requestExpected = new GetVirtualMachineRequest.Builder()
                .id(computeOrder.getInstanceId())
                .build(this.cloudstackUrl);

        // exercise
        ComputeInstance computeInstance = this.plugin.getInstance(computeOrder, cloudStackUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
        Matcher<GetVirtualMachineRequest> matcher = new RequestMatcher.GetVirtualMachine(requestExpected);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
                Mockito.argThat(matcher), Mockito.eq(cloudStackUser));
    }

    // test case: calling the getInstance method and it occurs a Exception,
    // it must verify if it was threw a FogbowException
    @Test
    public void testGetInstanceFail() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "fake-image-id");
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .requestGetVirtualMachine(Mockito.any(), Mockito.eq(cloudStackUser));

        // exercise
        try {
            this.plugin.getInstance(computeOrder, cloudStackUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN))
                    .buildComputeInstance(Mockito.any(), Mockito.any());
        }
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked,
    // it must verify if it returns the right ComputeInstance.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws FogbowException, HttpResponseException {
        //set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        DestroyVirtualMachineRequest destroyVirtualMachineRequest = new DestroyVirtualMachineRequest
                .Builder().build("anything");
        URIBuilder uriRequest = destroyVirtualMachineRequest.getUriBuilder();
        String instanceId = "InstanceId";

        Mockito.doReturn(new String()).when(this.plugin).doGet(Mockito.eq(uriRequest.toString()),
                Mockito.eq(cloudStackUser));

        // exercise
        this.plugin.doDeleteInstance(destroyVirtualMachineRequest, cloudStackUser, instanceId);

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));
    }

    // test case: calling the doDeleteInstance method and it occurs a Exception,
    // it must verify if it was threw a FogbowException.
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException, HttpResponseException {
        //set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        DestroyVirtualMachineRequest destroyVirtualMachineRequest = new DestroyVirtualMachineRequest
                .Builder().build("anything");
        URIBuilder uriRequest = destroyVirtualMachineRequest.getUriBuilder();
        String instanceId = "InstanceId";

        Mockito.doThrow(createBadRequestHttpResponse()).when(this.plugin)
                .doGet(Mockito.eq(uriRequest.toString()), Mockito.eq(cloudStackUser));

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDeleteInstance(destroyVirtualMachineRequest, cloudStackUser, instanceId);
    }

    // test case: When calling the deleteInstance method with secondary methods mocked,
    // it must check the Cloudstack request is the expected.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.eq(cloudStackUser))).thenReturn("");

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(this.testUtils.FAKE_INSTANCE_ID);

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(),
                Mockito.eq(cloudStackUser), Mockito.endsWith(this.testUtils.FAKE_INSTANCE_ID));

        CloudStackRequest requestExpected = new DestroyVirtualMachineRequest.Builder()
                .id(computeOrder.getInstanceId())
                .expunge(this.expungeOnDestroy)
                .build(this.cloudstackUrl);

        // exercise
        this.plugin.deleteInstance(computeOrder, cloudStackUser);

        // verify
        Matcher<DestroyVirtualMachineRequest> matcher = new RequestMatcher.DestroyVirtualMachine(requestExpected);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.argThat(matcher),
                        Mockito.eq(cloudStackUser), Mockito.endsWith(this.testUtils.FAKE_INSTANCE_ID));
    }

    // test case: calling the deleteInstance method and it occurs a Exception,
    // it must verify if it was threw a FogbowException.
    @Test
    public void testDeleteInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(cloudStackUser)))
                .thenThrow(createBadRequestHttpResponse());

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(this.testUtils.FAKE_INSTANCE_ID);

        Mockito.doThrow(new FogbowException()).when(this.plugin).doDeleteInstance(Mockito.any(),
                Mockito.eq(cloudStackUser), Mockito.endsWith(this.testUtils.FAKE_INSTANCE_ID));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.deleteInstance(computeOrder, cloudStackUser);
    }

    // test case: When calling the filterServicesOfferingByRequirements method with secondary methods mocked,
    // also is passed the computeOrder with requirements and there are several services offering,
    // it must verify if it returns the service offering that it matchs with the order requirements.
    @Test
    public void testFilterServicesOfferingSuccessfully() {
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

        List<GetAllServiceOfferingsResponse.ServiceOffering> servicesOffering =
                createServicesOfferingObjects(TestUtils.MEMORY_VALUE, TestUtils.CPU_VALUE);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOfferingExpected =
                new GetAllServiceOfferingsResponse().new ServiceOffering(
                "anyId", TestUtils.CPU_VALUE, TestUtils.MEMORY_VALUE, tagExpected) ;
        servicesOffering.add(serviceOfferingExpected);

        // verify before
        Assert.assertEquals(AMOUNT_EXTRA_SERVICE_OFFERING + 1, servicesOffering.size());

        // exercise
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFilted =
                this.plugin.filterServicesOfferingByRequirements(servicesOffering, computeOrder);

        // verify
        Assert.assertEquals(1, serviceOfferingsFilted.size());
        Assert.assertEquals(serviceOfferingExpected, serviceOfferingsFilted.get(0));
    }

    // test case: When calling the filterServicesOfferingByRequirements method with secondary methods mocked,
    // also is passed the computeOrder with requirements and there are several services offering,
    // it must verify if it returns a empty list because no services offering match with the requirements.
    @Test
    public void testFilterServicesOfferingByRequirementsSuccessfullyNotMatch() {
        // set up
        ComputeOrder computeOrder = createComputeOrder(new ArrayList<>(), "");
        Map<String, String> requirements = new HashMap<>();
        String keyOne = "one";
        String valueOne = "one";
        String tagOne = keyOne + CloudStackComputePlugin.FOGBOW_TAG_SEPARATOR + valueOne;
        requirements.put(keyOne, valueOne);
        String keyTwo = "two";
        String valueTwo = "two";
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

    // test case: When calling the filterServicesOfferingByRequirements method with secondary methods mocked,
    // also is passed the computeOrder without requirements and there are several services offering,
    // it must verify if it returns all services offering.
    @Test
    public void testFilterServicesOfferingByRequirementsSuccessfullyWitoutRequirements() {
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

    // test case: When calling the getDiskOffering method with secondary methods mocked,
    // it must verify if it returns the right diskOffering that it matchs with size required.
    @Test
    public void testGetDiskOfferingSuccessfully() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        int diskExpected = 1;

        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse = Mockito.mock(GetAllDiskOfferingsResponse.class);
        Mockito.doReturn(getAllDiskOfferingsResponse)
                .when(this.plugin).getDiskOfferings(Mockito.eq(cloudStackUser));
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = new ArrayList<>();
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingNotMatch =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingNotMatch.getDiskSize()).thenReturn(diskExpected - 1);

        GetAllDiskOfferingsResponse.DiskOffering diskOfferingMatch =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingMatch.getDiskSize()).thenReturn(diskExpected);

        diskOfferings.add(diskOfferingNotMatch);
        diskOfferings.add(diskOfferingMatch);

        Mockito.doReturn(diskOfferings).when(getAllDiskOfferingsResponse).getDiskOfferings();

        // exercise
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                this.plugin.getDiskOffering(diskExpected, cloudStackUser);

        // verify
        Assert.assertEquals(diskOfferingMatch.getDiskSize(), diskOffering.getDiskSize());
    }

    // test case: When calling the getDiskOffering method with secondary methods mocked,
    // it must verify if it returns an exception because no disk offering match with disk required
    @Test
    public void testGetDiskOfferingFailWhenNotMatch() throws FogbowException {
        // set up
        int diskExpected = 1;
        int diskNotMatch = diskExpected - 1;
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse = Mockito.mock(GetAllDiskOfferingsResponse.class);
        Mockito.doReturn(getAllDiskOfferingsResponse)
                .when(this.plugin).getDiskOfferings(Mockito.eq(cloudStackUser));
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = Mockito.spy(new ArrayList<>());
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingNotMatchOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingNotMatchOne.getDiskSize()).thenReturn(diskNotMatch);

        GetAllDiskOfferingsResponse.DiskOffering diskOfferingNotMatchTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingNotMatchTwo.getDiskSize()).thenReturn(diskNotMatch);

        diskOfferings.add(diskOfferingNotMatchOne);
        diskOfferings.add(diskOfferingNotMatchTwo);

        Mockito.doReturn(diskOfferings).when(getAllDiskOfferingsResponse).getDiskOfferings();

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);
        this.expectedException.expectMessage(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK);

        // exercise
        this.plugin.getDiskOffering(diskExpected, cloudStackUser);
    }

    // test case: When calling the getDiskOffering method with secondary methods mocked,
    // it must verify if it returns an exception because there are not disks.
    @Test
    public void testGetDiskOfferingFailWhenThereAreNotDisck() throws FogbowException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

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

    // test case: When calling the doGet method with secondary methods mocked,
    // it must verify if It returns the response correct.
    @Test
    public void testDoGetSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        String url = "http://localhost";
        String responseStrExpeced = "response";

        Mockito.when(this.client.doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser)))
                .thenReturn(responseStrExpeced);

        // exercise
        String responseStr = this.plugin.doGet(url, cloudStackUser);

        // verify
        Assert.assertEquals(responseStrExpeced, responseStr);
    }

    // test case: calling the doGet method and it occurs a Exception,
    // it must verify if it was threw a HttpResponseException.
    @Test
    public void testDoGetFail() throws FogbowException, HttpResponseException {
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
        this.plugin.doGet(url, CloudstackTestUtils.CLOUD_STACK_USER);
    }

    // test case: When calling the requestDeployVirtualMachine method with secondary methods mocked,
    // it must verify if It returns the DeployVirtualMachineResponse correct.
    @Test
    public void testRequestDeployVirtualMachineSuccessfully() throws FogbowException, IOException {
        // set up
        DeployVirtualMachineRequest deployVirtualMachineRequest = new DeployVirtualMachineRequest.Builder()
                .build("");
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

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
                this.plugin.requestDeployVirtualMachine(deployVirtualMachineRequest, cloudStackUser);

        // verify
        Assert.assertEquals(deployVirtualMachineResponseExpected, deployVirtualMachineResponse);
    }

    // test case: calling the requestDeployVirtualMachine method and it occurs a Exception,
    // it must verify if it was threw a FogbowException.
    @Test
    public void testRequestDeployVirtualMachineFail() throws FogbowException, IOException {
        // set up
        DeployVirtualMachineRequest deployVirtualMachineRequest = new DeployVirtualMachineRequest.Builder()
                .build("anything");
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        Mockito.when(this.client.doGetRequest(
                Mockito.any(), Mockito.eq(cloudStackUser))).thenThrow(createBadRequestHttpResponse());

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestDeployVirtualMachine(deployVirtualMachineRequest, cloudStackUser);
    }

    // test case: When calling the updateComputeOrder method with order and new values,
    // it must verify if It updates the compute order.
    @Test
    public void testUpdateComputeOrderSuccessfully() {
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

    // test case: When calling the getTemplateId method,
    // it must verify if It returns the template id refering to the image id on the order
    @Test
    public void testGetTemplateIdSuccessfully() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();

        // exercise
        String templateId = this.plugin.getTemplateId(computeOrder);

        // verify
        Assert.assertEquals(TestUtils.FAKE_IMAGE_ID, templateId);
    }

    // test case: When calling the getTemplateId method with imageId is null in the order,
    // it must verify if It throws an InvalidParameterException.
    @Test
    public void testGetTemplateIdFailWhenIsNull() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn(null);

        // verify
        this.expectedException.expect(InvalidParameterException.class);

        // exercise
        this.plugin.getTemplateId(computeOrder);
    }

    // test case: When calling the getTemplateId method with imageId is empty in the order,
    // it must verify if It throws an InvalidParameterException.
    @Test
    public void testGetTemplateIdFailWhenIsEmpty() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn("");

        // verify
        this.expectedException.expect(InvalidParameterException.class);

        // exercise
        this.plugin.getTemplateId(computeOrder);
    }

    private ComputeOrder createComputeOrder(ArrayList<UserData> fakeUserData, String fakeImageId) {
        SystemUser requester = this.testUtils.createSystemUser();

        NetworkOrder networkOrder = this.testUtils.createLocalNetworkOrder();
        networkOrder.setId(TestUtils.FAKE_NETWORK_ID);
        List<String> networkOrderIds = Mockito.spy(new ArrayList<>());
        networkOrderIds.add(networkOrder.getId());

        ComputeOrder computeOrder = new ComputeOrder(requester, this.testUtils.FAKE_REMOTE_MEMBER_ID,
                this.testUtils.FAKE_REMOTE_MEMBER_ID, this.testUtils.DEFAULT_CLOUD_NAME,
                "", TestUtils.CPU_VALUE, TestUtils.MEMORY_VALUE,
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

    private HttpResponseException createUnexpectedHttpResponse() {
        return new HttpResponseException(HttpStatus.SC_CONTINUE, "");
    }

    private void ignoringCloudStackUrl() throws InvalidParameterException {
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();
    }

}
