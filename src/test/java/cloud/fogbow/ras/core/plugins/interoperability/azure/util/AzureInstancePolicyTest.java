package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

@PrepareForTest({ AzureInstancePolicy.class })
public class AzureInstancePolicyTest extends AzureTestUtils {

    private static final String DEFINE_AND_CHECK_RESOURCE_NAME_METHOD = "defineAndCheckResourceName";
    private static final String DEFINE_AZURE_RESOURCE_NAME_METHOD = "defineAzureResourceName";
    private static final String DEFINE_RESOURCE_ID_BY_METHOD = "defineResourceIdBy";
    
    private TestUtils testUtils;
    
    @Before
    public void setup() {
        this.testUtils = new TestUtils();
    }

    // test case: When invoking the defineAzureResourceName method with a
    // computeOrder, it must verify that his internal methods have been called.
    @Test
    public void testDefineAzureResourceNameWithComputeOrder() throws Exception {
        // set up
        String orderName = ORDER_NAME;
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        Mockito.when(computeOrder.getName()).thenReturn(orderName);

        PowerMockito.mockStatic(AzureInstancePolicy.class);
        PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
                Mockito.eq(computeOrder));

        String prefix = AzureConstants.VIRTUAL_MACHINE_ID_PREFIX;
        String resourceId = prefix + RESOURCE_ID;
        PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, DEFINE_RESOURCE_ID_BY_METHOD,
                Mockito.eq(computeOrder));
        
        String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
        PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_AND_CHECK_RESOURCE_NAME_METHOD,
                Mockito.eq(resourceId), Mockito.eq(orderName));

        // exercise
        AzureInstancePolicy.defineAzureResourceName(computeOrder);

        // verify
        Mockito.verify(computeOrder, Mockito.times(TestUtils.RUN_ONCE)).getName();
        
        PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureInstancePolicy.defineResourceIdBy(computeOrder);
        
        PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureInstancePolicy.defineAndCheckResourceName(Mockito.eq(resourceId), Mockito.eq(orderName));
    }
    
    // test case: When invoking the defineAzureResourceName method with a
    // networkOrder, it must verify that his internal methods have been called.
    @Test
    public void testDefineAzureResourceNameWithNetworkOrder() throws Exception {
        // set up
        String orderName = ORDER_NAME;
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(networkOrder.getName()).thenReturn(orderName);

        PowerMockito.mockStatic(AzureInstancePolicy.class);
        PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
                Mockito.eq(networkOrder));

        String prefix = AzureConstants.VIRTUAL_NETWORK_ID_PREFIX;
        String resourceId = prefix + RESOURCE_ID;
        PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, DEFINE_RESOURCE_ID_BY_METHOD,
                Mockito.eq(networkOrder));
        
        String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
        PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_AND_CHECK_RESOURCE_NAME_METHOD,
                Mockito.eq(resourceId), Mockito.eq(orderName));

        // exercise
        AzureInstancePolicy.defineAzureResourceName(networkOrder);

        // verify
        Mockito.verify(networkOrder, Mockito.times(TestUtils.RUN_ONCE)).getName();
        
        PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureInstancePolicy.defineResourceIdBy(networkOrder);
        
        PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureInstancePolicy.defineAndCheckResourceName(Mockito.eq(resourceId), Mockito.eq(orderName));
    }
    
    // test case: When invoking the defineAzureResourceName method with a
    // computeOrder, it must verify that his internal methods have been called.
    @Test
    public void testDefineAzureResourceNameWithVolumeOrder() throws Exception {
        // set up
        String orderName = ORDER_NAME;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getName()).thenReturn(orderName);

        PowerMockito.mockStatic(AzureInstancePolicy.class);
        PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
                Mockito.eq(volumeOrder));

        String prefix = AzureConstants.VOLUME_ID_PREFIX;
        String resourceId = prefix + RESOURCE_ID;
        PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, DEFINE_RESOURCE_ID_BY_METHOD,
                Mockito.eq(volumeOrder));
        
        String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
        PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_AND_CHECK_RESOURCE_NAME_METHOD,
                Mockito.eq(resourceId), Mockito.eq(orderName));

        // exercise
        AzureInstancePolicy.defineAzureResourceName(volumeOrder);

        // verify
        Mockito.verify(volumeOrder, Mockito.times(TestUtils.RUN_ONCE)).getName();
        
        PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureInstancePolicy.defineResourceIdBy(volumeOrder);
        
        PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureInstancePolicy.defineAndCheckResourceName(Mockito.eq(resourceId), Mockito.eq(orderName));
    }
    
    // test case: When invoking the defineResourceIdBy method with an order without
    // the name attribute, the InvalidParameterException must be thrown.
    @Test
    public void testDefineResourceIdByFail() throws Exception {
        // set up
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(RESOURCE_ID);
        String expected = String.format(Messages.Exception.UNSUPPORTED_ORDER_NAME, order.getType());
        
        try {
            // exercise
            AzureInstancePolicy.defineResourceIdBy(order);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the generateResourceId method, it must generate a
    // random resource ID starting with the prefix passed by parameter.
    @Test
    public void testGenerateResourceId() {
        // set up
        String prefix = RESOUCE_ID_PREFIX;

        // exercise
        String resourceId = AzureInstancePolicy.generateResourceId(prefix);

        // verify
        Assert.assertTrue(resourceId.startsWith(prefix));
    }
    
    // test case: When calling the defineAndCheckResourceName method with the size
    // of the resource name up to the supported character limit, it must return a
    // valid resource name.
    @Test
    public void testDefineAndCheckResourceName() throws InvalidParameterException {
        // set up
        String resourceId = AzureInstancePolicy.generateResourceId(RESOUCE_ID_PREFIX);
        String orderName = ORDER_NAME;
        String expected = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
        
        // exercise
        String resourceName = AzureInstancePolicy.defineAndCheckResourceName(resourceId, orderName);

        // verify
        Assert.assertEquals(expected, resourceName);
    }
    
    // test case: When calling the defineAndCheckResourceName method with the
    // resource name size above the supported character limit, an
    // InvalidParameterException should be thrown.
    @Test
    public void testDefineAndCheckResourceNameFail() {
        // set up
        String resourceId = AzureInstancePolicy.generateResourceId(RESOUCE_ID_PREFIX);
        String orderName = SdkContext.randomResourceName(ORDER_NAME, BIGGEST_LENGTH);
        String expected = String.format(Messages.Error.ERROR_ID_LIMIT_SIZE_EXCEEDED, SIZE_EXCEEDED);
        
        try {
            // exercise
            AzureInstancePolicy.defineAndCheckResourceName(resourceId, orderName);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

}
