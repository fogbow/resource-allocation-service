package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralPolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;

import com.microsoft.azure.management.compute.VirtualMachineSize;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AzureImageOperationUtil.class, AzureGeneralPolicy.class, AzureGeneralUtil.class })
public class AzureComputePluginTest {

    private AzureComputePlugin azureComputePlugin;
    private AzureUser azureUser;
    private AzureVirtualMachineOperationSDK azureVirtualMachineOperation;
    private DefaultLaunchCommandGenerator launchCommandGenerator;
    private String defaultVirtualNetworkName;
    private String defaultResourceGroupName;
    private String defaultRegionName;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultVirtualNetworkName = properties.getProperty(AzureConstants.DEFAULT_VIRTUAL_NETWORK_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.launchCommandGenerator = Mockito.mock(DefaultLaunchCommandGenerator.class);
        this.azureVirtualMachineOperation = Mockito.mock(AzureVirtualMachineOperationSDK.class);
        this.azureComputePlugin = Mockito.spy(new AzureComputePlugin(azureConfFilePath));
        this.azureComputePlugin.setAzureVirtualMachineOperation(this.azureVirtualMachineOperation);
        this.azureComputePlugin.setLaunchCommandGenerator(this.launchCommandGenerator);
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
    
    // test case: When calling the getVirtualNetworkResourceName method with a
    // non-empty list of network identifiers, it must check that the list
    // contains just one element and return it.
    @Test
    public void testGetVirtualNetworkResourceNameWithNetworkIds() throws FogbowException {
        // set up
        String expected = AzureTestUtils.RESOURCE_NAME;
        String[] networkIds = { expected };

        List<String> networkIdList = Arrays.asList(networkIds);
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networkIdList);

        // exercise
        String virtualNetworkId = this.azureComputePlugin
                .getVirtualNetworkResourceName(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(expected, virtualNetworkId);
    }
    
    // test case: When calling the getVirtualNetworkResourceName method with an
    // empty list of network IDs, it must then return the resource Id of default
    // virtual network.
    @Test
    public void testGetVirtualNetworkResourceNameWithoutNetworkIds() throws FogbowException {
        // set up
        List<String> networkIdList = Collections.EMPTY_LIST;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networkIdList);

        String expected = this.defaultVirtualNetworkName;
        
        // exercise
        String virtualNetworkId = this.azureComputePlugin
                .getVirtualNetworkResourceName(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(expected, virtualNetworkId);
    }

    // test case: When calling the checkNetworkIdListIntegrity method, with a
    // list of network IDs containing more than one element, it should check
    // that an InvalidParameterException was thrown.
    @Test
    public void testCheckNetworkIdListIntegrity() throws InvalidParameterException {
        // set up
        String[] networkIds = { "network-id-1", "network-id-2" };
        List<String> networkIdList = Arrays.asList(networkIds);

        String expected = Messages.Exception.MANY_NETWORKS_NOT_ALLOWED;

        try {
            // exercise
            this.azureComputePlugin.checkNetworkIdListIntegrity(networkIdList);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the requestInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testRequestInstanceSuccessfully() throws Exception {
        // set up
        String imageId = "imageId";
        String orderName = "orderName";
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn(imageId);
        Mockito.when(computeOrder.getName()).thenReturn(orderName);

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "generateResourceName");

        String virtualNetworkName = "virtualNetworkName";
        Mockito.doReturn(virtualNetworkName).when(this.azureComputePlugin)
                .getVirtualNetworkResourceName(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        AzureGetImageRef imageRef = Mockito.mock(AzureGetImageRef.class);
        PowerMockito.mockStatic(AzureImageOperationUtil.class);
        PowerMockito.doReturn(imageRef).when(AzureImageOperationUtil.class, "buildAzureVirtualMachineImageBy",
                Mockito.eq(imageId));

        String osUserPassword = "password";
        PowerMockito.mockStatic(AzureGeneralPolicy.class);
        PowerMockito.doReturn(osUserPassword).when(AzureGeneralPolicy.class, "generatePassword");

        String userData = "userData";
        Mockito.doReturn(userData).when(this.azureComputePlugin).getUserData(Mockito.eq(computeOrder));

        int diskSize = 1;
        PowerMockito.doReturn(diskSize).when(AzureGeneralPolicy.class, "getDisk", Mockito.eq(computeOrder));

        String virtualMachineSizeName = "virtualMachineSizeName";
        VirtualMachineSize virtualMachineSize = Mockito.mock(VirtualMachineSize.class);
        Mockito.when(virtualMachineSize.name()).thenReturn(virtualMachineSizeName);

        Mockito.doReturn(virtualMachineSize).when(this.azureComputePlugin)
                .getVirtualMachineSize(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, orderName);

        AzureCreateVirtualMachineRef virtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .regionName(this.defaultRegionName)
                .resourceName(resourceName)
                .virtualNetworkName(virtualNetworkName)
                .azureGetImageRef(imageRef)
                .osComputeName(orderName)
                .osUserName(AzureComputePlugin.DEFAULT_OS_USER_NAME)
                .osUserPassword(osUserPassword)
                .userData(userData)
                .diskSize(diskSize)
                .size(virtualMachineSizeName)
                .tags(tags)
                .checkAndBuild();

        // exercise
        this.azureComputePlugin.requestInstance(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureComputePlugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.eq(computeOrder), Mockito.eq(this.azureUser),
                Mockito.eq(virtualMachineRef), Mockito.eq(virtualMachineSize));
    }

    // test case: When calling the getUserData method, it must verify that is
    // call was successful.
    @Test
    public void testGetUserDataSuccessfully() {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);

        String userData = "userData";
        Mockito.when(this.launchCommandGenerator.createLaunchCommand(Mockito.eq(computeOrder)))
                .thenReturn(userData);

        // exercise
        this.azureComputePlugin.getUserData(computeOrder);

        // verify
        Mockito.verify(this.launchCommandGenerator, Mockito.times(TestUtils.RUN_ONCE))
                .createLaunchCommand(Mockito.eq(computeOrder));
    }

    // test case: When calling the doCreateInstance method,
    // it must verify if It don't throw an exception.
    @Test
    public void testDoCreateInstanceSuccessfully() {
        // set up
        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = Mockito.mock(AzureCreateVirtualMachineRef.class);
        Runnable doOnComplete = Mockito.mock(Runnable.class);

        try {
            // exercise
            this.azureComputePlugin.doCreateInstance(this.azureUser, azureCreateVirtualMachineRef, doOnComplete);
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
        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = Mockito.mock(AzureCreateVirtualMachineRef.class);
        Runnable doOnComplete = Mockito.mock(Runnable.class);

        Mockito.doThrow(new FogbowException()).when(this.azureVirtualMachineOperation)
                .doCreateInstance(Mockito.eq(azureCreateVirtualMachineRef), Mockito.eq(doOnComplete),
                Mockito.eq(this.azureUser));

        try {
            // exercise
            this.azureComputePlugin.doCreateInstance(this.azureUser, azureCreateVirtualMachineRef, doOnComplete);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Mockito.verify(doOnComplete, Mockito.times(TestUtils.RUN_ONCE)).run();
        }
    }

    // test case: When calling the getVirtualMachineSizeName method,
    // it must verify if it calls the method with right parameters.
    @Test
    public void testGetVirtualMachineSizeName() throws FogbowException {
        // set up
        int memory = 1;
        int vcpu = 1;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getRam()).thenReturn(memory);
        Mockito.when(computeOrder.getvCPU()).thenReturn(vcpu);

        String regionName = this.defaultRegionName;

        // exercise
        this.azureComputePlugin.getVirtualMachineSize(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperation, Mockito.times(TestUtils.RUN_ONCE))
                .findVirtualMachineSize(Mockito.eq(memory), Mockito.eq(vcpu), Mockito.eq(regionName),
                Mockito.eq(this.azureUser));
    }

    // test case: When calling the getInstance method, it must verify if It returns
    // the right computeInstance.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getInstanceId()).thenReturn(instanceId);

        AzureGetVirtualMachineRef virtualMachineRef = Mockito.mock(AzureGetVirtualMachineRef.class);
        Mockito.when(this.azureVirtualMachineOperation.doGetInstance(Mockito.eq(this.azureUser),
                Mockito.eq(resourceName))).thenReturn(virtualMachineRef);

        ComputeInstance computeInstanceExpected = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceExpected).when(this.azureComputePlugin)
                .buildComputeInstance(Mockito.eq(virtualMachineRef));

        ComputeInstance computeInstanceCreated = null;
        Mockito.doReturn(computeInstanceCreated).when(this.azureComputePlugin)
                .getCreatingInstance(Mockito.eq(instanceId));

        // exercise
        ComputeInstance computeInstance = this.azureComputePlugin.getInstance(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(computeInstanceExpected, computeInstance);
    }

    // test case: When calling the getInstance method with instance creating, it must verify if It returns
    // the right computeInstance.
    @Test
    public void testGetInstanceSuccessfullyWhenCreatingInstance() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getInstanceId()).thenReturn(instanceId);

