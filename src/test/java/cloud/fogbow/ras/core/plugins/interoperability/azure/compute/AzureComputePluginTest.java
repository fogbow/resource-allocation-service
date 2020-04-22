package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralPolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import com.microsoft.azure.management.compute.VirtualMachineSize;
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
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureImageOperationUtil.class, AzureGeneralPolicy.class})
public class AzureComputePluginTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AzureComputePlugin azureComputePlugin;
    private AzureUser azureUser;
    private AzureVirtualMachineOperationSDK azureVirtualMachineOperation;
    private String defaultVirtualNetworkName;
    private String defaultResourceGroupName;
    private String defaultRegionName;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultVirtualNetworkName = properties.getProperty(AzureConstants.DEFAULT_VIRTUAL_NETWORK_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.azureComputePlugin = Mockito.spy(new AzureComputePlugin(azureConfFilePath));
        this.azureVirtualMachineOperation = Mockito.mock(AzureVirtualMachineOperationSDK.class);
        this.azureComputePlugin.setAzureVirtualMachineOperation(this.azureVirtualMachineOperation);
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the isReady method and the instance state is ready,
    // it must verify if It returns true value
    @Test
    public void testIsReadySuccessfullyWhenIsReady() {
        // set up
        String instanceState = AzureStateMapper.SUCCEEDED_STATE;

        // exercise and verify
        Assert.assertTrue(this.azureComputePlugin.isReady(instanceState));
    }

    // test case: When calling the isReady method and the instance state is not ready,
    // it must verify if It returns false value
    @Test
    public void testIsReadySuccessfullyWhenNotReady() {
        // set up
        String instanceState = AzureStateMapper.CREATING_STATE;

        // exercise and verify
        Assert.assertFalse(this.azureComputePlugin.isReady(instanceState));
    }

    // test case: When calling the hasFailed method and the instance state is failed,
    // it must verify if It returns true value
    @Test
    public void testHasFailedSuccessfullyWhenIsFailed() {
        // set up
        String instanceState = AzureStateMapper.FAILED_STATE;

        // exercise and verify
        Assert.assertTrue(this.azureComputePlugin.hasFailed(instanceState));
    }

    // test case: When calling the hasFailed method and the instance state is not failed,
    // it must verify if It returns false value
    @Test
    public void testHasFailedSuccessfullyWhenIsNotFailed() {
        // set up
        String instanceState = AzureStateMapper.SUCCEEDED_STATE;

        // exercise and verify
        Assert.assertFalse(this.azureComputePlugin.hasFailed(instanceState));
    }
    
    // test case: When calling the getVirtualNetworkId method with a non-empty list
    // of network IDs, it must return the resource ID of the first item in the list.
    @Test
    public void testGetVirtualNetworkIdWithNetworkIds() {
        // set up
        String[] networkIds = { AzureTestUtils.RESOURCE_NAME };
        List<String> networkIdList = Arrays.asList(networkIds);
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networkIdList);

        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String expected = AzureResourceIdBuilder.networkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(networkIdList.listIterator().next())
                .build();

        // exercise
        String virtualNetworkId = this.azureComputePlugin.getVirtualNetworkId(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(expected, virtualNetworkId);
    }
    
    // test case: When calling the getVirtualNetworkId method with an empty list of
    // network IDs, it must then return the resource Id of default virtual network.
    @Test
    public void testGetVirtualNetworkIdWithoutNetworkIds() {
        // set up
        List<String> networkIdList = Collections.EMPTY_LIST;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networkIdList);

        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceName = this.defaultVirtualNetworkName;
        String expected = AzureResourceIdBuilder.networkId()
              .withSubscriptionId(subscriptionId)
              .withResourceGroupName(this.defaultResourceGroupName)
              .withResourceName(resourceName)
              .build();
        
        // exercise
        String virtualNetworkId = this.azureComputePlugin.getVirtualNetworkId(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(expected, virtualNetworkId);
    }

    // test case: When calling the requestInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureGeneralPolicy.class);
        String imageId = "imageId";
        String orderName = "orderName";
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn(imageId);
        Mockito.when(computeOrder.getName()).thenReturn(orderName);

        String virtualNetworkId = "virtualNetworkId";
        Mockito.doReturn(virtualNetworkId).when(this.azureComputePlugin)
                .getVirtualNetworkId(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        String virtualMachineSizeName = "virtualMachineSizeName";
        VirtualMachineSize virtualMachineSizeMock = Mockito.mock(VirtualMachineSize.class);
        Mockito.when(virtualMachineSizeMock.name()).thenReturn(virtualMachineSizeName);

        Mockito.doReturn(virtualMachineSizeMock).when(this.azureComputePlugin)
                .getVirtualMachineSize(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        int disk = 1;
        Mockito.when(AzureGeneralPolicy.getDisk(Mockito.eq(computeOrder))).thenReturn(disk);

        AzureGetImageRef azureGetImageRef = new AzureGetImageRef("", "", "");
        PowerMockito.mockStatic(AzureImageOperationUtil.class);
        Mockito.when(AzureImageOperationUtil.buildAzureVirtualMachineImageBy(Mockito.eq(imageId)))
                .thenReturn(azureGetImageRef);

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        Mockito.doReturn(resourceName).when(this.azureComputePlugin).generateResourceName();

        String userData = "userData";
        Mockito.doReturn(userData).when(this.azureComputePlugin).getUserData(Mockito.eq(computeOrder));

        String password = "password";
        PowerMockito.when(AzureGeneralPolicy.generatePassword()).thenReturn(password);

        String regionName = this.defaultRegionName;
        String resourceGroupName = this.defaultResourceGroupName;
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, orderName);

        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .resourceName(resourceName)
                .azureGetImageRef(azureGetImageRef)
                .virtualNetworkId(virtualNetworkId)
                .diskSize(disk)
                .size(virtualMachineSizeName)
                .osComputeName(orderName)
                .osUserName(AzureComputePlugin.DEFAULT_OS_USER_NAME)
                .osUserPassword(password)
                .regionName(regionName)
                .resourceGroupName(resourceGroupName)
                .userData(userData)
                .tags(tags)
                .checkAndBuild();


        // exercise
        this.azureComputePlugin.requestInstance(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureComputePlugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                Mockito.eq(computeOrder), Mockito.eq(this.azureUser), Mockito.eq(azureCreateVirtualMachineRef),
                Mockito.any());
    }

    // test case: When calling the requestInstance method any throws any exception,
    // it must verify if it re-throws the exception.
    @Test
    public void testRequestInstanceFail() throws FogbowException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);

        Mockito.doThrow(FogbowException.class).when(this.azureComputePlugin)
                .getVirtualNetworkId(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.azureComputePlugin.requestInstance(computeOrder, this.azureUser);
    }

    // test case: When calling the getVirtualMachineSizeName method,
    // it must verify if it calls the method with right parameters.
    @Test
    public void testGetVirtualMachineSizeName() throws FogbowException {
        // set up
        int memory = 1;
        int vcpu = 1;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getMemory()).thenReturn(memory);
        Mockito.when(computeOrder.getvCPU()).thenReturn(vcpu);

        String regionName = this.defaultRegionName;

        // exercise
        this.azureComputePlugin.getVirtualMachineSize(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperation, Mockito.times(TestUtils.RUN_ONCE)).findVirtualMachineSize(
                Mockito.eq(memory), Mockito.eq(vcpu), Mockito.eq(regionName), Mockito.eq(this.azureUser)
        );
    }


    // test case: When calling the getInstance method, it must verify if It returns
    // the right computeInstance.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getInstanceId()).thenReturn(resourceName);

        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        Mockito.doReturn(resourceId).when(this.azureComputePlugin).buildResourceId(Mockito.eq(subscriptionId),
                Mockito.eq(resourceName));

        AzureGetVirtualMachineRef azureGetVirtualMachineRef = Mockito.mock(AzureGetVirtualMachineRef.class);
        Mockito.when(this.azureVirtualMachineOperation.doGetInstance(Mockito.eq(resourceId), Mockito.eq(this.azureUser)))
                .thenReturn(azureGetVirtualMachineRef);

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.azureComputePlugin)
                .buildComputeInstance(Mockito.eq(azureGetVirtualMachineRef), Mockito.eq(this.azureUser));

        // exercise
        ComputeInstance computeInstance = this.azureComputePlugin.getInstance(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
    }


    // test case: When calling the getInstance method and throws a Exception,
    // it must verify if It does not treat and rethrow the same exception.
    @Test
    public void testGetInstanceFail() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getInstanceId()).thenReturn(resourceName);

        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        Mockito.doReturn(resourceId).when(this.azureComputePlugin).buildResourceId(Mockito.eq(subscriptionId),
                Mockito.eq(resourceName));

        Mockito.when(this.azureVirtualMachineOperation.doGetInstance(Mockito.eq(resourceId), Mockito.eq(this.azureUser)))
                .thenThrow(new UnexpectedException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        this.azureComputePlugin.getInstance(computeOrder, this.azureUser);
    }

    // test case: When calling the buildComputeInstance method,
    // it must verify if it returns the right ComputeInstance.
    @Test
    public void testBuildComputeInstanceSuccessfully() {
        // set up
        int diskExpected = 1;
        int memoryExpected = 2;
        int vcpuExpected = 3;
        String cloudStateExpected = AzureStateMapper.SUCCEEDED_STATE;
        List<String> ipAddressExpected = Arrays.asList("id");
        String nameExpected = "virtualMachineNameExpected";
        Map expectedTags = Collections.singletonMap(AzureConstants.TAG_NAME, nameExpected);

        String idExpected = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(nameExpected)
                .build();

        AzureGetVirtualMachineRef azureGetVirtualMachineRef = AzureGetVirtualMachineRef.builder()
                .disk(diskExpected)
                .vCPU(vcpuExpected)
                .memory(memoryExpected)
                .cloudState(cloudStateExpected)
                .name(nameExpected)
                .ipAddresses(ipAddressExpected)
                .tags(expectedTags)
                .build();

        // exercise
        ComputeInstance computeInstance = this.azureComputePlugin
                .buildComputeInstance(azureGetVirtualMachineRef, this.azureUser);

        // verify
        Assert.assertEquals(diskExpected, computeInstance.getDisk());
        Assert.assertEquals(memoryExpected, computeInstance.getRam());
        Assert.assertEquals(vcpuExpected, computeInstance.getvCPU());
        Assert.assertEquals(idExpected, computeInstance.getId());
        Assert.assertEquals(cloudStateExpected, computeInstance.getCloudState());
        Assert.assertEquals(ipAddressExpected, computeInstance.getIpAddresses());
        Assert.assertEquals(nameExpected, computeInstance.getName());
    }

    // test case: When calling the deleteInstance method,
    // it must verify if it calls the method with right parameters.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getInstanceId()).thenReturn(resourceName);

        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceId = AzureResourceIdBuilder.virtualMachineId().withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName).withResourceName(resourceName).build();

        Mockito.doReturn(resourceId).when(this.azureComputePlugin).buildResourceId(Mockito.eq(subscriptionId),
                Mockito.eq(resourceName));

        Mockito.doNothing().when(this.azureVirtualMachineOperation).doDeleteInstance(Mockito.eq(resourceId),
                Mockito.eq(this.azureUser));

        // exercise
        this.azureComputePlugin.deleteInstance(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperation, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(resourceId), Mockito.eq(this.azureUser));
    }

}
