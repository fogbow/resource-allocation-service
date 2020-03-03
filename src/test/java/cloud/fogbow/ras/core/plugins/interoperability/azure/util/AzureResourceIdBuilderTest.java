package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

public class AzureResourceIdBuilderTest {

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


        String networkInterfaceIdExpected = String.format(AzureConstants.NETWORK_INTERFACE_STRUCTURE,
                this.azureUser.getSubscriptionId(),
                AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME,
                networkInterfaceName);

        // exercise
        String networkInterfaceId = AzureResourceIdBuilder.configure(AzureConstants.NETWORK_INTERFACE_STRUCTURE)
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

        String virtualMachineIdExpected = String.format(AzureConstants.VIRTUAL_MACHINE_STRUCTURE,
                this.azureUser.getSubscriptionId(),
                AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME,
                virtualMachineName);

        // exercise
        String networkInterfaceId = AzureResourceIdBuilder.configure(AzureConstants.VIRTUAL_MACHINE_STRUCTURE)
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(virtualMachineName)
                .build();

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

        String idInTheLimitSize = AzureResourceIdBuilder.configure(AzureConstants.BIGGER_STRUCTURE)
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(resourceNameInTheLimit)
                .build();

        // exercise
        AzureResourceIdBuilder.configure()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(resourceNameInTheLimit)
                .checkIdSizePolicy();

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

        String idInTheLimitSize = AzureResourceIdBuilder.configure(AzureConstants.BIGGER_STRUCTURE)
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(resourceNameInTheLimit)
                .build();

        // verify
        Assert.assertEquals(Order.FIELDS_MAX_SIZE + outOfLimit, idInTheLimitSize.length());
        this.expectedException.expect(InvalidParameterException.class);
        this.expectedException.expectMessage(String.format(Messages.Error.ERROR_ID_LIMIT_SIZE_EXCEEDED, outOfLimit));

        // exercise
        AzureResourceIdBuilder.configure()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(resourceNameInTheLimit)
                .checkIdSizePolicy();

    }

    private int getWhatLeftToTotalLimit() {
        String anyIdEmpty = AzureResourceIdBuilder.configure(AzureConstants.BIGGER_STRUCTURE)
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName("")
                .build();

        return Order.FIELDS_MAX_SIZE - anyIdEmpty.length();
    }


}
