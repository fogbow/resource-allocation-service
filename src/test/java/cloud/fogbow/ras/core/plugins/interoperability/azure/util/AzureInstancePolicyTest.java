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
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

@PrepareForTest({ AzureInstancePolicy.class })
public class AzureInstancePolicyTest extends AzureTestUtils {

    private static final String DEFINE_AND_CHECK_RESOURCE_NAME_METHOD = "defineAndCheckResourceName";
    private static final String DEFINE_AZURE_RESOURCE_NAME_METHOD = "defineAzureResourceName";
	private static final String DEFINE_VIRTUAL_MACHINE_ID_BY_METHOD = "defineVirtualMachineIdBy";
	private static final String DEFINE_VIRTUAL_NETWORK_ID_BY_METHOD = "defineVirtualNetworkIdBy";
	private static final String DEFINE_VOLUME_ID_BY_METHOD = "defineVolumeIdBy";
	private static final String GENERATE_RESOURCE_ID_METHOD = "generateResourceId";
    
    private TestUtils testUtils;
    
    @Before
    public void setup() {
        this.testUtils = new TestUtils();
    }

	// test case: When invoking the defineAzureResourceName method with a
	// computeOrder, it must verify that the defineVirtualMachineIdBy method has
	// been called.
	@Test
	public void testDefineAzureResourceNameWithComputeOrder() throws Exception {
		// set up
		String orderName = ORDER_NAME;
		ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
		Mockito.when(computeOrder.getName()).thenReturn(orderName);

		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
				Mockito.eq(computeOrder));

