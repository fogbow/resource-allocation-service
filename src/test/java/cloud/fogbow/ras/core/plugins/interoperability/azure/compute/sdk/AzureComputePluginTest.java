package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureVirtualMachineOperation;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.*;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureImageOperationUtil.class, AzureInstancePolicy.class, AzureGeneralPolicy.class})
public class AzureComputePluginTest extends TestUtils {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AzureComputePlugin azureComputePlugin;
    private AzureUser azureUser;
    private AzureVirtualMachineOperation azureVirtualMachineOperation;
    private String defaultNetworkInterfaceName;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultNetworkInterfaceName = properties.getProperty(AzureConstants.DEFAULT_NETWORK_INTERFACE_NAME_KEY);
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

    // test case: When calling the getNetworkInterfaceId method without network in the order,
    // it must verify if It returns the default networkInterfaceId.
    @Test
    public void testGetNetworkInterfaceIdSuccessfullyWhenEmptyNetworkInOrder() throws FogbowException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        List<String> networds = new ArrayList<>();
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networds);

        String networkInsterfaceIdExpected = AzureIdBuilder
                .configure(this.azureUser)
                .buildNetworkInterfaceId(this.defaultNetworkInterfaceName);

        // exercise
        String networkInterfaceId = this.azureComputePlugin.getNetworkInterfaceId(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(networkInsterfaceIdExpected, networkInterfaceId);
    }

    // test case: When calling the getNetworkInterfaceId method with one network in the order,
    // it must verify if It returns the network in the order.
    @Test
    public void testGetNetworkInterfaceIdSuccessfullyWhenOneNetworkInOrder() throws FogbowException {
        // set up
        String nertworkIdExpeceted = "networkId";
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        List<String> networds = new ArrayList<>();
        networds.add(nertworkIdExpeceted);
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networds);

        // exercise
        String networkInterfaceId = this.azureComputePlugin
                .getNetworkInterfaceId(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(nertworkIdExpeceted, networkInterfaceId);
    }

    // test case: When calling the getNetworkInterfaceId method with more than one network in the order,
    // it must verify if It throws a FogbowException.
    @Test
    public void testGetNetworkInterfaceIdFailWhenMoreThanOneNetworkInOrder() throws FogbowException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        List<String> networds = Arrays.asList("one", "two");
        Mockito.when(computeOrder.getNetworkIds()).thenReturn(networds);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(Messages.Error.ERROR_MULTIPLE_NETWORKS_NOT_ALLOWED);

        // exercise
        this.azureComputePlugin.getNetworkInterfaceId(computeOrder, this.azureUser);

    }

    // test case: When calling the requestInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureGeneralPolicy.class);
        String imageId = "imageId";
        String orderId = "orderId";
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getImageId()).thenReturn(imageId);
        Mockito.when(computeOrder.getId()).thenReturn(orderId);

        String networkInterfaceId = "networkInterfaceId";
        Mockito.doReturn(networkInterfaceId).when(this.azureComputePlugin)
                .getNetworkInterfaceId(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        String virtualMachineSizeName = "virtualMachineSizeName";
        Mockito.doReturn(virtualMachineSizeName).when(this.azureComputePlugin)
                .getVirtualMachineSizeName(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

        int disk = 1;
        Mockito.when(AzureGeneralPolicy.getDisk(Mockito.eq(computeOrder))).thenReturn(disk);

        AzureGetImageRef azureGetImageRef = new AzureGetImageRef("", "", "");
        PowerMockito.mockStatic(AzureImageOperationUtil.class);
        Mockito.when(AzureImageOperationUtil.buildAzureVirtualMachineImageBy(Mockito.eq(imageId)))
                .thenReturn(azureGetImageRef);

        String virtualMachineName = "virtualMachineName";
        PowerMockito.mockStatic(AzureInstancePolicy.class);
        PowerMockito.when(AzureInstancePolicy.
                generateAzureResourceNameBy(Mockito.eq(computeOrder), Mockito.eq(this.azureUser)))
                .thenReturn(virtualMachineName);

        String userData = "userData";
        Mockito.doReturn(userData).when(this.azureComputePlugin).getUserData(Mockito.eq(computeOrder));

        String password = "password";
        PowerMockito.when(AzureGeneralPolicy.generatePassword()).thenReturn(password);

        String regionName = this.azureUser.getRegionName();
        String resourceGroupName = this.azureUser.getResourceGroupName();

        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .virtualMachineName(virtualMachineName)
                .azureGetImageRef(azureGetImageRef)
                .networkInterfaceId(networkInterfaceId)
                .diskSize(disk)
                .size(virtualMachineSizeName)
                .osComputeName(orderId)
                .osUserName(orderId)
                .osUserPassword(password)
                .regionName(regionName)
                .resourceGroupName(resourceGroupName)
                .userData(userData)
                .checkAndBuild();


        // exercise
        this.azureComputePlugin.requestInstance(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureComputePlugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                Mockito.eq(computeOrder), Mockito.eq(this.azureUser), Mockito.eq(azureCreateVirtualMachineRef)
        );
    }

    // test case: When calling the requestInstance method any throws any exception,
    // it must verify if it re-throws the exception.
    @Test
    public void testRequestInstanceFail() throws FogbowException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);

        Mockito.doThrow(FogbowException.class).when(this.azureComputePlugin)
                .getNetworkInterfaceId(Mockito.eq(computeOrder), Mockito.eq(this.azureUser));

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

        String regionName = this.azureUser.getRegionName();

        // exercise
        this.azureComputePlugin.getVirtualMachineSizeName(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperation, Mockito.times(TestUtils.RUN_ONCE)).findVirtualMachineSizeName(
                Mockito.eq(memory), Mockito.eq(vcpu), Mockito.eq(regionName), Mockito.eq(this.azureUser)
        );
    }


    // test case: When calling the getInstance method, it must verify if It returns the right computeInstance.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder();
        String instanceId = "instanceId";
        computeOrder.setInstanceId(instanceId);

        AzureGetVirtualMachineRef azureGetVirtualMachineRef = Mockito.mock(AzureGetVirtualMachineRef.class);
        Mockito.when(this.azureVirtualMachineOperation
                .doGetInstance(Mockito.eq(instanceId), Mockito.eq(this.azureUser)))
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
        ComputeOrder computeOrder = new ComputeOrder();
        String instanceId = "instanceId";
        computeOrder.setInstanceId(instanceId);

        Mockito.when(this.azureVirtualMachineOperation
                .doGetInstance(Mockito.eq(instanceId), Mockito.eq(this.azureUser)))
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
        String nameExpected = "name";
        int memoryExpected = 2;
        int vcpuExpected = 3;
        String cloudStateExpected = AzureStateMapper.SUCCEEDED_STATE;
        List<String> ipAddressExpected = Arrays.asList("id");
        String idExpected = AzureIdBuilder.configure(this.azureUser).buildVirtualMachineId(nameExpected);
        AzureGetVirtualMachineRef azureGetVirtualMachineRef = AzureGetVirtualMachineRef.builder()
                .disk(diskExpected)
                .vCPU(vcpuExpected)
                .memory(memoryExpected)
                .cloudState(cloudStateExpected)
                .name(nameExpected)
                .ipAddresses(ipAddressExpected)
                .build();

        // exercise
        ComputeInstance computeInstance = this.azureComputePlugin
                .buildComputeInstance(azureGetVirtualMachineRef, this.azureUser);

        // verify
        Assert.assertEquals(diskExpected, computeInstance.getDisk());
        Assert.assertEquals(memoryExpected, computeInstance.getMemory());
        Assert.assertEquals(vcpuExpected, computeInstance.getvCPU());
        Assert.assertEquals(idExpected, computeInstance.getId());
        Assert.assertEquals(cloudStateExpected, computeInstance.getCloudState());
        Assert.assertEquals(ipAddressExpected, computeInstance.getIpAddresses());
    }

    // test case: When calling the deleteInstance method,
    // it must verify if it calls the method with right parameters.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        String instanceId = "instanceId";
        Mockito.when(computeOrder.getInstanceId()).thenReturn(instanceId);

        Mockito.doNothing()
                .when(this.azureVirtualMachineOperation)
                .doDeleteInstance(Mockito.eq(instanceId), Mockito.eq(this.azureUser));

        // exercise
        this.azureComputePlugin.deleteInstance(computeOrder, this.azureUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperation, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(instanceId), Mockito.eq(this.azureUser));
    }


}
