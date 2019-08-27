package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseManager.class})
public class OpenStackComputePluginTest extends BaseUnitTests {

    private OpenStackComputePlugin computePlugin;
    private OpenStackV3User openStackV3User;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private OpenStackHttpClient clientMock;
    private Properties propertiesMock;
    private final String defaultNetworkId = "fake-default-network-id";
    private final String imageId = "image-id";
    private final String publicKey = "public-key";
    private final String idKeyName = "493315b3-dd01-4b38-974f-289570f8e7ee";
    private final String bestFlavorId = "best-flavor";
    private final int bestVcpu = 8;
    private final int bestMemory = 1024;

    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String ANY_URL = "http://localhost:8008";
    private static final String ANY_STRING = "any-strinf";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";

    private final int bestDisk = 40;
    private final String privateNetworkId = "fake-private-network-id";
    private final String userData = "userDataFromLauchCommand";
    private final List<String> networksId = new ArrayList<String>();
    private final List<String> responseNetworkIds = new ArrayList<String>(networksId);
    private final String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    private final String expectedInstanceId = "instance-id-00";
    private final String localIpAddress = "localIpAddress";
    private final String instanceId = "compute-instance-id";
    private final String requirementTag = "besttag";
    private final String requirementValue = "bestvalue";
    private final int vCPU = 10;
    private final int ram = 15;
    private final int disk = 20;
    private String computeEndpoint;
    private String flavorEndpoint;
    private String flavorExtraSpecsEndpoint;
    private String osKeyPairEndpoint;
    private String hostName = "hostName";
    private String flavorId = "flavorId";
    private final String computeNovaV2UrlKey = "compute-nova-v2-url-key";
    private String openStackStateActive = OpenStackStateMapper.ACTIVE_STATUS;
    private SharedOrderHolders sharedOrderHolders;

    @Before
    public void setUp() throws Exception {
        testUtils.mockReadOrdersFromDataBase();

        this.propertiesMock = getPropertiesMock();
        this.clientMock = Mockito.mock(OpenStackHttpClient.class);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.networksId.add(privateNetworkId);

        this.computePlugin = Mockito.spy(new OpenStackComputePlugin(this.propertiesMock,
                this.launchCommandGeneratorMock, this.clientMock));

        this.openStackV3User = new OpenStackV3User(TestUtils.FAKE_USER_ID, TestUtils.FAKE_USER_NAME,
                this.FAKE_TOKEN_VALUE, this.FAKE_PROJECT_ID);
    }

    // test case: given a cloudUser with projectId field, should return the
    // projectId
    @Test
    public void testGetProjectId() throws InvalidParameterException {
        // exercise
        String projectId = this.computePlugin.getProjectId(this.openStackV3User);

        // verify
        Assert.assertEquals(this.FAKE_PROJECT_ID, projectId);
    }

    // test case: given a cloudUser without projectId field, should throw
    // an InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testGetProjectIdOfUnscopedToken() throws InvalidParameterException {
        // setup
        OpenStackV3User unscopedFakeUser = new OpenStackV3User(TestUtils.FAKE_USER_ID, TestUtils.FAKE_USER_NAME,
                this.FAKE_TOKEN_VALUE, null);
        // exercise
        this.computePlugin.getProjectId(unscopedFakeUser);

