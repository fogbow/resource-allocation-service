package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.HttpErrorConditionToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.*;

@PrepareForTest({DatabaseManager.class, GetFlavorResponse.class,
        PropertiesUtil.class, HttpErrorConditionToFogbowExceptionMapper.class,
        OpenStackPluginUtils.class, GetFlavorExtraSpecsResponse.class,
        GetAllFlavorsResponse.class})
public class OpenStackComputePluginTest extends BaseUnitTests {

    private OpenStackComputePlugin computePlugin;
    private OpenStackV3User cloudUser;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private OpenStackHttpClient clientMock;
    private Properties propertiesMock;
    private final String defaultNetworkId = "fake-default-network-id";
    private final String publicKey = "public-key";
    private final String bestFlavorId = "best-flavor";

    private static final int FAKE_FLAVOR_DISK = 80;
    private static final int FAKE_FLAVOR_MEMORY = 16;
    private static final int FAKE_FLAVOR_VCPU = 4;

    private static final String ANY_URL = "http://localhost:8008";
    private static final String ANY_STRING = "any-string";
    private static final String FAKE_CONF_FILE_PATH = "fake-conf-file-path";
    private static final String FAKE_FLAVOR_ID = "fake-flavor-id";
    private static final String FAKE_FLAVOR_NAME = "fake-flavor-name";
    private static final String FAKE_KEY_NAME = "fake-key-name";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";

    private static final String FAKE_REQUIREMENT = "fake-key-1";
    private static final String FAKE_UNMET_REQUIREMENT = "this-requiremnt-isnot-going-to-be-fullfiled";
    private static final String MAP_METHOD = "map";

    private final String privateNetworkId = "fake-private-network-id";
    private final String userData = "userDataFromLauchCommand";
    private final List<String> networksId = new ArrayList<String>();
    private final String instanceId = "compute-instance-id";
    private String flavorId = "flavorId";
    private final String computeNovaV2UrlKey = "compute-nova-v2-url-key";

    @Before
    public void setUp() throws Exception {
        testUtils.mockReadOrdersFromDataBase();

        this.propertiesMock = getPropertiesMock();
        this.clientMock = Mockito.mock(OpenStackHttpClient.class);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.networksId.add(privateNetworkId);

        PowerMockito.mockStatic(PropertiesUtil.class);
        BDDMockito.given(PropertiesUtil.readProperties(Mockito.eq(FAKE_CONF_FILE_PATH)))
                .willReturn(this.propertiesMock);

        this.computePlugin = Mockito.spy(new OpenStackComputePlugin(FAKE_CONF_FILE_PATH));

        this.computePlugin.setClient(this.clientMock);
        this.computePlugin.setLaunchCommandGenerator(this.launchCommandGeneratorMock);

        this.cloudUser = new OpenStackV3User(TestUtils.FAKE_USER_ID, TestUtils.FAKE_USER_NAME,
                this.FAKE_TOKEN_VALUE, this.FAKE_PROJECT_ID);
    }

    // test case: when given an order, it should return a flavor with it's resources
    // greater than or equal the requested
    @Test
    public void testGetBestFlavor() throws FogbowException {
        // setup
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        Mockito.doNothing().when(this.computePlugin)
                .updateFlavors(Mockito.eq(computeOrder), Mockito.eq(this.cloudUser));

        Mockito.doReturn(getHardwareRequirementsList()).when(this.computePlugin)
                .getHardwareRequirementsList();

        // exercise
        HardwareRequirements requirements = this.computePlugin.getBestFlavor(computeOrder, cloudUser);

        // verify
        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .updateFlavors(Mockito.eq(computeOrder), Mockito.eq(this.cloudUser));
        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getHardwareRequirementsList();
        Assert.assertTrue(testUtils.DISK_VALUE <= requirements.getDisk());
        Assert.assertTrue(testUtils.CPU_VALUE <= requirements.getCpu());
        Assert.assertTrue(testUtils.MEMORY_VALUE <= requirements.getRam());
    }

