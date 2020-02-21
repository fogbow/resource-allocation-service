package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AzureIdBuilderTest {

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

        String networkInterfaceIdExpected = String.format(AzureIdBuilder.NETWORK_INTERFACE_STRUCTURE,
                this.azureUser.getSubscriptionId(),
                this.azureUser.getResourceGroupName(),
                networkInterfaceName);

        // exercise
        String networkInterfaceId = AzureIdBuilder
                .configure(this.azureUser)
                .buildNetworkInterfaceId(networkInterfaceName);

        // verify
        Assert.assertEquals(networkInterfaceIdExpected, networkInterfaceId);
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

    // test case: When calling the checkIdSizePolicy method with resourceName within the limit,
    // it must verify if It does not throw an InvalidParameterException.
    @Test
    public void testCheckIdSizePolicySuccessfully() throws InvalidParameterException {
        // set up
        int whatLeftToTotalLimit = getWhatLeftToTotalLimit();
        String resourceNameInTheLimit = String.valueOf(new char[whatLeftToTotalLimit]);

        String idInTheLimitSize = AzureIdBuilder
                .configure(this.azureUser)
                .buildNetworkInterfaceId(resourceNameInTheLimit);

        // exercise
        AzureIdBuilder
                .configure(this.azureUser)
                .checkIdSizePolicy(resourceNameInTheLimit);

        // verify
        Assert.assertEquals(Order.FIELDS_MAX_SIZE, idInTheLimitSize.length());
    }

    // test case: When calling the checkIdSizePolicy method with resourceName out of the limit,
    // it must verify if It throws an InvalidParameterException.
    @Test
    public void testCheckIdSizePolicyFail() throws InvalidParameterException {
        // set up
        int whatLeftToTotalLimit = getWhatLeftToTotalLimit();
        int outOfLimit = 1;
        String resourceNameInTheLimit = String.valueOf(new char[whatLeftToTotalLimit + outOfLimit]);

        String idInTheLimitSize = AzureIdBuilder
                .configure(this.azureUser)
                .buildNetworkInterfaceId(resourceNameInTheLimit);

        // verify
        Assert.assertEquals(Order.FIELDS_MAX_SIZE + outOfLimit, idInTheLimitSize.length());
        this.expectedException.expect(InvalidParameterException.class);
        this.expectedException.expectMessage(String.format(Messages.Error.ERROR_ID_LIMIT_SIZE_EXCEEDED, outOfLimit));

        // exercise
        AzureIdBuilder
                .configure(this.azureUser)
                .checkIdSizePolicy(resourceNameInTheLimit);

    }

    private int getWhatLeftToTotalLimit() {
        String anyIdEmpty = AzureIdBuilder
                .configure(this.azureUser)
                .buildId(AzureIdBuilder.BIGGER_STRUCTURE, "");
        return Order.FIELDS_MAX_SIZE - anyIdEmpty.length();
    }


}
