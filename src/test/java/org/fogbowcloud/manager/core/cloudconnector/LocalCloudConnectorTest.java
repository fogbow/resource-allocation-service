package org.fogbowcloud.manager.core.cloudconnector;

import java.util.HashMap;
import java.util.Map;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.mockito.Mockito.times;

import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseManager.class})
public class LocalCloudConnectorTest extends BaseUnitTests {
	
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_ORDER_ID = "fake-order-id";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_IMAGE_NAME = "fake-image-name";
	private static final int VCPU_TOTAL = 2;
	private static final int RAM_TOTAL = 2048;
	private static final int INSTANCES_TOTAL = 2;
	private static final int VCPU_USED = 1;
	private static final int RAM_USED = 1024;
	private static final int INSTANCES_USED = 1;
	
	private LocalCloudConnector localCloudConnector;
	
	private AaController aaController;
	private CloudPluginsHolder cloudPluginsHolder;

	private ComputePlugin computePlugin;
	private AttachmentPlugin attachmentPlugin;
	private NetworkPlugin networkPlugin;
	private VolumePlugin volumePlugin;
	private ImagePlugin imagePlugin;
	private ComputeQuotaPlugin computeQuotaPlugin;
	
	private Order order;
	private Image image;
	private FederationUser federationUser;
	
	private NetworkInstance networkInstance;
	private VolumeInstance volumeInstance;
	private AttachmentInstance attachmentInstance;
	private ComputeInstance computeInstance;
	
	@Before
    public void setUp() throws FogbowManagerException, UnexpectedException {
		
		
		// mocking databaseManager
		DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList());
        PowerMockito.mockStatic(DatabaseManager.class);
        BDDMockito.given(DatabaseManager.getInstance()).willReturn(databaseManager);
		
        // mocking class attributes
        this.federationUser = Mockito.mock(FederationUser.class);
        this.aaController = Mockito.mock(AaController.class);
        this.cloudPluginsHolder = Mockito.mock(CloudPluginsHolder.class);  
        this.computePlugin = Mockito.mock(ComputePlugin.class);
        this.attachmentPlugin = Mockito.mock(AttachmentPlugin.class);
        this.networkPlugin = Mockito.mock(NetworkPlugin.class);
        this.volumePlugin = Mockito.mock(VolumePlugin.class);
        this.imagePlugin = Mockito.mock(ImagePlugin.class);
        this.computeQuotaPlugin = Mockito.mock(ComputeQuotaPlugin.class);
        
