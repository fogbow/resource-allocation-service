package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.BiFunction;

public class AzureInstancePolicyTest {

    private AzureUser azureUser;

    @Before
    public void setUp() {
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the generateAzureResourceNameBy method with general order,
    // it must verify if it returns a resourceGroupName using the order id.
    @Test
    public void testGenerateAzureResourceNameSuccessfullyByWhenOrder() throws InvalidParameterException {
        // set up
        Order order = Mockito.mock(Order.class);
        String orderId = "orderId";
        Mockito.when(order.getId()).thenReturn(orderId);

        String resourceNameExpected = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + orderId;

        // exercise
        String resourceName = AzureInstancePolicy.generateAzureResourceNameBy(order.getId(),
                this.azureUser);

        // verify
        Assert.assertEquals(resourceNameExpected, resourceName);
    }

    // test case: When calling the generateAzureResourceNameBy method with Compute order,
    // it must verify if it returns a resourceGroupName using the order name.
    @Test
    public void testGenerateAzureResourceNameSuccessfullyByWhenComputeOrder() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        String orderId = "orderId";
        Mockito.when(computeOrder.getId()).thenReturn(orderId);
        String resourceNameExpected = "resourceName";
        Mockito.when(computeOrder.getName()).thenReturn(resourceNameExpected);

        // exercise
        String resourceName = AzureInstancePolicy.generateAzureResourceNameBy(
                computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(resourceNameExpected, resourceName);
    }

    // test case: When calling the generateFogbowInstanceIdBy method with Compute order,
    // it must verify if it returns an instance using the order name.
    @Test
    public void testGenerateFogbowInstanceIdBySuccessfullyWhenComputeOrder()
            throws InvalidParameterException {

        // set up
        String resourceName = "resourceName";
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        String orderId = "orderId";
        Mockito.when(computeOrder.getId()).thenReturn(orderId);
        Mockito.when(computeOrder.getName()).thenReturn(resourceName);

        String instanceIdExpected = AzureIdBuilder.configure(this.azureUser)
                .resourceName(resourceName)
                .resourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .structure(AzureIdBuilder.VIRTUAL_MACHINE_STRUCTURE)
                .build();

        // exercise
        String instanceId = AzureInstancePolicy
                .generateFogbowInstanceIdBy(computeOrder, this.azureUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: When calling the generateFogbowInstanceIdBy method with general order,
    // it must verify if it returns an instance using the order name.
    @Test
    public void testGenerateFogbowInstanceIdBySuccessfullyWhenOrder()
            throws InvalidParameterException {

        // set up
        Order order = Mockito.mock(Order.class);
        String orderId = "orderId";
        Mockito.when(order.getId()).thenReturn(orderId);
        BiFunction<String, AzureUser, String> builder = (name, cloudUser) ->
                AzureIdBuilder.configure(cloudUser)
                        .resourceName(name)
                        .resourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                        .structure(AzureIdBuilder.VIRTUAL_MACHINE_STRUCTURE)
                        .build();


        String resourceNameExpected = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + orderId;

        String instanceIdExpected = AzureIdBuilder.configure(this.azureUser)
                .resourceName(resourceNameExpected)
                .resourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .structure(AzureIdBuilder.VIRTUAL_MACHINE_STRUCTURE)
                .build();


        // exercise
        String instanceId = AzureInstancePolicy
                .generateFogbowInstanceIdBy(order.getId(), this.azureUser, builder);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

}
