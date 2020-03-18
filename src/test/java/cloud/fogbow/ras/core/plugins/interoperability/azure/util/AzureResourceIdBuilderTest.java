package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

public class AzureResourceIdBuilderTest extends AzureTestUtils {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private AzureUser azureUser;

    @Before
    public void setUp() {
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the buildNetworkInterfaceId method,
    // it must verify if It return the right network interface id.
    @Test
    public void testBuildNetworkInterfaceIdSuccessfully() {
        // set up
        String networkInterfaceName = "networkInterfaceName";


        String networkInterfaceIdExpected = String.format(AzureResourceIdBuilder.NETWORK_INTERFACE_STRUCTURE,
                this.azureUser.getSubscriptionId(),
                AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME,
                networkInterfaceName);

        // exercise
        String networkInterfaceId = AzureResourceIdBuilder.networkInterfaceId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(networkInterfaceName)
                .build();

        // verify
        Assert.assertEquals(networkInterfaceIdExpected, networkInterfaceId);
    }

    // test case: When calling the buildVirtualMachineId method, it must verify if
    // It returns the right virtual machine id.
    @Test
    public void testBuildVirtualMachineIdSuccessfully() {
        // set up
        String virtualMachineName = "virtualMachineName";

        String virtualMachineIdExpected = String.format(AzureResourceIdBuilder.VIRTUAL_MACHINE_STRUCTURE,
                this.azureUser.getSubscriptionId(),
                AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME,
                virtualMachineName);

        // exercise
        String networkInterfaceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(virtualMachineName)
                .build();

        // verify
        Assert.assertEquals(virtualMachineIdExpected, networkInterfaceId);
    }
    
    // test case: When calling the diskId constructor to build a disk resource ID,
    // it must verify that it returns a valid disk ID.
    @Test
    public void testBuildDiskIdSuccessfully() {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        String expected = String.format(AzureResourceIdBuilder.DISK_STRUCTURE,
                this.azureUser.getSubscriptionId(), 
                AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME, 
                resourceName);

        // exercise
        String diskId = AzureResourceIdBuilder.diskId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(resourceName)
                .build();

        // verify
        Assert.assertEquals(expected, diskId);
    }

}
