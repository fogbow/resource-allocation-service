package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

public class AzureInstancePolicyTest {

    private AzureUser azureUser;

    @Before
    public void setUp() {
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the defineAzureResourceName method with general order,
    // it must verify if it returns a resourceName using the order id.
    @Test
    public void testDefineAzureResourceNameSuccessfully() throws InvalidParameterException {
        // set up
        ComputeOrder order = Mockito.mock(ComputeOrder.class);
        String orderId = "orderId";
        Mockito.when(order.getId()).thenReturn(orderId);

        String resourceNameExpected = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + orderId;

        // exercise
        String resourceName = AzureInstancePolicy.defineAzureResourceName(order, this.azureUser, "default-resource-group");

        // verify
        Assert.assertEquals(resourceNameExpected, resourceName);
    }

    // test case: When calling the generateFogbowInstanceId method with Compute order,
    // it must verify if it returns an instance using the order name.
    @Test
    public void testGenerateFogbowInstanceIdSuccessfully() throws InvalidParameterException {
        // set up
        String resourceName = "fogbow-orderId";
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        String orderId = "orderId";
        Mockito.when(computeOrder.getId()).thenReturn(orderId);
        Mockito.when(computeOrder.getName()).thenReturn(resourceName);

        String instanceIdExpected = AzureResourceIdBuilder.configure(AzureConstants.VIRTUAL_MACHINE_STRUCTURE)
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .build();

        // exercise
        String instanceId = AzureInstancePolicy.generateFogbowInstanceId(computeOrder, this.azureUser, resourceGroupName);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

}
