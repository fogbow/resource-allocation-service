package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureVirtualMachineOperation;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@PrepareForTest({})
public class AzureComputePluginTest extends TestUtils {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AzureComputePlugin azureComputePlugin;
    private AzureUser azureUser;
    private AzureVirtualMachineOperation azureVirtualMachineOperation;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.azureComputePlugin = Mockito.spy(new AzureComputePlugin(azureConfFilePath));
        this.azureVirtualMachineOperation = Mockito.mock(AzureVirtualMachineOperationSDK.class);
        this.azureComputePlugin.setAzureVirtualMachineOperation(this.azureVirtualMachineOperation);
        this.azureUser = AzureTestUtils.createAzureUser();
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

}