    // test case: when given and order with huge resources it should not be capable
    // of allocating any flavor
    @Test
    public void testGetBestFlavorMismatch() throws FogbowException {
        // setup
        int bigVcpu = 9999;
        int bigMemory = 99999999;
        int bigDisk = 99999999;
        ComputeOrder computeOrder = new ComputeOrder(testUtils.FAKE_USER_ID, testUtils.createSystemUser(), testUtils.FAKE_REMOTE_MEMBER_ID, testUtils.LOCAL_MEMBER_ID,
                testUtils.DEFAULT_CLOUD_NAME, testUtils.FAKE_INSTANCE_NAME, bigVcpu, bigMemory, bigDisk, testUtils.FAKE_IMAGE_ID,
                testUtils.mockUserData(), publicKey, null);

        Mockito.doNothing().when(this.computePlugin)
                .updateFlavors(Mockito.eq(computeOrder), Mockito.eq(this.cloudUser));

        Mockito.doReturn(getHardwareRequirementsList()).when(this.computePlugin)
                .getHardwareRequirementsList();

        // exercise
        HardwareRequirements requirements = this.computePlugin.getBestFlavor(computeOrder, cloudUser);

        // verify
        Assert.assertNull(requirements);
    }

    // test case: when an order is given, return any hardwareRequirements
    // that suits the order's needs
    @Test
    public void testFindSmallestFlavor() throws FogbowException {
        // setup
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        HardwareRequirements bestRequirements = getHardwareRequirementsList().last();
        Mockito.doReturn(bestRequirements).when(this.computePlugin)
                .getBestFlavor(Mockito.eq(computeOrder), Mockito.eq(this.cloudUser));

        // exercise
        HardwareRequirements actualRequirements = this.computePlugin.findSmallestFlavor(computeOrder, cloudUser);

        // verify
        Assert.assertEquals(bestRequirements, actualRequirements);
        Mockito.verify(this.computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getBestFlavor(Mockito.eq(computeOrder), Mockito.eq(this.cloudUser));
    }

    // test case: when a order is given, no flavor will match the order so a UnacceptableOperationException
    // will be thrown
    @Test(expected = UnacceptableOperationException.class)
    public void testFindSmallestFlavorWhenNoResourceAvailable() throws FogbowException {
        // setup
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        Mockito.doReturn(null).when(this.computePlugin)
                .getBestFlavor(Mockito.eq(computeOrder), Mockito.eq(this.cloudUser));

        // exercise
        this.computePlugin.findSmallestFlavor(computeOrder, cloudUser);
    }

    // test case: test if works correctly by calling doCreateKeyName()
    @Test
    public void testGetKeyName() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doNothing().when(this.computePlugin)
                .doCreateKeyName(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));

        // exercise
        this.computePlugin.getKeyName(this.FAKE_PROJECT_ID, publicKey, cloudUser);

