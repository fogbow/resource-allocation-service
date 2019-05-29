package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import cloud.fogbow.common.util.CloudInitUserDataBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.mockito.Mockito.never;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, CloudStackUrlUtil.class, DefaultLaunchCommandGenerator.class})
public class CloudStackComputePluginTest {

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
    public static final String COMMAND_KEY = "command";
    public static final String ZONE_ID_KEY = "zoneid";
    public static final String SERVICE_OFFERING_ID_KEY = "serviceofferingid";
    public static final String TEMPLATE_ID_KEY = "templateid";
    public static final String DISK_OFFERING_ID_KEY = "diskofferingid";
    public static final String NETWORK_IDS_KEY = "networkids";
    public static final String USER_DATA_KEY = "userdata";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String CLOUD_NAME = "cloudstack";

    private String fakeZoneId;

    private CloudStackComputePlugin plugin;
    private CloudStackHttpClient client;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private Properties properties;
    private String defaultNetworkId;
    private SharedOrderHolders sharedOrderHolders;

    @Before
    public void setUp() {
        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.defaultNetworkId = this.properties.getProperty(CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = new CloudStackComputePlugin(cloudStackConfFilePath);
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

    // Test case: when deploying virtual machine, the token should be signed and five HTTP GET requests should be made:
    // 1) retrieve the service offerings from the cloudstack compute service; 2) retrieve disk offerings
    // from the cloudstack volume service; 3) register ssh keypair using public key passed in the order; // 4) request
    // to the compute service to actually create the vm; 5) delete keypair used to created the vm.
    @Test
    public void testRequestInstance() throws FogbowException, HttpResponseException, UnsupportedEncodingException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DeployVirtualMachineRequest.DEPLOY_VM_COMMAND;
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeImageId = "fake-image-id";

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));
        ArrayList<UserData> userData = new ArrayList<>();
        userData.add(fakeUserData);

