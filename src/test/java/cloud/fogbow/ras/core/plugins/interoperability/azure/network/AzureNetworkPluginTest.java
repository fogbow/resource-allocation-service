package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureVirtualNetworkOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.util.Properties;

@PrepareForTest({AzureGeneralUtil.class})
public class AzureNetworkPluginTest extends AzureTestUtils {

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

    // test case: When calling the requestInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        String cidr = "10.10.10.10/24";
        String name = "name";
        String orderId = "orderId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getCidr()).thenReturn(cidr);
        Mockito.when(networkOrder.getId()).thenReturn(orderId);

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.when(AzureGeneralUtil.generateResourceName()).thenReturn(name);

        Mockito.doNothing().when(this.azureVirtualNetworkOperation).doCreateInstance(Mockito.any(), Mockito.any());

        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRefExpected = AzureCreateVirtualNetworkRef.builder()
                .name(name)
                .cidr(cidr)
                .checkAndBuild();
        String instanceIdExpected = AzureGeneralUtil.defineInstanceId(name);

        // exercise
        String instanceId = this.azureNetworkPlugin.requestInstance(networkOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualNetworkOperation, Mockito.times(TestUtils.RUN_ONCE)).doCreateInstance(
                Mockito.eq(azureCreateVirtualNetworkRefExpected), Mockito.eq(this.azureUser));
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: When calling the requestInstance method with mocked methods and throws an exception,
    // it must verify if it rethrows the same exception.
    @Test
    public void testRequestInstanceFail() throws FogbowException {
        // set up
        String cidr = "10.10.10.10/24";
        String name = "name";
        String orderId = "orderId";
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getCidr()).thenReturn(cidr);
        Mockito.when(networkOrder.getId()).thenReturn(orderId);
        Mockito.when(networkOrder.getName()).thenReturn(name);

        FogbowException exceptionExpected = new FogbowException(TestUtils.ANY_VALUE);
        Mockito.doThrow(exceptionExpected)
                .when(this.azureVirtualNetworkOperation).doCreateInstance(Mockito.any(), Mockito.any());

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureNetworkPlugin.requestInstance(networkOrder, this.azureUser);
    }

}
