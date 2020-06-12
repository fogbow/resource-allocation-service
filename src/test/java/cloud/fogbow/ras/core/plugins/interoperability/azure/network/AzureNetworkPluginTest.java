package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureVirtualNetworkOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureGeneralUtil.class, AsyncInstanceCreationManager.class, AsyncInstanceCreationManager.class})
public class AzureNetworkPluginTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AzureUser azureUser;
    private String defaultResourceGroupName;
    private String defaultRegionName;
    private AzureNetworkPlugin azureNetworkPlugin;
    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperation;

    @Before
    public void setUp() {

        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.azureNetworkPlugin = Mockito.spy(new AzureNetworkPlugin(azureConfFilePath));
        AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK =
                new AzureVirtualNetworkOperationSDK(this.defaultRegionName, this.defaultResourceGroupName);
        this.azureVirtualNetworkOperation = Mockito.spy(azureVirtualNetworkOperationSDK);
        this.azureNetworkPlugin.setAzureVirtualNetworkOperationSDK(this.azureVirtualNetworkOperation);
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the isReady method and the instance state is ready,
    // it must verify if It returns true value.
    @Test
    public void testIsReadySuccessfullyWhenIsReady() {
        // set up
        String instanceState = AzureStateMapper.SUCCEEDED_STATE;

        // exercise and verify
        Assert.assertTrue(this.azureNetworkPlugin.isReady(instanceState));
    }

    // test case: When calling the isReady method and the instance state is not ready,
    // it must verify if It returns false value.
    @Test
    public void testIsReadySuccessfullyWhenNotReady() {
        // set up
        String instanceState = TestUtils.ANY_VALUE;

        // exercise and verify
        Assert.assertFalse(this.azureNetworkPlugin.isReady(instanceState));
    }

    // test case: When calling the hasFailed method with any value,
    // it must verify if It returns false.
    @Test
    public void testHasFailedSuccessfullyWhenIsFailed() {
        // set up
        String instanceState = TestUtils.ANY_VALUE;

        // exercise and verify
        Assert.assertFalse(this.azureNetworkPlugin.hasFailed(instanceState));
    }

    // test case: When calling the requestInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        String cidr = "10.10.10.10/24";
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String orderId = "orderId";
        String name = AzureTestUtils.ORDER_NAME;
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, name);
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getCidr()).thenReturn(cidr);
        Mockito.when(networkOrder.getId()).thenReturn(orderId);
        Mockito.when(networkOrder.getName()).thenReturn(name);

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.when(AzureGeneralUtil.generateResourceName()).thenReturn(resourceName);

        Mockito.doNothing().when(this.azureVirtualNetworkOperation)
                .doCreateInstance(Mockito.any(), Mockito.any(), Mockito.any());

        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRefExpected = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceName)
                .cidr(cidr)
                .tags(tags)
                .checkAndBuild();
        String instanceIdExpected = AzureGeneralUtil.defineInstanceId(resourceName);

        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);
        Mockito.doReturn(finishCreationCallbacks)
                .when(this.azureNetworkPlugin).startInstanceCreation(Mockito.eq(instanceIdExpected));

        Mockito.doNothing().when(this.azureNetworkPlugin)
                .waitAndCheckForInstanceCreationFailed(Mockito.eq(instanceIdExpected));

        // exercise
        String instanceId = this.azureNetworkPlugin.requestInstance(networkOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualNetworkOperation, Mockito.times(TestUtils.RUN_ONCE)).doCreateInstance(
                Mockito.eq(azureCreateVirtualNetworkRefExpected), Mockito.eq(this.azureUser), Mockito.eq(finishCreationCallbacks));
        Mockito.verify(this.azureNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE))
                .startInstanceCreation(Mockito.eq(instanceId));
        Mockito.verify(this.azureNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE))
                .waitAndCheckForInstanceCreationFailed(Mockito.eq(instanceIdExpected));
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: When calling the requestInstance method with mocked methods and throws an exception,
    // it must verify if it rethrows the same exception.
    @Test
    public void testRequestInstanceFail() throws FogbowException {
        // set up
        String cidr = "10.10.10.10/24";
        String name = AzureTestUtils.ORDER_NAME;
        String orderId = "orderId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getCidr()).thenReturn(cidr);
        Mockito.when(networkOrder.getId()).thenReturn(orderId);
        Mockito.when(networkOrder.getName()).thenReturn(name);

        FogbowException exceptionExpected = new FogbowException(TestUtils.ANY_VALUE);
        Mockito.doThrow(exceptionExpected)
                .when(this.azureNetworkPlugin).doCreateInstance(Mockito.any(), Mockito.any(), Mockito.any());

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureNetworkPlugin.requestInstance(networkOrder, this.azureUser);
    }

    // test case: When calling the doCreateInstance method,
    // it must verify if It don't throw an exception.
    @Test
    public void testDoCreateInstanceSuccessfully() throws FogbowException {
        // set up
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = Mockito.mock(AzureCreateVirtualNetworkRef.class);
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        Mockito.doNothing().when(this.azureVirtualNetworkOperation)
                .doCreateInstance(Mockito.any(), Mockito.any(), Mockito.any());

        try {
            // exercise
            this.azureNetworkPlugin.doCreateInstance(this.azureUser, azureCreateVirtualNetworkRef, finishCreationCallbacks);
        } catch (Exception e) {
            // verify
            Assert.fail();
        }
    }

    // test case: When calling the doCreateInstance method and throws an exception,
    // it must verify if It rethrow the exception and perform the doOnComplete.
    @Test
    public void testDoCreateInstanceFail() throws FogbowException {
        // set up
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = Mockito.mock(AzureCreateVirtualNetworkRef.class);
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        Mockito.doThrow(new FogbowException())
                .when(this.azureVirtualNetworkOperation)
                .doCreateInstance(Mockito.any(), Mockito.any(), Mockito.eq(finishCreationCallbacks));

        try {
            // exercise
            this.azureNetworkPlugin.doCreateInstance(this.azureUser, azureCreateVirtualNetworkRef, finishCreationCallbacks);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnError();
            Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.NEVER_RUN)).runOnComplete();
        }
    }

    // test case: When calling the buildNetworkInstance method,
    // it must verify if it returns the right networkInstance.
    @Test
    public void testBuildNetworkInstanceSuccessfully() {
        // set up
        String cirdExpected = "cirdExpected";
        String idExpected = "idExpected";
        String nameExpected = "nameExpected";
        String stateExpected = "stateExpected";

        // exercise
        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = AzureGetVirtualNetworkRef.builder()
                .cidr(cirdExpected)
                .id(idExpected)
                .name(nameExpected)
                .state(stateExpected)
                .build();
        NetworkInstance networkInstance = this.azureNetworkPlugin.buildNetworkInstance(azureGetVirtualNetworkRef);

        // verify
        Assert.assertEquals(cirdExpected, networkInstance.getCidr());
        Assert.assertEquals(idExpected, networkInstance.getId());
        Assert.assertEquals(stateExpected, networkInstance.getCloudState());
        Assert.assertEquals(nameExpected, networkInstance.getName());
        Assert.assertEquals(AzureGeneralUtil.NO_INFORMATION, networkInstance.getGateway());
        Assert.assertEquals(AzureGeneralUtil.NO_INFORMATION, networkInstance.getInterfaceState());
        Assert.assertEquals(AzureGeneralUtil.NO_INFORMATION, networkInstance.getMACInterface());
        Assert.assertEquals(NetworkAllocationMode.DYNAMIC, networkInstance.getAllocationMode());
    }

    // test case: When calling the getInstance method with mocked methods,
    // it must verify if it returns the right networkInstance.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getInstanceId()).thenReturn(instanceId);

        String resourceNameExpected = AzureGeneralUtil.defineResourceName(instanceId);

        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = Mockito.mock(AzureGetVirtualNetworkRef.class);
        Mockito.doReturn(azureGetVirtualNetworkRef)
                .when(this.azureVirtualNetworkOperation).doGetInstance(
                        Mockito.eq(resourceNameExpected), Mockito.eq(this.azureUser));

        NetworkInstance networkInstanceExpected = Mockito.mock(NetworkInstance.class);
        Mockito.doReturn(networkInstanceExpected)
                .when(this.azureNetworkPlugin).buildNetworkInstance(Mockito.eq(azureGetVirtualNetworkRef));

        // exercise
        NetworkInstance networkInstance = this.azureNetworkPlugin.getInstance(networkOrder, this.azureUser);

        // verify
        Assert.assertEquals(networkInstanceExpected, networkInstance);
    }

    // test case: When calling the getInstance method with mocked methods and the instance is creating,
    // it must verify if it returns the right networkInstance with state creating.
    @Test
    public void testGetInstanceSuccessfullyWhenInstanceIsCreating() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getInstanceId()).thenReturn(instanceId);

        NetworkInstance networkInstanceCreating = Mockito.mock(NetworkInstance.class);
        Mockito.doReturn(networkInstanceCreating).when(this.azureNetworkPlugin)
                .getCreatingInstance(Mockito.eq(instanceId));

        // exercise
        NetworkInstance networkInstance = this.azureNetworkPlugin.getInstance(networkOrder, this.azureUser);

        // verify
        Assert.assertEquals(networkInstanceCreating, networkInstance);
    }

    // test case: When calling the getInstance method with mocked methods and throws an exception,
    // it must verify if it rethrows the same exception.
    @Test
    public void testGetInstanceFail() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getInstanceId()).thenReturn(instanceId);
        String resourceNameExpected = AzureGeneralUtil.defineResourceName(instanceId);

        FogbowException exceptionExpected = new FogbowException(TestUtils.ANY_VALUE);
        Mockito.doThrow(exceptionExpected)
                .when(this.azureVirtualNetworkOperation).doGetInstance(
                Mockito.eq(resourceNameExpected), Mockito.eq(this.azureUser));

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureNetworkPlugin.getInstance(networkOrder, this.azureUser);
    }

    // test case: When calling the deleteInstance method with mocked method,
    // it must verify if it executes all methods with right values.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getInstanceId()).thenReturn(instanceId);

        String resourceNameExpected = AzureGeneralUtil.defineResourceName(instanceId);

        Mockito.doNothing().when(this.azureVirtualNetworkOperation)
                .doDeleteInstance(Mockito.anyString(), Mockito.any());

        // exercise
        this.azureNetworkPlugin.deleteInstance(networkOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualNetworkOperation, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(resourceNameExpected), Mockito.eq(this.azureUser));
        Mockito.verify(this.azureNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE))
                .endInstanceCreation(instanceId);
    }

    // test case: When calling the deleteInstance method with mocked methods and throws an exception,
    // it must verify if it rethrows the same exception.
    @Test
    public void testDeleteInstanceFail() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getInstanceId()).thenReturn(instanceId);
        String resourceNameExpected = AzureGeneralUtil.defineResourceName(instanceId);

        FogbowException exceptionExpected = new FogbowException(TestUtils.ANY_VALUE);
        Mockito.doThrow(exceptionExpected).when(this.azureVirtualNetworkOperation)
                .doDeleteInstance(Mockito.eq(resourceNameExpected), Mockito.eq(this.azureUser));

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        try {
            this.azureNetworkPlugin.deleteInstance(networkOrder, this.azureUser);
        } finally {
            // verify
            Mockito.verify(this.azureNetworkPlugin, Mockito.never()).endInstanceCreation(instanceId);
        }
    }

}
