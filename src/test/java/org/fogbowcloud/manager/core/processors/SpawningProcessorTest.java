package org.fogbowcloud.manager.core.processors;

import org.fogbowcloud.manager.core.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.util.connectivity.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.util.connectivity.SshConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.TunnelingServiceUtil;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class SpawningProcessorTest extends BaseUnitTests {

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_SOURCE = "fake-source";
    private static final String FAKE_TARGET = "fake-target";
    private static final String FAKE_DEVICE = "fake-device";
    
    private ChainedList failedOrderList;
    private ChainedList fulfilledOrderList;
    private ChainedList openOrderList;
    private ChainedList spawningOrderList;
    private CloudConnector cloudConnector;
    private SpawningProcessor spawningProcessor;
    private SshConnectivityUtil sshConnectivity;
    private Thread thread;
    private TunnelingServiceUtil tunnelingService;

    @Before
    public void setUp() {
        super.mockDB();

        HomeDir.getInstance().setPath(TEST_PATH);

        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(
                Mockito.anyString())).thenReturn(localCloudConnector);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(
                BaseUnitTests.LOCAL_MEMBER_ID);

        this.spawningProcessor = Mockito.spy(new SpawningProcessor(
                BaseUnitTests.LOCAL_MEMBER_ID, this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
        this.openOrderList = sharedOrderHolders.getOpenOrdersList();
        
        this.thread = null;
    }
    
    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }
    
    @Test(expected = UnexpectedException.class)
    public void testThrowUnexpectedExceptionWhileTryingToProcessOrder()
            throws UnexpectedException, Exception {

        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        Mockito.doThrow(new UnexpectedException()).when(this.spawningProcessor)
                .processSpawningOrder(order);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();

        Thread.sleep(500);

        this.spawningProcessor.processSpawningOrder(order);
    }
    
    @Test
    public void testTryingProcessComputeOrderNotSpawning() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.OPEN);
        this.openOrderList.addItem(order);
        
        this.spawningProcessor.processSpawningOrder(order);

        Assert.assertNotNull(this.openOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }
    
    @Test
    public void testRunProcessComputeOrderWithoutSSHConnectivity() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        
        Mockito.doReturn(false).when(order).isRequesterRemote(Mockito.anyString());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(
                Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(
                Mockito.anyString())).thenReturn(new HashMap<>());
        
        Mockito.when(this.sshConnectivity.checkSSHConnectivity(
                Mockito.any(SshTunnelConnectionData.class))).thenReturn(false);
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNotNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }
    
    @Test
    public void testRunProcessComputeOrderWithoutExternalAddresses() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        
        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(
                Mockito.anyString())).thenReturn(null);
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNotNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }
    
    @Test
    public void testRunProcessWhenOrderTypeIsNotComputer() throws Exception {
        Order order = spyAnoterInstanceType();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Mockito.doReturn(false).when(order).isRequesterRemote(Mockito.anyString());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(
                Mockito.any(Order.class));

        Assert.assertNull(this.fulfilledOrderList.getNext());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }
    
    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsNotReady() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Mockito.doReturn(false).when(order).isRequesterRemote(Mockito.anyString());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.DISPATCHED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNotNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }
    
    @Test
    public void testRunProcessComputeOrderInstanceReachable() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        
        Mockito.doReturn(false).when(order).isRequesterRemote(Mockito.anyString());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(
                Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(
                Mockito.eq(order.getId()))).thenReturn(new HashMap<>());

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(
                Mockito.any(SshTunnelConnectionData.class))).thenReturn(true);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsInactive() throws InterruptedException {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        ComputeInstance computeOrderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        computeOrderInstance.setState(InstanceState.INACTIVE);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Assert.assertNull(this.fulfilledOrderList.getNext());
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Order test = this.spawningOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
    }

    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsFailed() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Mockito.doReturn(false).when(order).isRequesterRemote(Mockito.anyString());
        
        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);
        
        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(
                Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(
                Mockito.eq(order.getId()))).thenReturn(new HashMap<>());

        Assert.assertNull(this.failedOrderList.getNext());
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
    }

    @Test
    public void testRunProcessComputeOrderWhenOrderStateIsNotSpawning() throws Exception {
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.OPEN);
        this.openOrderList.addItem(order);
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Order test = this.openOrderList.getNext();
        Assert.assertEquals(OrderState.OPEN, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    private Order spyComputeOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        UserData userData = Mockito.mock(UserData.class);
        
        Order order = Mockito.spy(
                new ComputeOrder(
                        federationUser, 
                        requestingMember, 
                        providingMember, 
                        8, 
                        1024,
                        30, 
                        FAKE_IMAGE_NAME, 
                        userData, 
                        FAKE_PUBLIC_KEY, 
                        null));
        
        return order;
    }
    
    private Order spyAnoterInstanceType() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;

        Order order = Mockito.spy(
                new AttachmentOrder(
                        federationUser, 
                        requestingMember,
                        providingMember, 
                        FAKE_SOURCE, 
                        FAKE_TARGET, 
                        FAKE_DEVICE));
        
        return order;
    }
    
}