        String fakeNetworkIdsString = this.defaultNetworkId + "," + FAKE_NETWORK_ID;

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, computeCommand);
        expectedParams.put(RESPONSE_KEY, JSON);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(TEMPLATE_ID_KEY, fakeImageId);
        expectedParams.put(SERVICE_OFFERING_ID_KEY, fakeServiceOfferingId);
        expectedParams.put(DISK_OFFERING_ID_KEY, fakeDiskOfferingId);
        expectedParams.put(USER_DATA_KEY, fakeUserDataString);
        expectedParams.put(NETWORK_IDS_KEY, fakeNetworkIdsString);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);
        String computeResponse = getDeployVirtualMachineResponse(FAKE_ID);

        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class))).thenReturn(fakeUserDataString);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN))).thenReturn(computeResponse);

        // exercise
        ComputeOrder order = createComputeOrder(userData, fakeImageId);
        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, createdVirtualMachineId);

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(3));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(FAKE_TOKEN));
    }

    // test case: when order has requirements, filter out non-matching service offerings
    @Test
    public void testRequestInstanceWithRequirements() throws FogbowException, HttpResponseException, UnsupportedEncodingException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DeployVirtualMachineRequest.DEPLOY_VM_COMMAND;
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeImageId = "fake-image-id";

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));
        ArrayList<UserData> userData = new ArrayList<>();
        userData.add(fakeUserData);

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);
        String fakeNetworkIdsString = this.defaultNetworkId + "," + FAKE_NETWORK_ID;

        Map<String, String> fakeRequirements = new HashMap<>();
        fakeRequirements.put("tag1", "value1");

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, computeCommand);
        expectedParams.put(RESPONSE_KEY, JSON);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(TEMPLATE_ID_KEY, fakeImageId);
        expectedParams.put(SERVICE_OFFERING_ID_KEY, fakeServiceOfferingId);
        expectedParams.put(DISK_OFFERING_ID_KEY, fakeDiskOfferingId);
        expectedParams.put(USER_DATA_KEY, fakeUserDataString);
        expectedParams.put(NETWORK_IDS_KEY, fakeNetworkIdsString);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);
        String computeResponse = getDeployVirtualMachineResponse(FAKE_ID);

        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class))).thenReturn(fakeUserDataString);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN))).thenReturn(computeResponse);

        // exercise
        ComputeOrder order = createComputeOrder(userData, fakeImageId);
        order.setRequirements(fakeRequirements);
        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, createdVirtualMachineId);

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(3));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(FAKE_TOKEN));
    }

    // Test case: template, zone and default network ids are required paramaters for requesting an instance, so they
    // should be appropriately defined in the config file or in the order. raise exception otherwise.
    @Test(expected = InvalidParameterException.class)
    public void testRequestInstanceNullRequiredParamater() throws FogbowException, HttpResponseException {
        // set up
        String fakeImageId = null;

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CLOUD_NAME, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, userData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, never()).doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class));
    }

    // Test case: fail to retrieve service offerings from cloudstack compute service on request instance
    @Test(expected = FogbowException.class)
    public void testRequestInstanceServiceOfferingRequestException() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(FogbowException.class);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CLOUD_NAME, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, userData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedServiceOfferingsRequestUrl, FAKE_TOKEN);
    }

    // test case: if no service offering is found for order with requirements, raise exception
    @Test(expected = FogbowException.class)
    public void testRequestInstanceNoMatchingRequirements() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        Map<String, String> fakeRequirements = new HashMap<>();
        fakeRequirements.put("tag3", "value3");

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);
        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_CLOUD_NAME, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, userData, FAKE_PUBLIC_KEY, fakeNetworkdIds);
        order.setRequirements(fakeRequirements);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedServiceOfferingsRequestUrl, FAKE_TOKEN);
    }

    // Test case: when no mininum service offering is found to fulfill the order, raise exception
    @Test(expected = FogbowException.class)
    public void testRequestInstanceServiceOfferingNotFound() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        int lowerCpuNumber = 2;
        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                lowerCpuNumber, Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CLOUD_NAME, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, userData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedServiceOfferingsRequestUrl, FAKE_TOKEN);
    }

    // Test case: raise exception on fail to retrieve disk offerings from cloudstack compute service on request instance
    @Test(expected = FogbowException.class)
    public void testRequestInstanceDiskOfferingRequestException() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);


        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(FogbowException.class);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, CLOUD_NAME, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, userData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(2))
                .doGetRequest(expectedDiskOfferingsRequestUrl, FAKE_TOKEN);
    }

    // Test case: raises exception in case no minimum disk offering is found for the order
    @Test(expected = NoAvailableResourcesException.class)
    public void testRequestInstanceDiskOfferingNotFound() throws FogbowException, HttpResponseException, UnsupportedEncodingException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        int lowerDiskSize = 20;
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, lowerDiskSize,true);
        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);
        String computeResponse = getDeployVirtualMachineResponse(FAKE_ID);

        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class))).thenReturn(fakeUserDataString);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);

        // exercise
        ComputeOrder order = createComputeOrder(userData, fakeImageId);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);
    }

    // Test case: http request fails on attempting to deploy a new virtual machine
    @Test(expected = FogbowException.class)
    public void testRequestInstanceFail() throws FogbowException, HttpResponseException, UnsupportedEncodingException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DeployVirtualMachineRequest.DEPLOY_VM_COMMAND;
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeImageId = "fake-image-id";

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag");
        ArrayList<UserData> userData = new ArrayList<UserData>(Arrays.asList(new UserData[] { fakeUserData }));

        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);
        String fakeNetworkIdsString = this.defaultNetworkId + "," + FAKE_NETWORK_ID;

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand,
                RESPONSE_KEY, JSON);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand,
                RESPONSE_KEY, JSON);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, computeCommand);
        expectedParams.put(RESPONSE_KEY, JSON);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(TEMPLATE_ID_KEY, fakeImageId);
        expectedParams.put(SERVICE_OFFERING_ID_KEY, fakeServiceOfferingId);
        expectedParams.put(DISK_OFFERING_ID_KEY, fakeDiskOfferingId);
        expectedParams.put(USER_DATA_KEY, fakeUserDataString);
        expectedParams.put(NETWORK_IDS_KEY, fakeNetworkIdsString);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY), FAKE_TAGS);
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);

        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any(ComputeOrder.class))).thenReturn(fakeUserDataString);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(new HttpResponseException(503, "service unavailable"));

        // exercise
        ComputeOrder order = createComputeOrder(userData, fakeImageId);
        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(3));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(FAKE_TOKEN));
    }

    // Test case: when getting virtual machine, the token should be signed and two HTTP GET requests should be made:
    // one to retrieve the virtual machine from the cloudstack compute service and another to retrieve that vm disk
    // size from the cloudstack volume service. Finally, valid compute instance should be returned from those
    // requests results.
    @Test
    public void testGetInstance() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String volumeCommand = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        List<String> ipAddresses =  new ArrayList<>();
        ipAddresses.add(FAKE_ADDRESS);

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_ID);
        String expectedVolumeRequestUrl = generateExpectedUrl(endpoint, volumeCommand,
                                                              RESPONSE_KEY, JSON,
                                                              VIRTUAL_MACHINE_ID_KEY, FAKE_ID,
                                                              TYPE_KEY, FAKE_TYPE);

        String successfulComputeResponse = getVirtualMachineResponse(FAKE_ID, FAKE_INSTANCE_NAME, FAKE_STATE,
                                                                     FAKE_CPU_NUMBER, FAKE_MEMORY,
                                                                     FAKE_ADDRESS);

        double value = Integer.valueOf(FAKE_DISK) * Math.pow(1024, 3);
        String fakeDiskInBytes = new Double(value).toString();
        String volumeResponse = getVolumeResponse(FAKE_ID, FAKE_INSTANCE_NAME, fakeDiskInBytes, FAKE_STATE);
        String successfulVolumeResponse = getListVolumesResponse(volumeResponse);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn(successfulComputeResponse);
        Mockito.when(this.client.doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN)).thenReturn(successfulVolumeResponse);

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        ComputeInstance retrievedInstance = this.plugin.getInstance(computeOrder, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, retrievedInstance.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, retrievedInstance.getName());
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, retrievedInstance.getCloudState());
        Assert.assertEquals(FAKE_CPU_NUMBER, String.valueOf(retrievedInstance.getvCPU()));
        Assert.assertEquals(FAKE_MEMORY, String.valueOf(retrievedInstance.getMemory()));
        Assert.assertEquals(FAKE_DISK, String.valueOf(retrievedInstance.getDisk()));
        Assert.assertEquals(ipAddresses, retrievedInstance.getIpAddresses());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN);
    }

    // Test case: when getting virtual machine which root disk size could not be retrieved, default volume size to -1
    @Test
    public void testGetInstanceNoVolume() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String volumeCommand = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        String errorDiskSize = "-1";
        List<String> ipAddresses =  new ArrayList<>();
        ipAddresses.add(FAKE_ADDRESS);

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_ID);
        String expectedVolumeRequestUrl = generateExpectedUrl(endpoint, volumeCommand,
                RESPONSE_KEY, JSON,
                VIRTUAL_MACHINE_ID_KEY, FAKE_ID,
                TYPE_KEY, FAKE_TYPE);

        String successfulComputeResponse = getVirtualMachineResponse(FAKE_ID, FAKE_INSTANCE_NAME, FAKE_STATE,
                FAKE_CPU_NUMBER, FAKE_MEMORY,
                FAKE_ADDRESS);
        String emptyVolumeResponse = getListVolumesResponse();

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn(successfulComputeResponse);
        Mockito.when(this.client.doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN))
                .thenThrow(new HttpResponseException(503, "service unavailable")) // http request failed
                .thenReturn(emptyVolumeResponse); // no volume found with this vm id

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        ComputeInstance retrievedInstance = this.plugin.getInstance(computeOrder, FAKE_TOKEN);

        Assert.assertEquals(FAKE_ID, retrievedInstance.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, retrievedInstance.getName());
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, retrievedInstance.getCloudState());
        Assert.assertEquals(FAKE_CPU_NUMBER, String.valueOf(retrievedInstance.getvCPU()));
        Assert.assertEquals(FAKE_MEMORY, String.valueOf(retrievedInstance.getMemory()));
        Assert.assertEquals(errorDiskSize, String.valueOf(retrievedInstance.getDisk()));
        Assert.assertEquals(ipAddresses, retrievedInstance.getIpAddresses());

        ComputeOrder computeOrder2 = new ComputeOrder();
        computeOrder2.setInstanceId(FAKE_ID);

        // exercise
        ComputeInstance retrievedInstance2 = this.plugin.getInstance(computeOrder, FAKE_TOKEN);

        Assert.assertEquals(FAKE_ID, retrievedInstance2.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, retrievedInstance2.getName());
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, retrievedInstance2.getCloudState());
        Assert.assertEquals(FAKE_CPU_NUMBER, String.valueOf(retrievedInstance2.getvCPU()));
        Assert.assertEquals(FAKE_MEMORY, String.valueOf(retrievedInstance2.getMemory()));
        Assert.assertEquals(errorDiskSize, String.valueOf(retrievedInstance2.getDisk()));
        Assert.assertEquals(ipAddresses, retrievedInstance2.getIpAddresses());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(4));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(2)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
        Mockito.verify(this.client, Mockito.times(2)).doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN);
    }

    // Test case: instance not found
    @Test(expected = InstanceNotFoundException.class)
    public void getInstanceNotFound() throws FogbowException, HttpResponseException {
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = GetVirtualMachineRequest.LIST_VMS_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_ID);
        String emptyComputeResponse = getVirtualMachineResponse();

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn(emptyComputeResponse);

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        ComputeInstance retrievedInstance = this.plugin.getInstance(computeOrder, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }


    // Test case: deleting an instance
    @Test
    public void deleteInstance() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DestroyVirtualMachineRequest.DESTROY_VIRTUAL_MACHINE_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_ID,
                EXPUNGE_KEY, FAKE_EXPUNGE);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn("");

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        this.plugin.deleteInstance(computeOrder, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }

    // Test case: failing to delete an instance
    @Test(expected = FogbowException.class)
    public void deleteInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DestroyVirtualMachineRequest.DESTROY_VIRTUAL_MACHINE_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_ID,
                EXPUNGE_KEY, FAKE_EXPUNGE);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenThrow(
                new HttpResponseException(503, "service unavailable"));

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(FAKE_ID);

        // exercise
        this.plugin.deleteInstance(computeOrder, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }

    private String getBaseEndpointFromCloudStackConf() {
        return this.properties.getProperty(CLOUDSTACK_URL);
    }

    private String generateExpectedUrl(String endpoint, String command, String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            // there should be one value for each key
            return null;
        }

        String url = String.format("%s?command=%s", endpoint, command);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            url += String.format("&%s=%s", key, value);
        }

        return url;
    }

    private String getVirtualMachineResponse(String id, String name, String state,
                                             String cpunumber, String memory, String ipaddress) {
        String format = "{\"listvirtualmachinesresponse\":{\"count\":1" +
                ",\"virtualmachine\":[" +
                "{\"id\":\"%s\"" +
                ",\"name\":\"%s\"" +
                ",\"state\":\"%s\"" +
                ",\"cpunumber\":\"%s\"" +
                ",\"memory\":\"%s\"" +
                ",\"nic\":[" +
                "{\"ipaddress\":\"%s\"" +
                "}]}]}}";

        return String.format(format, id, name, state, cpunumber, memory, ipaddress);
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

    private String getListDiskOfferrings(String id, int diskSize, boolean customized) {
        String response = "{\"listdiskofferingsresponse\":{" + "\"diskoffering\":[{"
                + "\"id\": \"%s\","
                + "\"disksize\": %s,"
                + "\"iscustomized\": %s"
                + "}]}}";

        return String.format(response, id, diskSize, customized);
    }

    private String getListServiceOfferrings(String id, String name, int cpuNumber, int memory, String tags) {
        String response = "{\"listserviceofferingsresponse\":{" + "\"serviceoffering\":[{"
                + "\"id\": \"%s\","
                + "\"name\": \"%s\","
                + "\"cpunumber\": \"%s\","
                + "\"memory\": \"%s\","
                + "\"tags\": \"%s\""
                + "}]}}";

        return String.format(response, id, name, cpuNumber, memory, tags);
    }

    private String getDeployVirtualMachineResponse(String id) {
        String response = "{\"deployvirtualmachineresponse\":{\"id\":\"%s\"}}";

        return String.format(response, id);
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
}
