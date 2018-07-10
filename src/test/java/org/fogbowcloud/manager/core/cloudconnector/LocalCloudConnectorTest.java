package org.fogbowcloud.manager.core.cloudconnector;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;


@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalCloudConnector.class})
public class LocalCloudConnectorTest extends BaseUnitTests {
	
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	
	private LocalCloudConnector localCloudConnector;
	
	private AaController aaController;
	private CloudPluginsHolder cloudPluginsHolder;

	private ComputePlugin computePlugin;
	private AttachmentPlugin attachmentPlugin;
	private NetworkPlugin networkPlugin;
	private VolumePlugin volumePlugin;
	
	private Order order;
	
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
		
        this.aaController = Mockito.mock(AaController.class);
        this.cloudPluginsHolder = Mockito.mock(CloudPluginsHolder.class);
        
        this.computePlugin = Mockito.mock(ComputePlugin.class);
        this.attachmentPlugin = Mockito.mock(AttachmentPlugin.class);
        this.networkPlugin = Mockito.mock(NetworkPlugin.class);
        this.volumePlugin = Mockito.mock(VolumePlugin.class);
        
        when(cloudPluginsHolder.getComputePlugin()).thenReturn(computePlugin);
        when(cloudPluginsHolder.getAttachmentPlugin()).thenReturn(attachmentPlugin);
        when(cloudPluginsHolder.getNetworkPlugin()).thenReturn(networkPlugin);
        when(cloudPluginsHolder.getVolumePlugin()).thenReturn(volumePlugin);
        when(aaController.getLocalToken(any(FederationUser.class))).thenReturn(new Token());
        
        this.localCloudConnector = new LocalCloudConnector(aaController, cloudPluginsHolder);
        
    }
	
	@Test
	@Ignore
	public void testRequestInstanceFromNullOrder() throws FogbowManagerException, UnexpectedException {
		this.order = null;
		assertNull(this.localCloudConnector.requestInstance(order));
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
	    when(networkPlugin.requestInstance(any(NetworkOrder.class), any(Token.class))).thenReturn(null);
		this.localCloudConnector.requestInstance(order);
	}
	
	
	
	
	

}