        Assert.fail();
    }

    // test case: when given and order, it should return a flavor with it's resources
    // greater than or equal the requested
    @Test
    public void testGetBestFlavor() throws FogbowException {
        // setup
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        Mockito.doNothing().when(this.computePlugin)
                .updateFlavors(Mockito.eq(this.openStackV3User), Mockito.eq(computeOrder));

        Mockito.doReturn(createHardwareRequirementsList()).when(this.computePlugin)
                .getHardwareRequirementsList();

        // exercise
        HardwareRequirements requirements = this.computePlugin.getBestFlavor(computeOrder, openStackV3User);

        // verify
        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .updateFlavors(Mockito.eq(this.openStackV3User), Mockito.eq(computeOrder));
        Mockito.verify(computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getHardwareRequirementsList();
        Assert.assertTrue(bestDisk <= requirements.getDisk());
        Assert.assertTrue(bestVcpu <= requirements.getCpu());
        Assert.assertTrue(bestMemory <= requirements.getMemory());
    }

    // test case: when a order is given, return any hardwareRequirements
    // that suits the need of the order
    @Test
    public void testFindSmallestFlavor() throws FogbowException {
        // setup
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        HardwareRequirements bestRequirements = createHardwareRequirementsList().last();
        Mockito.doReturn(bestRequirements).when(this.computePlugin)
                .getBestFlavor(Mockito.eq(computeOrder), Mockito.eq(this.openStackV3User));

        // exercise
        HardwareRequirements actualRequirements = this.computePlugin.findSmallestFlavor(computeOrder, openStackV3User);

        // verify
        Assert.assertEquals(bestRequirements, actualRequirements);
    }

    // test case: when a order is given, no flavor will match the order so a NoAvailableResourcesException
    // will be thrown
    @Test(expected = NoAvailableResourcesException.class)
    public void testFindSmallestFlavorWhenNoResourceAvailable() throws FogbowException {
        // setup
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        Mockito.doReturn(null).when(this.computePlugin)
                .getBestFlavor(Mockito.eq(computeOrder), Mockito.eq(this.openStackV3User));

        // exercise
        this.computePlugin.findSmallestFlavor(computeOrder, openStackV3User);
    }

    // test case: test if getKeyName() returns a String
    @Test
    public void testGetKeyName() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        // exercise
        String keyName = this.computePlugin.getKeyName(this.FAKE_PROJECT_ID, openStackV3User, publicKey);

        // verify
        Mockito.verify(this.computePlugin, Mockito.times(testUtils.RUN_ONCE))
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.clientMock, Mockito.times(testUtils.RUN_ONCE))
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.eq(openStackV3User));
        Assert.assertTrue(keyName instanceof String);
    }

    // test case: test if deleteKeyName() will call doDeleteRequest passing the keyName
    @Test
    public void testDeleteKeyName() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(ANY_URL).when(this.computePlugin)
                .getComputeEndpoint(Mockito.anyString(), Mockito.anyString());

        // exercise
        this.computePlugin.deleteKeyName(FAKE_PROJECT_ID, openStackV3User, ANY_STRING);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(testUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(openStackV3User));
    }

    // test case: test if getRequestBody() will return an CreateComputeRequest instance
    //
    @Test
    public void testGetRequestBody() {
        // set up
        List<String> networksIds = getMockedNetworkIds();

        //exercise
        this.computePlugin.getRequestBody(FAKE_INSTANCE_NAME, imageId, flavorId, userData,
                ANY_STRING, networksIds);
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
        Mockito.when(properties.getProperty(Mockito.eq(OpenStackComputePlugin.DEFAULT_NETWORK_ID_KEY)))
                .thenReturn(this.defaultNetworkId);

        return propertiesMock;
    }

    private TreeSet<HardwareRequirements> createHardwareRequirementsList() {
        TreeSet<HardwareRequirements> hardwareRequirements = new TreeSet();

        int qtd = 10;
        for (int i = 0; i < qtd-1; ++i) {
            HardwareRequirements requirements = new HardwareRequirements(
                    "nameflavor" + Integer.toString(i),
                    Integer.toString(i),
                    Math.max(1, bestVcpu - 1 - i),
                    Math.max(1, bestMemory - 1 - i),
                    Math.max(1, bestDisk - 1 - i));

            hardwareRequirements.add(requirements);
        }

        hardwareRequirements.add(
                new HardwareRequirements(
                        "nameflavor" + bestFlavorId,
                bestFlavorId,
                bestVcpu,
                bestMemory,
                bestDisk)
        );
        return hardwareRequirements;
    }
}