        // mocking instances/image and the return of getID method
        this.networkInstance = Mockito.mock(NetworkInstance.class);
        Mockito.when(networkInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        
        this.volumeInstance = Mockito.mock(VolumeInstance.class);
        Mockito.when(volumeInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        
        this.attachmentInstance = Mockito.mock(AttachmentInstance.class);
        Mockito.when(attachmentInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        
        this.computeInstance = Mockito.mock(ComputeInstance.class);
        Mockito.when(computeInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
       
        this.image = Mockito.mock(Image.class);
        Mockito.when(image.getId()).thenReturn(FAKE_IMAGE_ID);
        
        // mocking cloudPluginsHolder to return the correct plugin for each call
        Mockito.when(cloudPluginsHolder.getComputePlugin()).thenReturn(computePlugin);
        Mockito.when(cloudPluginsHolder.getAttachmentPlugin()).thenReturn(attachmentPlugin);
        Mockito.when(cloudPluginsHolder.getNetworkPlugin()).thenReturn(networkPlugin);
        Mockito.when(cloudPluginsHolder.getVolumePlugin()).thenReturn(volumePlugin);
        Mockito.when(cloudPluginsHolder.getImagePlugin()).thenReturn(imagePlugin);
        Mockito.when(cloudPluginsHolder.getComputeQuotaPlugin()).thenReturn(computeQuotaPlugin);
        Mockito.when(aaController.getLocalToken(Mockito.any(FederationUser.class))).thenReturn(new Token());
        
        // starting the object we want to test
        this.localCloudConnector = new LocalCloudConnector(aaController, cloudPluginsHolder);
        
    }
	
	// test case: Request a compute instance Mockito.when the plugin returns a correct id
	@Test
	public void testRequestComputeInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(computePlugin.requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
		
	    // exercise
	    String returnedInstanceId = this.localCloudConnector.requestInstance(order);
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(1)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: Request an attachment instance Mockito.when the plugin returns a correct id
	@Test
	public void testRequestAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(attachmentPlugin.requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
		
	    //exercise
	    String returnedInstanceId = this.localCloudConnector.requestInstance(order);
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(1)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: Request a volume instance Mockito.when the plugin returns a correct id
	@Test
	public void testRequestVolumeInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(volumePlugin.requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
	    
	    //exercise
	    String returnedInstanceId = this.localCloudConnector.requestInstance(order);
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
		Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(1)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: Request a network instance Mockito.when the plugin returns a correct id
	@Test
	public void testRequestNetworkInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    Mockito.when(networkPlugin.requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
	    
	    //exercise
	    String returnedInstanceId = this.localCloudConnector.requestInstance(order);
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(1)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: If plugin returns a null instance id, the method requestInstance() must throw an exception
	@Test(expected = UnexpectedException.class)
	public void testExceptionNullComputeInstanceId() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(computePlugin.requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class))).thenReturn(null);
		
		// exercise
	    this.localCloudConnector.requestInstance(order);
	    
	    // verify
		Mockito.verify(computePlugin, times(1)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: If plugin returns a null instance id, the method requestInstance() must throw an exception
	@Test(expected = UnexpectedException.class)
	public void testExceptionNullNetworkInstanceId() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    Mockito.when(networkPlugin.requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class))).thenReturn(null);
		
	    // exercise
	    this.localCloudConnector.requestInstance(order);
	    
	    // verify
		Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(1)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: If plugin returns a null instance id, the method requestInstance() must throw an exception
	@Test(expected = UnexpectedException.class)
	public void testExceptionNullAttachmentInstanceId() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(attachmentPlugin.requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class))).thenReturn(null);
		
	    // exercise
	    this.localCloudConnector.requestInstance(order);
		
	    // verify
	    Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(1)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: If plugin returns a null instance id, the method requestInstance() must throw an exception
	@Test(expected = UnexpectedException.class)
	public void testExceptionNullVolumeInstanceId() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(volumePlugin.requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class))).thenReturn(null);
		
	    // exercise
	    this.localCloudConnector.requestInstance(order);
		
