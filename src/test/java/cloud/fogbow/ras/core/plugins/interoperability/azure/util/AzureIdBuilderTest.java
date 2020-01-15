package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AzureIdBuilderTest {

    private AzureUser azureUser;

    @Before
    public void setUp() {
        this.azureUser = AzureTestUtils.createAzureCloudUser();
    }

    // test case: When calling the buildVirtualMachineId method, it must verify if
    // It returns the right virtual machine id.
    @Test
    public void testBuildVirtualMachineIdSuccessfully() {
        // set up
        String virtualMachineName = "virtualMachineName";

        String virtualMachineIdExpected = String.format(AzureIdBuilder.VIRTUAL_MACHINE_STRUCTURE,
                this.azureUser.getSubscriptionId(),
                this.azureUser.getResourceGroupName(),
                virtualMachineName);

        // exercise
        String networkInterfaceId = AzureIdBuilder
                .configure(this.azureUser)
                .buildVirtualMachineId(virtualMachineName);

        // verify
        Assert.assertEquals(virtualMachineIdExpected, networkInterfaceId);
    }

}
