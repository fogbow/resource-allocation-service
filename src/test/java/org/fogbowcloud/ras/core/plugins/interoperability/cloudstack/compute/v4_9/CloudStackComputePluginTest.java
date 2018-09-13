package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackComputePluginTest {

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_INSTANCE_NAME = "fake-name";
    public static final String FAKE_STATE = "Running";
    public static final String FAKE_CPU_NUMBER = "4";
    public static final String FAKE_MEMORY = "2024";
    public static final String FAKE_DISK = "25";
    public static final String FAKE_ADDRESS = "10.0.0.0/24";
    public static final String FAKE_NETWORK_ID = "fake-network-id";
    public static final String FAKE_TYPE = "ROOT";
    public static final String FAKE_EXPUNGE = "true";
    public static final String FAKE_MEMBER = "fake-member";
    public static final String FAKE_PUBLIC_KEY = "fake-member";

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";

    public static final CloudStackToken FAKE_TOKEN = new CloudStackToken(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE,
        FAKE_USER_ID, FAKE_USERNAME);

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

    private String fakeZoneId;

    private CloudStackComputePlugin plugin;
    private HttpRequestClientUtil client;

    private void initializeProperties() {
        String cloudStackConfFilePath = HomeDir.getPath() + File.separator
               + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.fakeZoneId = properties.getProperty(CloudStackComputePlugin.ZONE_ID_KEY);
    }

    @Before
    public void setUp() {
        initializeProperties();
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.plugin = new CloudStackComputePlugin();

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin.setClient(this.client);
    }

    // Test case: when deploying a virtual machine, the token should be signed and three HTTP GET requests should be made:
    // one to retrieve the service offerings from the cloudstack compute service, one to retrieve disk offerings
    // from the cloudstack volume service and one last request to the compute service to actually create the vm and return its id.
    @Test
    public void testRequestInstance() throws FogbowRasException, HttpResponseException, UnexpectedException, UnsupportedEncodingException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DeployVirtualMachineRequest.DEPLOY_VM_COMMAND;
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeImageId = "fake-image-id";

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);
        String fakeNetworkIdsString = "fake-default-network-id," + FAKE_NETWORK_ID;

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, computeCommand);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(TEMPLATE_ID_KEY, fakeImageId);
        expectedParams.put(SERVICE_OFFERING_ID_KEY, fakeServiceOfferingId);
        expectedParams.put(DISK_OFFERING_ID_KEY, fakeDiskOfferingId);
        expectedParams.put(USER_DATA_KEY, fakeUserDataString);
        expectedParams.put(NETWORK_IDS_KEY, fakeNetworkIdsString);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY));
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);
        String computeResponse = getDeployVirtualMachineResponse(FAKE_ID);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN))).thenReturn(computeResponse);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);
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
    public void testRequestInstanceNullRequiredParamater() throws FogbowRasException, HttpResponseException, UnexpectedException {
        // set up
        String fakeImageId = null;

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, never()).doGetRequest(Mockito.anyString(), Mockito.any(Token.class));
    }

    // Test case: fail to retrieve service offerings from cloudstack compute service on request instance
    @Test(expected = FogbowRasException.class)
    public void testRequestInstanceServiceOfferingRequestException() throws FogbowRasException, HttpResponseException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);


        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(FogbowRasException.class);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedServiceOfferingsRequestUrl, FAKE_TOKEN);
    }

    // Test case: when no mininum service offering is found to fulfill the order, raise exeception
    @Test(expected = FogbowRasException.class)
    public void testRequestInstanceServiceOfferingNotFound() throws FogbowRasException, HttpResponseException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        int lowerCpuNumber = 2;
        String fakeServiceOfferingId = "fake-service-offering-id";

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                lowerCpuNumber, Integer.parseInt(FAKE_MEMORY));

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedServiceOfferingsRequestUrl, FAKE_TOKEN);
    }

    // Test case: fail to retrieve disk offerings from cloudstack compute service on request instance
    @Test(expected = FogbowRasException.class)
    public void testRequestInstanceDiskOfferingRequestException() throws FogbowRasException, HttpResponseException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);


        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY));

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(FogbowRasException.class);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(2))
                .doGetRequest(expectedDiskOfferingsRequestUrl, FAKE_TOKEN);
    }

    // Test case: send deploy virtual machine request with no disk paramater in case no minimum disk offering is found
    // for the order
    @Test
    public void testRequestInstanceDiskOfferingNotFound() throws FogbowRasException, HttpResponseException, UnexpectedException, UnsupportedEncodingException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String endpoint = getBaseEndpointFromCloudStackConf();
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String computeCommand = DeployVirtualMachineRequest.DEPLOY_VM_COMMAND;

        String fakeImageId = "fake-image-id";
        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);

        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));

        String fakeNetworkIdsString = "fake-default-network-id," + FAKE_NETWORK_ID;

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, computeCommand);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(TEMPLATE_ID_KEY, fakeImageId);
        expectedParams.put(SERVICE_OFFERING_ID_KEY, fakeServiceOfferingId);
        expectedParams.put(USER_DATA_KEY, fakeUserDataString);
        expectedParams.put(NETWORK_IDS_KEY, fakeNetworkIdsString);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        int lowerDiskSize = 20;
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, lowerDiskSize,true);
        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY));
        String computeResponse = getDeployVirtualMachineResponse(FAKE_ID);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN))).thenReturn(computeResponse);

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);

        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedServiceOfferingsRequestUrl, FAKE_TOKEN);
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDiskOfferingsRequestUrl, FAKE_TOKEN);
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN));
    }

    // Test case: http request fails on attempting to deploy a new virtual machine
    @Test(expected = FogbowRasException.class)
    public void testRequestInstanceFail() throws FogbowRasException, HttpResponseException, UnexpectedException, UnsupportedEncodingException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DeployVirtualMachineRequest.DEPLOY_VM_COMMAND;
        String serviceOfferingsCommand = GetAllServiceOfferingsRequest.LIST_SERVICE_OFFERINGS_COMMAND;
        String diskOfferingsCommand = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String fakeImageId = "fake-image-id";

        UserData fakeUserData = new UserData("fakeuserdata", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
        String fakeUserDataString = Base64.getEncoder().encodeToString(
                fakeUserData.getExtraUserDataFileContent().getBytes("UTF-8"));

        List<String> fakeNetworkdIds = new ArrayList<>();
        fakeNetworkdIds.add(FAKE_NETWORK_ID);
        String fakeNetworkIdsString = "fake-default-network-id," + FAKE_NETWORK_ID;

        String expectedServiceOfferingsRequestUrl = generateExpectedUrl(endpoint, serviceOfferingsCommand);
        String expectedDiskOfferingsRequestUrl = generateExpectedUrl(endpoint, diskOfferingsCommand);

        String fakeServiceOfferingId = "fake-service-offering-id";
        String fakeDiskOfferingId = "fake-disk-offering-id";

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, computeCommand);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(TEMPLATE_ID_KEY, fakeImageId);
        expectedParams.put(SERVICE_OFFERING_ID_KEY, fakeServiceOfferingId);
        expectedParams.put(DISK_OFFERING_ID_KEY, fakeDiskOfferingId);
        expectedParams.put(USER_DATA_KEY, fakeUserDataString);
        expectedParams.put(NETWORK_IDS_KEY, fakeNetworkIdsString);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        String serviceOfferingResponse = getListServiceOfferrings(fakeServiceOfferingId, "fake-service-offering",
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY));
        String diskOfferingResponse = getListDiskOfferrings(fakeDiskOfferingId, Integer.parseInt(FAKE_DISK),true);
        String computeResponse = getDeployVirtualMachineResponse(FAKE_ID);

        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedServiceOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(serviceOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDiskOfferingsRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(diskOfferingResponse);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN)))
                .thenThrow(new HttpResponseException(503, "service unavailable"));

        // exercise
        ComputeOrder order = new ComputeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_INSTANCE_NAME,
                Integer.parseInt(FAKE_CPU_NUMBER), Integer.parseInt(FAKE_MEMORY),
                Integer.parseInt(FAKE_DISK), fakeImageId, fakeUserData, FAKE_PUBLIC_KEY, fakeNetworkdIds);
        String createdVirtualMachineId = this.plugin.requestInstance(order, FAKE_TOKEN);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(3));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(FAKE_TOKEN));
    }

    // Test case: when getting a virtual machine, the token should be signed and two HTTP GET requests should be made:
    // one to retrieve the virtual machine from the cloudstack compute service and another to retrieve that vm disk
    // size from the cloudstack volume service. Finally, a valid compute instance should be returned from those
    // requests results.
    @Test
    public void testGetInstance() throws UnexpectedException, FogbowRasException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String volumeCommand = GetVolumeRequest.LIST_VOLUMES_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand, ID_KEY, FAKE_ID);
        String expectedVolumeRequestUrl = generateExpectedUrl(endpoint, volumeCommand,
                                                              VIRTUAL_MACHINE_ID_KEY, FAKE_ID,
                                                              TYPE_KEY, FAKE_TYPE);

        String successfulComputeResponse = getVirtualMachineResponse(FAKE_ID, FAKE_INSTANCE_NAME, FAKE_STATE,
                                                                     FAKE_CPU_NUMBER, FAKE_MEMORY,
                                                                     FAKE_ADDRESS);
        String volumeResponse = getVolumeResponse(FAKE_ID, FAKE_INSTANCE_NAME, FAKE_DISK, FAKE_STATE);
        String successfulVolumeResponse = getListVolumesResponse(volumeResponse);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn(successfulComputeResponse);
        Mockito.when(this.client.doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN)).thenReturn(successfulVolumeResponse);

        // exercise
        ComputeInstance retrievedInstance = this.plugin.getInstance(FAKE_ID, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, retrievedInstance.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, retrievedInstance.getHostName());
        Assert.assertEquals("READY", retrievedInstance.getState().name());
        Assert.assertEquals(FAKE_CPU_NUMBER, String.valueOf(retrievedInstance.getvCPU()));
        Assert.assertEquals(FAKE_MEMORY, String.valueOf(retrievedInstance.getRam()));
        Assert.assertEquals(FAKE_DISK, String.valueOf(retrievedInstance.getDisk()));
        Assert.assertEquals(FAKE_ADDRESS, retrievedInstance.getLocalIpAddress());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN);
    }

    // Test case: when getting a virtual machine which root disk size could not be retrieved, default volume size to -1
    @Test
    public void testGetInstanceNoVolume() throws FogbowRasException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String volumeCommand = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        String errorDiskSize = "-1";

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand, ID_KEY, FAKE_ID);
        String expectedVolumeRequestUrl = generateExpectedUrl(endpoint, volumeCommand,
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

        // exercise http request failed
        ComputeInstance retrievedInstance = this.plugin.getInstance(FAKE_ID, FAKE_TOKEN);

        Assert.assertEquals(FAKE_ID, retrievedInstance.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, retrievedInstance.getHostName());
        Assert.assertEquals("READY", retrievedInstance.getState().name());
        Assert.assertEquals(FAKE_CPU_NUMBER, String.valueOf(retrievedInstance.getvCPU()));
        Assert.assertEquals(FAKE_MEMORY, String.valueOf(retrievedInstance.getRam()));
        Assert.assertEquals(errorDiskSize, String.valueOf(retrievedInstance.getDisk()));
        Assert.assertEquals(FAKE_ADDRESS, retrievedInstance.getLocalIpAddress());

        // exercise empty response from volume request
        ComputeInstance retrievedInstance2 = this.plugin.getInstance(FAKE_ID, FAKE_TOKEN);

        Assert.assertEquals(FAKE_ID, retrievedInstance2.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, retrievedInstance2.getHostName());
        Assert.assertEquals("READY", retrievedInstance2.getState().name());
        Assert.assertEquals(FAKE_CPU_NUMBER, String.valueOf(retrievedInstance2.getvCPU()));
        Assert.assertEquals(FAKE_MEMORY, String.valueOf(retrievedInstance2.getRam()));
        Assert.assertEquals(errorDiskSize, String.valueOf(retrievedInstance2.getDisk()));
        Assert.assertEquals(FAKE_ADDRESS, retrievedInstance2.getLocalIpAddress());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(4));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(2)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
        Mockito.verify(this.client, Mockito.times(2)).doGetRequest(expectedVolumeRequestUrl, FAKE_TOKEN);
    }

    // Test case: instance not found
    @Test(expected = InstanceNotFoundException.class)
    public void getInstanceNotFound() throws FogbowRasException, HttpResponseException {
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = GetVirtualMachineRequest.LIST_VMS_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand, ID_KEY, FAKE_ID);
        String emptyComputeResponse = getVirtualMachineResponse();

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn(emptyComputeResponse);

        ComputeInstance retrievedInstance = this.plugin.getInstance(FAKE_ID, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }


    // Test case: deleting an instance
    @Test
    public void deleteInstance() throws UnexpectedException, FogbowRasException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DestroyVirtualMachineRequest.DESTROY_VIRTUAL_MACHINE_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                ID_KEY, FAKE_ID,
                EXPUNGE_KEY, FAKE_EXPUNGE);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn("");

        // exercise
        this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }

    // Test case: failing to delete an instance
    @Test(expected = FogbowRasException.class)
    public void deleteInstanceFail() throws UnexpectedException, FogbowRasException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String computeCommand = DestroyVirtualMachineRequest.DESTROY_VIRTUAL_MACHINE_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, computeCommand,
                ID_KEY, FAKE_ID,
                EXPUNGE_KEY, FAKE_EXPUNGE);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenThrow(
                new HttpResponseException(503, "service unavailable"));

        // exercise
        this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }


    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(CLOUDSTACK_URL);
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

    private String getListServiceOfferrings(String id, String name, int cpuNumber, int memory) {
        String response = "{\"listserviceofferingsresponse\":{" + "\"serviceoffering\":[{"
                + "\"id\": \"%s\","
                + "\"name\": \"%s\","
                + "\"cpunumber\": \"%s\","
                + "\"memory\": \"%s\""
                + "}]}}";

        return String.format(response, id, name, cpuNumber, memory);
    }

    private String getDeployVirtualMachineResponse(String id) {
        String response = "{\"jobresult\":{\"virtualmachine\":{\"id\":\"%s\"}}}";

        return String.format(response, id);
    }
}