	    // verify
	    Mockito.verify(computePlugin, times(0)).requestInstance(Mockito.any(ComputeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(1)).requestInstance(Mockito.any(VolumeOrder.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).requestInstance(Mockito.any(AttachmentOrder.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).requestInstance(Mockito.any(NetworkOrder.class), Mockito.any(Token.class));
	}
	
	// test case: The order has an InstanceID, so the method getResourceInstance() is called.
	@Test
	public void testGetNetworkInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    Mockito.when(networkPlugin.getInstance(Mockito.any(String.class), Mockito.any(Token.class))).thenReturn(this.networkInstance);
	    
	    // exercise
	    String returnedInstanceId = this.localCloudConnector.getInstance(order).getId();
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(1)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order has an InstanceID, so the method getResourceInstance() is called.
	@Test
	public void testGetVolumeInstance() throws FogbowManagerException, UnexpectedException {
		
		//set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    Mockito.when(volumePlugin.getInstance(Mockito.any(String.class), Mockito.any(Token.class))).thenReturn(this.volumeInstance);
	    
	    // exercise
	    String returnedInstanceId = this.localCloudConnector.getInstance(order).getId();
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(1)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order has an InstanceID, so the method getResourceInstance() is called.
	@Test
	public void testGetAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    Mockito.when(attachmentPlugin.getInstance(Mockito.any(String.class), Mockito.any(Token.class))).thenReturn(this.attachmentInstance);
		
	    // exercise
	    String returnedInstanceId = this.localCloudConnector.getInstance(order).getId();
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(1)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order has an InstanceID, so the method getResourceInstance() is called.
	// addReverseTunnelInfo() is called in this case.
	@Test
	public void testGetComputeInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		
		// Avoid to test addReverseTunnelInfoMethod behaviour 
		LocalCloudConnector localCloudConnectorSpy = Mockito.spy(this.localCloudConnector);
		Mockito.doNothing().when(localCloudConnectorSpy).addReverseTunnelInfo(Mockito.any(String.class), Mockito.any(ComputeInstance.class));

		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    Mockito.when(computePlugin.getInstance(Mockito.any(String.class), Mockito.any(Token.class))).thenReturn(this.computeInstance);
	        
	    // exercise
	    String returnedInstanceId = localCloudConnectorSpy.getInstance(order).getId();
	    
	    // verify
	    Assert.assertEquals(FAKE_INSTANCE_ID, returnedInstanceId);
		Mockito.verify(computePlugin, times(1)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: If order instance is CLOSED, an exception must be throw
	@Test(expected = org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException.class)
	public void testGetInstanceWithClosedOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
		Mockito.when(this.order.getOrderState()).thenReturn(OrderState.CLOSED);
		
		//exercise
		this.localCloudConnector.getInstance(order);
		
		// verify
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: If order instance is DEACTIVATED, an exception must be throw
	@Test(expected = org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException.class)
	public void testGetInstanceWithDeactivatedOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
		Mockito.when(this.order.getOrderState()).thenReturn(OrderState.DEACTIVATED);
		
		// exercise
		this.localCloudConnector.getInstance(order);
		
		// verify
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty NetworkInstance is returned with the same id of order.
	// The order state is OPEN, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyNetworkInstanceWithOpenOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
		Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
		Mockito.when(this.order.getInstanceId()).thenReturn(null);
		Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		Mockito.when(this.order.getOrderState()).thenReturn(OrderState.OPEN);
		
		// exercise
		Instance instance = this.localCloudConnector.getInstance(order);
		
		//verify
		Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
		Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty NetworkInstance is returned with the same id of order.
	// The order state is PENDING, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyNetworkInstanceWithPendingOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
		Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
		Mockito.when(this.order.getInstanceId()).thenReturn(null);
		Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		Mockito.when(this.order.getOrderState()).thenReturn(OrderState.PENDING);
		
		// exercise
		Instance instance = this.localCloudConnector.getInstance(order);
				
		//verify
		Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
		Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty NetworkInstance is returned with the same id of order.
	// The order state is FAILED, so the instance state must be FAILED.
	@Test
	public void testGetEmptyNetworkInstanceWithFailedOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
		Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
		Mockito.when(this.order.getInstanceId()).thenReturn(null);
		Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		Mockito.when(this.order.getOrderState()).thenReturn(OrderState.FAILED);
		
		// exercise
		Instance instance = this.localCloudConnector.getInstance(order);
						
		//verify
		Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
		Assert.assertEquals(InstanceState.FAILED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty VolumeInstance is returned with the same id of order.
	// The order state is OPEN, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyVolumeInstanceWithOpenOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.OPEN);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty VolumeInstance is returned with the same id of order.
	// The order state is PENDING, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyVolumeInstanceWithPendingOrder() throws FogbowManagerException, UnexpectedException {
		
		//set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.PENDING);
	     
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty VolumeInstance is returned with the same id of order.
	// The order state is FAILED, so the instance state must be FAILED.
	@Test
	public void testGetEmptyVolumeInstanceWithFailedOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.FAILED);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.FAILED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty AttachmentInstance is returned with the same id of order.
	// The order state is OPEN, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyAttachmentInstanceWithOpenOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.OPEN);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty AttachmentInstance is returned with the same id of order.
	// The order state is PENDING, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyAttachmentInstanceWithPendingOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.PENDING);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty AttachmentInstance is returned with the same id of order.
	// The order state is FAILED, so the instance state must be FAILED.
	@Test
	public void testGetEmptyAttachmentInstanceWithFailedOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.FAILED);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.FAILED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty ComputeInstance is returned with the same id of order.
	// The order state is OPEN, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyComputeInstanceWithOpenOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.OPEN);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());	
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty ComputeInstance is returned with the same id of order.
	// The order state is PENDING, so the instance state must be DISPATCHED.
	@Test
	public void testGetEmptyComputeInstanceWithPendingOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.PENDING);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 	 	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.DISPATCHED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: The order doesn't have an InstanceID, so an empty ComputeInstance is returned with the same id of order.
	// The order state is FAILED, so the instance state must be FAILED.
	@Test
	public void testGetEmptyComputeInstanceWithFailedOrder() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(this.order.getInstanceId()).thenReturn(null);
	    Mockito.when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
	    Mockito.when(this.order.getOrderState()).thenReturn(OrderState.FAILED);
	    
	    // exercise
	 	Instance instance = this.localCloudConnector.getInstance(order);
	 	 	 	 	 	 						
	 	//verify
	 	Assert.assertEquals(FAKE_ORDER_ID, instance.getId());
	 	Assert.assertEquals(InstanceState.FAILED, instance.getState());
		Mockito.verify(computePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).getInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Try to delete an instance without instance id. Nothing happens
	@Test
	public void testDeleteInstanceWithoutInstanceID() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    
	    // exercise
	    this.localCloudConnector.deleteInstance(order);
	    
	    // verify
	    Mockito.verify(computePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Deleting a compute instance with ID. Compute plugin must be called.
	@Test
	public void testDeleteComputeInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(ComputeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    
	    // exercise
		this.localCloudConnector.deleteInstance(order);
		
		// verify
		Mockito.verify(computePlugin, times(1)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Deleting a volume instance with ID. Volume plugin must be called.
	@Test
	public void testDeleteVolumeInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(VolumeOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    
	    // exercise
		this.localCloudConnector.deleteInstance(order);
		
		// verify
		Mockito.verify(computePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(1)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Deleting a network instance with ID. Network plugin must be called.
	@Test
	public void testDeleteNetworkInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(NetworkOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    
	    // exercise
		this.localCloudConnector.deleteInstance(order);
		
		// verify
		Mockito.verify(computePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(1)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Deleting a attachment instance with ID. Attachment plugin must be called.
	@Test
	public void testDeleteAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		
		// set up
		this.order = Mockito.mock(AttachmentOrder.class);
	    Mockito.when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    Mockito.when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    
	    // exercise
		this.localCloudConnector.deleteInstance(order);
		
		// verify
		Mockito.verify(computePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(volumePlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(attachmentPlugin, times(1)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	    Mockito.verify(networkPlugin, times(0)).deleteInstance(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Getting an image. Image plugin must be called
	@Test
	public void testGetImage() throws FogbowManagerException, UnexpectedException {
		
		// set up
	    Mockito.when(this.imagePlugin.getImage(Mockito.any(String.class), Mockito.any(Token.class))).thenReturn(this.image);
	    
	    // exercise
	    String returnedImageId = this.localCloudConnector.getImage(FAKE_IMAGE_ID, federationUser).getId();
	    
	    // verify
		Assert.assertEquals(FAKE_IMAGE_ID, returnedImageId);
		Mockito.verify(imagePlugin, times(1)).getImage(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	// test case: Getting a null image. Image plugin must be called
	@Test
	public void testGetNullImage() throws FogbowManagerException, UnexpectedException {
		
		// set up
	    Mockito.when(this.imagePlugin.getImage(Mockito.any(String.class), Mockito.any(Token.class))).thenReturn(null);
	    
	    // exercise
	    Image image = this.localCloudConnector.getImage(FAKE_IMAGE_ID, federationUser);
	    
	    // verify
		Assert.assertNull(image);
		Mockito.verify(imagePlugin, times(1)).getImage(Mockito.any(String.class), Mockito.any(Token.class));
	}
	
	
	// test case: Getting user compute quota. Compute quota plugin must be called.
	@Test
	public void testGetUserComputeQuota() throws FogbowManagerException, UnexpectedException {
		
		// set up
		ComputeAllocation fakeTotalComputeAllocation = new ComputeAllocation(VCPU_TOTAL, RAM_TOTAL, INSTANCES_TOTAL);
		ComputeAllocation fakeUsedComputeAllocation = new ComputeAllocation(VCPU_USED, RAM_USED, INSTANCES_USED);
		ComputeQuota fakeComputeQuota = new ComputeQuota(fakeTotalComputeAllocation, fakeUsedComputeAllocation);
	    Mockito.when(this.computeQuotaPlugin.getUserQuota(Mockito.any(Token.class))).thenReturn(fakeComputeQuota);
	    
	    // exercise
	    ComputeQuota quota = (ComputeQuota) this.localCloudConnector.getUserQuota(federationUser, InstanceType.COMPUTE);
	    
	    // verify
	    Assert.assertEquals(VCPU_TOTAL, quota.getTotalQuota().getvCPU());
	    Assert.assertEquals(RAM_TOTAL, quota.getTotalQuota().getRam());
	    Assert.assertEquals(INSTANCES_TOTAL, quota.getTotalQuota().getInstances());
	    Assert.assertEquals(VCPU_USED, quota.getUsedQuota().getvCPU());
	    Assert.assertEquals(RAM_USED, quota.getUsedQuota().getRam());
	    Assert.assertEquals(INSTANCES_USED, quota.getUsedQuota().getInstances());
	    Mockito.verify(computeQuotaPlugin, times(1)).getUserQuota(Mockito.any(Token.class));
	}
	
	// test case: If the instance type isn't of Compute type, an exception must be throw
	@Test(expected = UnexpectedException.class)
	public void testGetUserVolumeQuotaException() throws FogbowManagerException, UnexpectedException { 
		
		// exercise
	    this.localCloudConnector.getUserQuota(federationUser, InstanceType.VOLUME);
	    
	    // verify
	    Mockito.verify(computeQuotaPlugin, times(0)).getUserQuota(Mockito.any(Token.class));
	
	}
	
	
	// test case: If the instance type isn't of Compute type, an exception must be throw
	@Test(expected = UnexpectedException.class)
	public void testGetUserAttachmentQuotaException() throws FogbowManagerException, UnexpectedException { 
		
		// exercise
	    this.localCloudConnector.getUserQuota(federationUser, InstanceType.ATTACHMENT);
	    
	    // verify
	    Mockito.verify(computeQuotaPlugin, times(0)).getUserQuota(Mockito.any(Token.class));
	
	}
	
	
	// test case: If the instance type isn't of Compute type, an exception must be throw
	@Test(expected = UnexpectedException.class)
	public void testGetUserNetworkQuotaException() throws FogbowManagerException, UnexpectedException {
		
		// exercise
	    this.localCloudConnector.getUserQuota(federationUser, InstanceType.NETWORK);
	    
	    // verify
	    Mockito.verify(computeQuotaPlugin, times(0)).getUserQuota(Mockito.any(Token.class));
	}
	
	// test case: Getting all images. Image plugin must be called
	@Test
	public void testGetAllImages() throws FogbowManagerException, UnexpectedException {
		
		// set up
	    Map<String, String> fakeMapImages = new HashMap<>();
	    fakeMapImages.put(FAKE_IMAGE_ID, FAKE_IMAGE_NAME);
	    Mockito.when(this.imagePlugin.getAllImages(Mockito.any(Token.class))).thenReturn(fakeMapImages);
	    
	    // exercise
	    Map<String, String> returnedImages = this.localCloudConnector.getAllImages(federationUser);
	    
	    // verify
	    Assert.assertEquals(FAKE_IMAGE_NAME, returnedImages.get(FAKE_IMAGE_ID));
	    Assert.assertEquals(1, returnedImages.size());
	    Mockito.verify(imagePlugin, times(1)).getAllImages(Mockito.any(Token.class));
	}
	
	
	// test case: The return of getAllImages must be null. Image plugin must be called.
	@Test
	public void testGetAllImagesNullReturn() throws FogbowManagerException, UnexpectedException {
		
		// set up
	    Mockito.when(this.imagePlugin.getAllImages(Mockito.any(Token.class))).thenReturn(null);
	    
	    // exercise
	    Map<String, String> returnedImages = this.localCloudConnector.getAllImages(federationUser);
	    
	    // verify
	    Assert.assertNull(returnedImages);
	    Mockito.verify(imagePlugin, times(1)).getAllImages(Mockito.any(Token.class));
	}
	
	
	

}