		String prefix = AzureConstants.VIRTUAL_MACHINE_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
		PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_VIRTUAL_MACHINE_ID_BY_METHOD,
				Mockito.eq(computeOrder));

		// exercise
		AzureInstancePolicy.defineAzureResourceName(computeOrder);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineVirtualMachineIdBy(Mockito.eq(computeOrder));
	}
	
	// test case: When invoking the defineAzureResourceName method with a
	// networkOrder, it must verify that the defineVirtualNetworkIdBy method has
	// been called.
	@Test
	public void testDefineAzureResourceNameWithNetworkOrder() throws Exception {
		// set up
		String orderName = ORDER_NAME;
		NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
		Mockito.when(networkOrder.getName()).thenReturn(orderName);

		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
				Mockito.eq(networkOrder));

		String prefix = AzureConstants.VIRTUAL_NETWORK_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
		PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_VIRTUAL_NETWORK_ID_BY_METHOD,
				Mockito.eq(networkOrder));

		// exercise
		AzureInstancePolicy.defineAzureResourceName(networkOrder);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineVirtualNetworkIdBy(Mockito.eq(networkOrder));
	}
	
	// test case: When invoking the defineAzureResourceName method with a
	// volumeOrder, it must verify that the defineVolumeIdBy method has
	// been called.
	@Test
	public void testDefineAzureResourceNameWithVolumeOrder() throws Exception {
		// set up
		String orderName = ORDER_NAME;
		VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
		Mockito.when(volumeOrder.getName()).thenReturn(orderName);

		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
				Mockito.eq(volumeOrder));

		String prefix = AzureConstants.VOLUME_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
		PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_VOLUME_ID_BY_METHOD,
				Mockito.eq(volumeOrder));

		// exercise
		AzureInstancePolicy.defineAzureResourceName(volumeOrder);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineVolumeIdBy(Mockito.eq(volumeOrder));
	}
	
	// test case: When invoking the defineAzureResourceName method with a
	// publicIpOrder, it must verify that the defineResourceId method has
	// been called.
	@Test
	public void testDefineAzureResourceNameWithPublicIpOrder() throws Exception {
		// set up
		String computeOrderId = TestUtils.FAKE_ORDER_ID;
		PublicIpOrder publicIpOrder = this.testUtils.createLocalPublicIpOrder(computeOrderId);

		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_AZURE_RESOURCE_NAME_METHOD,
				Mockito.eq(publicIpOrder));

		String prefix = AzureConstants.PUBLIC_IP_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, GENERATE_RESOURCE_ID_METHOD,
				Mockito.eq(publicIpOrder));

		// exercise
		AzureInstancePolicy.defineAzureResourceName(publicIpOrder);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.generateResourceId(Mockito.eq(prefix));
	}
	
	// test case: When invoking the defineAzureResourceName method with an invalid
	// order, the InvalidParameterException must be thrown.
	@Test
	public void testDefineAzureResourceNameFail() throws Exception {
		// set up
		ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
		VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
		AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);

		String expected = String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE, order.getType());

		try {
			// exercise
			AzureInstancePolicy.defineAzureResourceName(order);
			Assert.fail();
		} catch (InvalidParameterException e) {
			// verify
			Assert.assertEquals(expected, e.getMessage());
		}
	}
	
	// test case: When invoking the defineVirtualMachineIdBy method with a valid
	// Order, it must verify that his internal methods have been called.
	@Test
	public void testDefineVirtualMachineIdBy() throws Exception {
		// set up
		String orderName = ORDER_NAME;
		ComputeOrder order = Mockito.spy(new ComputeOrder());
		Mockito.when(order.getName()).thenReturn(orderName);
		
		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_VIRTUAL_MACHINE_ID_BY_METHOD,
				Mockito.eq(order));

		String prefix = AzureConstants.VIRTUAL_MACHINE_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, GENERATE_RESOURCE_ID_METHOD,
				Mockito.eq(prefix));

		String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
		PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_AND_CHECK_RESOURCE_NAME_METHOD,
				Mockito.eq(resourceId), Mockito.eq(orderName));

		// exercise
		AzureInstancePolicy.defineVirtualMachineIdBy(order);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineVirtualMachineIdBy(Mockito.eq(order));

		Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).getName();

		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineAndCheckResourceName(Mockito.eq(resourceId), Mockito.eq(orderName));
	}
	
	// test case: When invoking the defineVirtualNetworkIdBy method with a valid
	// Order, it must verify that his internal methods have been called.
	@Test
	public void testDefineVirtualNetworkIdBy() throws Exception {
		// set up
		String orderName = ORDER_NAME;
		NetworkOrder order = Mockito.spy(new NetworkOrder());
		Mockito.when(order.getName()).thenReturn(orderName);

		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_VIRTUAL_NETWORK_ID_BY_METHOD,
				Mockito.eq(order));

		String prefix = AzureConstants.VIRTUAL_NETWORK_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, GENERATE_RESOURCE_ID_METHOD,
				Mockito.eq(prefix));

		String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
		PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_AND_CHECK_RESOURCE_NAME_METHOD,
				Mockito.eq(resourceId), Mockito.eq(orderName));

		// exercise
		AzureInstancePolicy.defineVirtualNetworkIdBy(order);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineVirtualNetworkIdBy(Mockito.eq(order));

		Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).getName();

		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineAndCheckResourceName(Mockito.eq(resourceId), Mockito.eq(orderName));
	}
	
	// test case: When invoking the defineVolumeIdBy method with a valid
	// Order, it must verify that his internal methods have been called.
	@Test
	public void testDefineVolumeIdBy() throws Exception {
		// set up
		String orderName = ORDER_NAME;
		VolumeOrder order = Mockito.spy(new VolumeOrder());
		Mockito.when(order.getName()).thenReturn(orderName);

		PowerMockito.mockStatic(AzureInstancePolicy.class);
		PowerMockito.doCallRealMethod().when(AzureInstancePolicy.class, DEFINE_VOLUME_ID_BY_METHOD,
				Mockito.eq(order));

		String prefix = AzureConstants.VOLUME_ID_PREFIX;
		String resourceId = prefix + RESOURCE_ID;
		PowerMockito.doReturn(resourceId).when(AzureInstancePolicy.class, GENERATE_RESOURCE_ID_METHOD,
				Mockito.eq(prefix));

		String resourceName = resourceId + AzureConstants.RESOURCE_NAME_SEPARATOR + orderName;
		PowerMockito.doReturn(resourceName).when(AzureInstancePolicy.class, DEFINE_AND_CHECK_RESOURCE_NAME_METHOD,
				Mockito.eq(resourceId), Mockito.eq(orderName));

		// exercise
		AzureInstancePolicy.defineVolumeIdBy(order);

		// verify
		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineVolumeIdBy(Mockito.eq(order));

		Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).getName();

		PowerMockito.verifyStatic(AzureInstancePolicy.class, Mockito.times(TestUtils.RUN_ONCE));
		AzureInstancePolicy.defineAndCheckResourceName(Mockito.eq(resourceId), Mockito.eq(orderName));
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
