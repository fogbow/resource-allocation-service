package org.fogbowcloud.manager.core.cloudconnector;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceNotFoundException;

import org.apache.commons.collections.map.HashedMap;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import junit.framework.Assert;

import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
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
import org.fogbowcloud.manager.core.models.quotas.Quota;
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
		
		DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FAILED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList());

        PowerMockito.mockStatic(DatabaseManager.class);
        given(DatabaseManager.getInstance()).willReturn(databaseManager);
		
        this.federationUser = Mockito.mock(FederationUser.class);
        this.aaController = Mockito.mock(AaController.class);
        this.cloudPluginsHolder = Mockito.mock(CloudPluginsHolder.class);
        
        this.computePlugin = Mockito.mock(ComputePlugin.class);
        this.attachmentPlugin = Mockito.mock(AttachmentPlugin.class);
        this.networkPlugin = Mockito.mock(NetworkPlugin.class);
        this.volumePlugin = Mockito.mock(VolumePlugin.class);
        this.imagePlugin = Mockito.mock(ImagePlugin.class);
        this.computeQuotaPlugin = Mockito.mock(ComputeQuotaPlugin.class);
        
        this.networkInstance = Mockito.mock(NetworkInstance.class);
        when(networkInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        
        this.volumeInstance = Mockito.mock(VolumeInstance.class);
        when(volumeInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        
        this.attachmentInstance = Mockito.mock(AttachmentInstance.class);
        when(attachmentInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        
        this.computeInstance = Mockito.mock(ComputeInstance.class);
        when(computeInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
       
        this.image = Mockito.mock(Image.class);
        when(image.getId()).thenReturn(FAKE_IMAGE_ID);
        
        when(cloudPluginsHolder.getComputePlugin()).thenReturn(computePlugin);
        when(cloudPluginsHolder.getAttachmentPlugin()).thenReturn(attachmentPlugin);
        when(cloudPluginsHolder.getNetworkPlugin()).thenReturn(networkPlugin);
        when(cloudPluginsHolder.getVolumePlugin()).thenReturn(volumePlugin);
        when(cloudPluginsHolder.getImagePlugin()).thenReturn(imagePlugin);
        when(cloudPluginsHolder.getComputeQuotaPlugin()).thenReturn(computeQuotaPlugin);
        when(aaController.getLocalToken(any(FederationUser.class))).thenReturn(new Token());
        
        this.localCloudConnector = new LocalCloudConnector(aaController, cloudPluginsHolder);
        
    }
	
	@Test
	public void testRequestComputeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(ComputeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    when(computePlugin.requestInstance(any(ComputeOrder.class), any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.requestInstance(order));
	}
	
	@Test
	public void testRequestAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(AttachmentOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    when(attachmentPlugin.requestInstance(any(AttachmentOrder.class), any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.requestInstance(order));
	}
	
	@Test
	public void testRequestVolumeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(VolumeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    when(volumePlugin.requestInstance(any(VolumeOrder.class), any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.requestInstance(order));
	}
	
	@Test
	public void testRequestNetworkInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(NetworkOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    when(networkPlugin.requestInstance(any(NetworkOrder.class), any(Token.class))).thenReturn(FAKE_INSTANCE_ID);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.requestInstance(order));
	}
	
	// If plugin returns a null instance id, the method requestInstance() must throw an exception
	@Test(expected = UnexpectedException.class)
	public void testExceptionNullInstanceId() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(ComputeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    when(computePlugin.requestInstance(any(ComputeOrder.class), any(Token.class))).thenReturn(null);
		this.localCloudConnector.requestInstance(order);
	}
	
	
	
	// The order has an InstanceID, so the method getResourceInstance() is called.
	@Test
	public void testGetNetworkInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(NetworkOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    when(networkPlugin.getInstance(any(String.class), any(Token.class))).thenReturn(this.networkInstance);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	// The order has an InstanceID, so the method getResourceInstance() is called.
	@Test
	public void testGetVolumeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(VolumeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    when(volumePlugin.getInstance(any(String.class), any(Token.class))).thenReturn(this.volumeInstance);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	// The order has an InstanceID, so the method getResourceInstance() is called.
	@Test
	public void testGetAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(AttachmentOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    when(attachmentPlugin.getInstance(any(String.class), any(Token.class))).thenReturn(this.attachmentInstance);
		assertEquals(FAKE_INSTANCE_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	// The order has an InstanceID, so the method getResourceInstance() is called.
	// addReverseTunnelInfo() is called in this case.
	@Test
	public void testGetComputeInstance() throws FogbowManagerException, UnexpectedException {
		
		LocalCloudConnector localCloudConnectorSpy = Mockito.spy(this.localCloudConnector);
		Mockito.doNothing().when(localCloudConnectorSpy).addReverseTunnelInfo(any(String.class), any(ComputeInstance.class));

		this.order = Mockito.mock(ComputeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
	    when(computePlugin.getInstance(any(String.class), any(Token.class))).thenReturn(this.computeInstance);
		assertEquals(FAKE_INSTANCE_ID, localCloudConnectorSpy.getInstance(order).getId());
	}
	
	
	// The order doesn't have an InstanceID, so an empty NetworkInstance is returned with the same id of order.
	@Test
	public void testGetEmptyNetworkInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(NetworkOrder.class);
		when(this.order.getType()).thenReturn(InstanceType.NETWORK);
		when(this.order.getInstanceId()).thenReturn(null);
		when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		assertEquals(FAKE_ORDER_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	// The order doesn't have an InstanceID, so an empty VolumeInstance is returned with the same id of order.
	@Test
	public void testGetEmptyVolumeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(VolumeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    when(this.order.getInstanceId()).thenReturn(null);
	    when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		assertEquals(FAKE_ORDER_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	// The order doesn't have an InstanceID, so an empty AttachmentInstance is returned with the same id of order.
	@Test
	public void testGetEmptyAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(AttachmentOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    when(this.order.getInstanceId()).thenReturn(null);
	    when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		assertEquals(FAKE_ORDER_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	// The order doesn't have an InstanceID, so an empty ComputeInstance is returned with the same id of order.
	@Test
	public void testGetEmptyComputeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(ComputeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    when(this.order.getInstanceId()).thenReturn(null);
	    when(this.order.getId()).thenReturn(FAKE_ORDER_ID);
		assertEquals(FAKE_ORDER_ID, this.localCloudConnector.getInstance(order).getId());
	}
	
	@Test
	public void testDeleteInstanceWithoutInstanceID() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(ComputeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    this.localCloudConnector.deleteInstance(order);
	}
	
	@Test
	public void testDeleteComputeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(ComputeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.COMPUTE);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
		this.localCloudConnector.deleteInstance(order);
	}
	
	@Test
	public void testDeleteVolumeInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(VolumeOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.VOLUME);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
		this.localCloudConnector.deleteInstance(order);
	}
	
	@Test
	public void testDeleteNetworkInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(NetworkOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.NETWORK);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
		this.localCloudConnector.deleteInstance(order);
	}
	
	@Test
	public void testDeleteAttachmentInstance() throws FogbowManagerException, UnexpectedException {
		this.order = Mockito.mock(AttachmentOrder.class);
	    when(this.order.getType()).thenReturn(InstanceType.ATTACHMENT);
	    when(this.order.getInstanceId()).thenReturn(FAKE_INSTANCE_ID);
		this.localCloudConnector.deleteInstance(order);
	}
	
	@Test
	public void testGetImage() throws FogbowManagerException, UnexpectedException {
	    when(this.imagePlugin.getImage(any(String.class), any(Token.class))).thenReturn(this.image);
		assertEquals(FAKE_IMAGE_ID, this.localCloudConnector.getImage(FAKE_IMAGE_ID, federationUser).getId());
	}
	
	@Test
	public void testGetNullImage() throws FogbowManagerException, UnexpectedException {
	    when(this.imagePlugin.getImage(any(String.class), any(Token.class))).thenReturn(null);
		assertNull(this.localCloudConnector.getImage(FAKE_IMAGE_ID, federationUser));
	}
	
	@Test
	public void testGetUserComputeQuota() throws FogbowManagerException, UnexpectedException {
		
		ComputeAllocation fakeTotalComputeAllocation = new ComputeAllocation(VCPU_TOTAL, RAM_TOTAL, INSTANCES_TOTAL);
		ComputeAllocation fakeUsedComputeAllocation = new ComputeAllocation(VCPU_USED, RAM_USED, INSTANCES_USED);
		ComputeQuota fakeComputeQuota = new ComputeQuota(fakeTotalComputeAllocation, fakeUsedComputeAllocation);
	    when(this.computeQuotaPlugin.getUserQuota(any(Token.class))).thenReturn(fakeComputeQuota);
	    
	    ComputeQuota quota = (ComputeQuota) this.localCloudConnector.getUserQuota(federationUser, InstanceType.COMPUTE);
	    assertEquals(VCPU_TOTAL, quota.getTotalQuota().getvCPU());
	    assertEquals(RAM_TOTAL, quota.getTotalQuota().getRam());
	    assertEquals(INSTANCES_TOTAL, quota.getTotalQuota().getInstances());
	    
	    assertEquals(VCPU_USED, quota.getUsedQuota().getvCPU());
	    assertEquals(RAM_USED, quota.getUsedQuota().getRam());
	    assertEquals(INSTANCES_USED, quota.getUsedQuota().getInstances());
	}
	
	// If the instance type isn't of Compute type, an exception must be throw
	@Test(expected = UnexpectedException.class)
	public void testGetUserVolumeQuotaException() throws FogbowManagerException, UnexpectedException {    
	    this.localCloudConnector.getUserQuota(federationUser, InstanceType.VOLUME);
	
	}
	
	
	// If the instance type isn't of Compute type, an exception must be throw
	@Test(expected = UnexpectedException.class)
	public void testGetUserAttachmentQuotaException() throws FogbowManagerException, UnexpectedException {    
	    this.localCloudConnector.getUserQuota(federationUser, InstanceType.ATTACHMENT);
	
	}
	
	
	// If the instance type isn't of Compute type, an exception must be throw
	@Test(expected = UnexpectedException.class)
	public void testGetUserNetworkQuotaException() throws FogbowManagerException, UnexpectedException {    
	    this.localCloudConnector.getUserQuota(federationUser, InstanceType.NETWORK);
	}
	
	@Test
	public void testGetAllImages() throws FogbowManagerException, UnexpectedException {    
	    Map<String, String> fakeMapImages = new HashMap<>();
	    fakeMapImages.put(FAKE_IMAGE_ID, FAKE_IMAGE_NAME);
	    when(this.imagePlugin.getAllImages(any(Token.class))).thenReturn(fakeMapImages);
	    
	    assertEquals(FAKE_IMAGE_NAME, this.localCloudConnector.getAllImages(federationUser).get(FAKE_IMAGE_ID));
	    assertEquals(1, this.localCloudConnector.getAllImages(federationUser).size());
	}
	
	@Test
	public void testGetAllImagesNullReturn() throws FogbowManagerException, UnexpectedException {    
	    when(this.imagePlugin.getAllImages(any(Token.class))).thenReturn(null);
	    assertNull(this.localCloudConnector.getAllImages(federationUser));
	}
	
	
	

}