        ComputeInstance computeInstanceCreating = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(computeInstanceCreating).when(this.azureComputePlugin)
                .getCreatingInstance(Mockito.eq(instanceId));

        // exercise
        ComputeInstance computeInstance = this.azureComputePlugin.getInstance(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(computeInstanceCreating, computeInstance);
    }

    // test case: When calling the buildComputeInstance method,
    // it must verify if it returns the right ComputeInstance.
    @Test
    public void testBuildComputeInstanceSuccessfully() {
        // set up
        String nameExpected = AzureTestUtils.ORDER_NAME;
        String idExpected = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(nameExpected)
                .build();

        String cloudStateExpected = AzureStateMapper.SUCCEEDED_STATE;
        int vcpuExpected = 3;
        int memoryExpected = 2;
        int diskExpected = 1;
        String ipAddressExpected = "ipAddress";
        List<String> ipAddressListExpected = Arrays.asList(ipAddressExpected);
        Map expectedTags = Collections.singletonMap(AzureConstants.TAG_NAME, nameExpected);

        AzureGetVirtualMachineRef azureGetVirtualMachineRef = AzureGetVirtualMachineRef.builder()
                .id(idExpected)
                .cloudState(cloudStateExpected)
                .vCPU(vcpuExpected)
                .memory(memoryExpected)
                .disk(diskExpected)
                .ipAddresses(ipAddressListExpected)
                .tags(expectedTags)
                .build();

        // exercise
        ComputeInstance computeInstance = this.azureComputePlugin
                .buildComputeInstance(azureGetVirtualMachineRef);

        // verify
        Assert.assertEquals(idExpected, computeInstance.getId());
        Assert.assertEquals(cloudStateExpected, computeInstance.getCloudState());
        Assert.assertEquals(nameExpected, computeInstance.getName());
        Assert.assertEquals(vcpuExpected, computeInstance.getvCPU());
        Assert.assertEquals(memoryExpected, computeInstance.getRam());
        Assert.assertEquals(diskExpected, computeInstance.getDisk());
        Assert.assertEquals(ipAddressListExpected, computeInstance.getIpAddresses());
    }

    // test case: When calling the deleteInstance method, it must verify if it
    // calls the method with right parameters.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getInstanceId()).thenReturn(resourceName);

        Mockito.doNothing().when(this.azureVirtualMachineOperation)
                .doDeleteInstance(Mockito.eq(this.azureUser), Mockito.eq(resourceName));

        // exercise
        this.azureComputePlugin.deleteInstance(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperation, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(this.azureUser), Mockito.eq(resourceName));
    }

}