        // verify
        Mockito.verify(this.computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .doCreateKeyName(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));
    }

    // test case: when calling doCreateKeyName() method, it must verify that the
    // call was successful.
    @Test
    public void testDoCreateKeyNameSuccessfully() throws Exception {
        // exercise
        this.computePlugin.doCreateKeyName(ANY_URL, ANY_STRING, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(TestUtils.RUN_ONCE))
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    // test case: when doCreateKeyName() request fails, it must
    @Test
    public void testDoCreateKeyNameUnsuccessfully() throws Exception {
        // set up
        InternalServerErrorException exception = testUtils.getInternalServerErrorException();
        Mockito.when(this.clientMock.doPostRequest(Mockito.anyString(),
                Mockito.anyString(), Mockito.eq(cloudUser))).thenThrow(exception);

        // exercise
        try {
            this.computePlugin.doCreateKeyName(ANY_URL, ANY_STRING, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    // test case: test if deleteKeyName() will call doDeleteRequest passing the keyName
    @Test
    public void testDeleteKeyName() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        // exercise
        this.computePlugin.deleteKeyName(FAKE_PROJECT_ID, ANY_STRING, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(testUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(cloudUser));
    }

    // test case: when calling doDeleteRequest() method, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteSuccessfully() throws Exception {
        // exercise
        this.computePlugin.doDeleteRequest(ANY_URL, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.any());
    }

    // test case: when doDeleteRequest() request fails, it must
    // call HttpErrorConditionToFogbowExceptionMapper.map(e)
    @Test
    public void testDoDeleteUnsuccessfully() throws Exception {
        // set up
        InternalServerErrorException exception = testUtils.getInternalServerErrorException();
        Mockito.doThrow(exception).when(this.clientMock)
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        // exercise
        try {
            this.computePlugin.doDeleteRequest(ANY_URL, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    // test case: test if getRequestBody() will return an CreateComputeRequest instance
    @Test
    public void testGetRequestBody() {
        // set up
        CreateComputeRequest requestMock = Mockito.mock(CreateComputeRequest.class);
        Mockito.doReturn(requestMock).when(this.computePlugin)
                .getCreateComputeRequest(Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.any(), Mockito.any());
        List<String> networksIds = getMockedNetworkIds();

        //exercise
        CreateComputeRequest createComputeRequest = this.computePlugin.getRequestBody(
                FAKE_INSTANCE_NAME, testUtils.FAKE_IMAGE_ID, flavorId, userData, ANY_STRING, networksIds);

        // verify
        Mockito.verify(this.computePlugin, Mockito.times(TestUtils.RUN_ONCE))
                .getCreateComputeRequest(Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.any(), Mockito.any());
    }

    // test case: When given a list of Flavor, check if it does have them cached, else
    // it should perform a request for those flavors.
    @Test
    public void testDetailFlavors() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(getHardwareRequirementsList()).when(this.computePlugin)
                .getHardwareRequirementsList();
        List<String> flavorIds = getFlavorIdsFromHardwareRequirementsList();
        String unCachedFlavorId = "uncached-flavor-id";
        flavorIds   .add(unCachedFlavorId);

        HardwareRequirements requirementsMock = Mockito.mock(HardwareRequirements.class);
        Mockito.doReturn(requirementsMock).when(this.computePlugin)
                .fetchHardwareRequirements(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));

        // exercise
        this.computePlugin.detailFlavors(ANY_URL, flavorIds, cloudUser);

        // verify
        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getHardwareRequirementsList();

        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .fetchHardwareRequirements(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));
    }

    @Test
    public void testFetchHardwareRequirements() throws FogbowException, HttpResponseException {
        // set up
        GetFlavorResponse mockedResponse = createGetFlavorResponseMock();
        PowerMockito.mockStatic(GetFlavorResponse.class);
        BDDMockito.given(GetFlavorResponse.fromJson(Mockito.anyString()))
                .willReturn(mockedResponse);

        Mockito.doReturn(ANY_STRING).when(this.computePlugin)
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        // exercise
        this.computePlugin.fetchHardwareRequirements(ANY_URL, ANY_STRING, cloudUser);

        // verify
        Mockito.verify(this.computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetFlavorResponse.class);
        GetFlavorResponse.fromJson(Mockito.anyString());
    }

    private GetFlavorResponse createGetFlavorResponseMock() {
        GetFlavorResponse response = Mockito.mock(GetFlavorResponse.class);
        Mockito.when(response.getName()).thenReturn(FAKE_FLAVOR_NAME);
        Mockito.when(response.getId()).thenReturn(FAKE_FLAVOR_ID);
        Mockito.when(response.getVcpusCount()).thenReturn(FAKE_FLAVOR_VCPU);
        Mockito.when(response.getMemory()).thenReturn(FAKE_FLAVOR_MEMORY);
        Mockito.when(response.getDisk()).thenReturn(FAKE_FLAVOR_DISK);

        return response;
    }

    // test case: When performing an unsuccessful request, it must throw an InternalServerErrorException
    @Test(expected = InternalServerErrorException.class)
    public void testDetailFlavorsUnsuccessfully() throws FogbowException {
        // set up
        Mockito.doReturn(getHardwareRequirementsList()).when(this.computePlugin)
                .getHardwareRequirementsList();
        List<String> flavorIds = getFlavorIdsFromHardwareRequirementsList();
        String unCachedFlavorId = "uncached-flavor-id";
        flavorIds.add(unCachedFlavorId);

        Mockito.when(this.clientMock.doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser)))
                .thenThrow(InternalServerErrorException.class);

        // exercise
        this.computePlugin.detailFlavors(ANY_URL, flavorIds, cloudUser);

        Assert.fail();
    }

    @Test
    public void getInstanceFromJson() {
        // set up
        String getRawResponse = createGetComputeResponseJson(testUtils.FAKE_INSTANCE_ID, FAKE_INSTANCE_NAME);

        // exercise
        ComputeInstance computeInstance = this.computePlugin.getInstanceFromJson(getRawResponse);

        // verify
        Assert.assertEquals(testUtils.FAKE_INSTANCE_ID, computeInstance.getId());
        Assert.assertEquals(FAKE_INSTANCE_NAME, computeInstance.getName());
    }

    // test case: checks if a given flavor has a requirement
    @Test
    public void testFlavorHasRequirements() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(OpenStackPluginUtils.class);
        BDDMockito.given(OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .willReturn(ANY_STRING);

        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        String getFlavorsResponseJson = createExtraSpecsResponseJson(createFakeExtraSpecs());
        Mockito.doReturn(getFlavorsResponseJson)
                .when(computePlugin).doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(GetFlavorExtraSpecsResponse.class);
        BDDMockito.given(GetFlavorExtraSpecsResponse.fromJson(Mockito.anyString()))
                .willCallRealMethod();

        Map<String, String> fakeRequirement = new HashMap<>();
        fakeRequirement.put(FAKE_REQUIREMENT, ANY_STRING);

        // exercise
        boolean hasRequirement = this.computePlugin.flavorHasRequirements(cloudUser, fakeRequirement, flavorId);

        // verify
        Assert.assertTrue(hasRequirement);

        PowerMockito.verifyStatic(OpenStackPluginUtils.class);
        OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        Mockito.verify(computePlugin, Mockito.times(1))
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetFlavorExtraSpecsResponse.class);
        GetFlavorExtraSpecsResponse.fromJson(Mockito.anyString());
    }

    // test case: when a flavor does not have a requirement, it should return false
    @Test
    public void testFlavorHasNotRequirements() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(OpenStackPluginUtils.class);
        BDDMockito.given(OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .willReturn(ANY_STRING);

        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        String getFlavorsResponseJson = createExtraSpecsResponseJson(createFakeExtraSpecs());
        Mockito.doReturn(getFlavorsResponseJson)
                .when(computePlugin).doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(GetFlavorExtraSpecsResponse.class);
        BDDMockito.given(GetFlavorExtraSpecsResponse.fromJson(Mockito.anyString()))
                .willCallRealMethod();

        Map<String, String> fakeRequirement = new HashMap<>();
        fakeRequirement.put(FAKE_UNMET_REQUIREMENT, FAKE_UNMET_REQUIREMENT);

        // exercise
        boolean hasRequirement = this.computePlugin.flavorHasRequirements(cloudUser, fakeRequirement, flavorId);

        // verify
        Assert.assertFalse(hasRequirement);

        PowerMockito.verifyStatic(OpenStackPluginUtils.class);
        OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        Mockito.verify(computePlugin, Mockito.times(1))
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetFlavorExtraSpecsResponse.class);
        GetFlavorExtraSpecsResponse.fromJson(Mockito.anyString());
    }

    // test case: the updateFlavors() should perform a HTTP request for the flavors and
    // update the current data the plugin holds
    @Test
    public void testUpdateFlavorsSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(OpenStackPluginUtils.class);
        BDDMockito.given(OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .willReturn(ANY_STRING);

        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        String getAllFlavorsResponseJson = createGetAllFlavorsResponseJson();
        Mockito.doReturn(getAllFlavorsResponseJson)
                .when(computePlugin).doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.spy(GetAllFlavorsResponse.class);
        BDDMockito.given(GetAllFlavorsResponse.fromJson(Mockito.anyString()))
                .willCallRealMethod();

        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();
        Map<String, String> fakeRequirement = new HashMap<>();
        fakeRequirement.put(FAKE_REQUIREMENT, ANY_STRING);
        computeOrder.setRequirements(fakeRequirement);

        Mockito.doNothing().when(this.computePlugin).setHardwareRequirementsList(Mockito.any());
        Mockito.doReturn(false).doReturn(true).when(this.computePlugin)
                .flavorHasRequirements(Mockito.eq(cloudUser), Mockito.any(), Mockito.any());

        Mockito.doReturn(getHardwareRequirementsList()).when(this.computePlugin)
                .detailFlavors(Mockito.any(), Mockito.any(), Mockito.any());

        // exercise
        this.computePlugin.updateFlavors(computeOrder, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackPluginUtils.class);
        OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetAllFlavorsResponse.class);
        GetAllFlavorsResponse.fromJson(Mockito.anyString());

        Mockito.verify(this.computePlugin, Mockito.atLeast(1))
                .flavorHasRequirements(Mockito.eq(cloudUser), Mockito.any(), Mockito.any());

        Mockito.verify(this.computePlugin, Mockito.times(1))
                .detailFlavors(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(this.computePlugin, Mockito.times(1)).setHardwareRequirementsList(Mockito.any());
    }

    // test case: when a request is unsuccessful, it should thrown InternalServerErrorException
    @Test(expected = InternalServerErrorException.class)
    public void testUpdateFlavorsUnsuccessfully() throws FogbowException, HttpResponseException {
        // set up
        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();
        Map<String, String> fakeRequirement = new HashMap<>();
        fakeRequirement.put(FAKE_REQUIREMENT, ANY_STRING);
        computeOrder.setRequirements(fakeRequirement);

        Mockito.when(clientMock.doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser)))
                .thenThrow(InternalServerErrorException.class);

        // exercise
        this.computePlugin.updateFlavors(computeOrder, cloudUser);

        Assert.fail();
    }

    // test case: given and order, it should call
    @Test
    public void testDeleteInstance() throws FogbowException, HttpResponseException {
        // set up
        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();
        Mockito.doNothing().when(this.computePlugin)
                .doDeleteRequest(Mockito.any(), Mockito.any());

        // exercise
        this.computePlugin.deleteInstance(computeOrder, cloudUser);

        // verify
        Mockito.verify(this.computePlugin, Mockito.times(1))
                .doDeleteRequest(Mockito.any(), Mockito.eq(cloudUser));
    }

    // test case: Test if after a successful request, it can return the
    // appropriate computeInstance
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        PowerMockito.mockStatic(OpenStackPluginUtils.class);
        BDDMockito.given(OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .willReturn(ANY_STRING);

        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        String rawInstanceResponse = createGetComputeResponseJson(testUtils.FAKE_INSTANCE_ID, FAKE_INSTANCE_NAME);
        Mockito.doReturn(rawInstanceResponse).when(this.computePlugin)
                .doGetRequest(Mockito.any(), Mockito.any());

        ComputeInstance fakeComputeInstance = new ComputeInstance(instanceId, ANY_STRING, ANY_STRING, null, null);
        Mockito.doReturn(fakeComputeInstance).when(computePlugin)
                .getInstanceFromJson(Mockito.anyString());

        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();

        // exercise
        this.computePlugin.getInstance(computeOrder, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackPluginUtils.class);
        OpenStackPluginUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());
    }

    // test case: when a request is unsuccessful, it should thrown InternalServerErrorException
    @Test(expected = InternalServerErrorException.class)
    public void testGetInstanceUnsuccessfully() throws FogbowException, HttpResponseException {
        // set up
        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();

        Mockito.when(clientMock.doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser)))
                .thenThrow(InternalServerErrorException.class);

        // exercise
        this.computePlugin.getInstance(computeOrder, cloudUser);

        Assert.fail();
    }

    // test case: Test if the networkdIds contain the defaultNetworkId
    @Test
    public void testGetNetworkIds() {
        // set up
        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();

        // exercise
        List<String> networkIds = this.computePlugin.getNetworkIds(computeOrder);

        // verify
        Assert.assertEquals(defaultNetworkId, networkIds.get(0));
    }

    // test case? test if
    @Test
    public void testSetAllocationToOrder() {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        HardwareRequirements hardwareRequirements = Mockito.mock(HardwareRequirements.class);

        // exercise
        this.computePlugin.setAllocationToOrder(computeOrder, hardwareRequirements);

        // verify
        Mockito.verify(computeOrder, Mockito.times(testUtils.RUN_ONCE))
                .setActualAllocation(Mockito.any(ComputeAllocation.class));
    }

    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        Mockito.when(this.clientMock.doPostRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(createCreateComputeResponseJson());

        // exercise
        String instanceId = this.computePlugin.doRequestInstance(FAKE_PROJECT_ID, FAKE_KEY_NAME, ANY_STRING, cloudUser);

        // verify
        Assert.assertEquals(this.instanceId, instanceId);
    }

    @Test(expected = InternalServerErrorException.class)
    public void testDoRequestInstanceUnsuccessfully() throws FogbowException {
        // set up
        Mockito.when(this.clientMock.doPostRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(InternalServerErrorException.class);

        // exercise
        this.computePlugin.doRequestInstance(FAKE_PROJECT_ID, FAKE_KEY_NAME, ANY_STRING, cloudUser);

        // verify
        Assert.fail();
    }

    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();

        HardwareRequirements mockRequirements = Mockito.mock(HardwareRequirements.class);
        Mockito.doReturn(mockRequirements).when(this.computePlugin).findSmallestFlavor(computeOrder, cloudUser);
        Mockito.doReturn(new ArrayList<String>()).when(this.computePlugin).getNetworkIds(Mockito.any());
        Mockito.doReturn(ANY_STRING).when(this.computePlugin).getKeyName(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(this.computePlugin).setAllocationToOrder(Mockito.eq(computeOrder), Mockito.any(HardwareRequirements.class));
        Mockito.doReturn(ANY_STRING).when(this.computePlugin)
                .doRequestInstance(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, cloudUser);

        // verify
        Assert.assertEquals(ANY_STRING, instanceId);
    }

    private Map<String, String> createFakeExtraSpecs() {
        Map<String, String> extraSpecs = new HashMap();

        extraSpecs.put(FAKE_REQUIREMENT, ANY_STRING);

        return extraSpecs;
    }

    private String createExtraSpecsResponseJson(Map<String, String> extraSpecs) {
        List<String> specs = new ArrayList();

        for (Map.Entry entry : extraSpecs.entrySet()) {
            specs.add(entry.getKey() + ":\"" + entry.getValue()+"\"");
        }

        return "{\"extra_specs\": {"+String.join(",", specs)+"}}";
    }private String createGetComputeResponseJson(String id, String name) {
        return "{\"server\":{\"id\":\""+id+"\",\"name\":\""+name+"\",\"addresses\":{\"provider\":" +
                "[{\"addr\":\"192.168.0.3\"}]},\"flavor\":{\"id\":1},\"status\":\"ACTIVE\"}}";
    }

    private String createGetAllFlavorsResponseJson() {
        return "{\"flavors\":[{\"id\":\"1\"},{\"id\":\"2\"},{\"id\":\"3\"},{\"id\":\"4\"}]}";
    }

    private String createCreateComputeResponseJson () {
        return "{\"server\":{\"id\":\""+this.instanceId+"\"}}";
    }

    private List<String> getFlavorIdsFromHardwareRequirementsList() {
        List<String> flavorIds = new ArrayList();
        TreeSet<HardwareRequirements> requirements = getHardwareRequirementsList();

        for (HardwareRequirements requirement : requirements) {
            flavorIds.add(requirement.getFlavorId());
        }
        return flavorIds;
    }

    private List<String> getMockedNetworkIds() {
        List<String> networksIds = new ArrayList<>();
        int qtd = 5;
        for (int i = 0; i < qtd; ++i) {
            String fakeNetworksId = privateNetworkId + Integer.toString(i);
            networksIds.add(fakeNetworksId);
        }

        return networksIds;
    }

    private Properties getPropertiesMock() {
        Properties properties = Mockito.mock(Properties.class);

        Mockito.when(properties.getProperty(OpenStackPluginUtils.COMPUTE_NOVA_URL_KEY))
                .thenReturn(this.computeNovaV2UrlKey);
        Mockito.when(properties.getProperty(OpenStackPluginUtils.DEFAULT_NETWORK_ID_KEY))
                .thenReturn(defaultNetworkId);

        return properties;
    }

    private TreeSet<HardwareRequirements> getHardwareRequirementsList() {
        TreeSet<HardwareRequirements> hardwareRequirements = new TreeSet();

        int qtd = 10;
        for (int i = 0; i < qtd-1; ++i) {
            HardwareRequirements requirements = new HardwareRequirements(
                    "nameflavor" + Integer.toString(i),
                    Integer.toString(i),
                    Math.max(1, testUtils.CPU_VALUE - 1 - i),
                    Math.max(1, testUtils.MEMORY_VALUE - 1 - i),
                    Math.max(1, testUtils.MEMORY_VALUE - 1 - i));

            hardwareRequirements.add(requirements);
        }

        hardwareRequirements.add(
                new HardwareRequirements(
                        "nameflavor" + bestFlavorId,
                bestFlavorId,
                testUtils.CPU_VALUE,
                testUtils.MEMORY_VALUE,
                testUtils.DISK_VALUE)
        );
        return hardwareRequirements;
    }
}
