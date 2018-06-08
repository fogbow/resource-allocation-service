package org.fogbowcloud.manager.core.processors;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.BehaviorPluginsHolder;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SpawningProcessorTest extends BaseUnitTests {

    private CloudConnector cloudConnector;
    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;
    private SpawningProcessor spawningProcessor;
    private Thread thread;
    private ChainedList spawningOrderList;
    private ChainedList fulfilledOrderList;
    private ChainedList failedOrderList;
    private ChainedList openOrderList;
    private ChainedList pendingOrderList;
    private ChainedList closedOrderList;
    private BehaviorPluginsHolder behaviorPluginsHolder;
    private LocalIdentityPlugin localIdentityPlugin;
    private AaController aaController;
    private OrderController orderController;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/main/resources");
        
        initServiceConfig();
        
        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);
        
        this.cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID);

        this.spawningProcessor = Mockito.spy(
                new SpawningProcessor(BaseUnitTests.LOCAL_MEMBER_ID, this.tunnelingService, this.sshConnectivity,
                        DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME));
        
        this.thread = null;

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
        this.openOrderList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrderList = sharedOrderHolders.getPendingOrdersList();
        this.closedOrderList = sharedOrderHolders.getClosedOrdersList();
    }
    
    private void initServiceConfig() {
        PluginInstantiationService instantiationInitService = PluginInstantiationService.getInstance();

        this.behaviorPluginsHolder = new BehaviorPluginsHolder(instantiationInitService);
        this.behaviorPluginsHolder.getLocalUserCredentialsMapperPlugin();

        this.aaController = new AaController(this.localIdentityPlugin, this.behaviorPluginsHolder);
        this.orderController = new OrderController(getLocalMemberId());

        CloudPluginsHolder cloudPluginsHolder = new CloudPluginsHolder(instantiationInitService);
        CloudConnectorFactory.getInstance().setCloudPluginsHolder(cloudPluginsHolder);
        CloudConnectorFactory.getInstance().setLocalMemberId(getLocalMemberId());
        CloudConnectorFactory.getInstance().setAaController(this.aaController);
        CloudConnectorFactory.getInstance().setOrderController(this.orderController);
//        CloudConnectorFactory.getInstance().setPacketSender(xmppComponentManager);
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    @Test
    public void testRunThrowableExceptionWhileTryingToProcessOrder() throws Exception {
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.spawningOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException("Any Exception")).when(this.spawningProcessor)
                .processSpawningOrder(order);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();

        Thread.sleep(500);
    }

    @Test
    public void testRunProcesseComputeOrderInstanceActive() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        String instanceId = "fake-id";
//        Instance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        Instance orderInstance = this.cloudConnector.getInstance(order); 
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

//        Mockito.doReturn(orderInstance).when(this.cloudConnector)
//                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
				.thenReturn(new HashMap<>());

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
        		SshTunnelConnectionData.class))).thenReturn(true);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.fulfilledOrderList.getNext();
        assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    @Test
    public void testRunProcesseComputeOrderInstanceInactive() throws InterruptedException {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        String instanceId = "fake-id";
        ComputeInstance computeOrderInstance =
                Mockito.spy(new ComputeInstance(instanceId));
        computeOrderInstance.setState(InstanceState.INACTIVE);
        order.setInstanceId(instanceId);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Order test = this.spawningOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
    }

    @Test
    public void testRunProcesseComputeOrderInstanceFailed() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        
//        this.localUserCredentialsMapperPlugin = behaviorPluginsHolder.getLocalUserCredentialsMapperPlugin();

        String instanceId = "fake-id";
//      Instance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        Instance orderInstance = this.cloudConnector.getInstance(order); 
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(instanceId);
        
//        Mockito.doReturn(orderInstance).when(this.cloudConnector)
//                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
				.thenReturn(new HashMap<>());

        Assert.assertNull(this.failedOrderList.getNext());
        
        this.spawningProcessor.processSpawningOrder(order);

//        this.thread = new Thread(this.spawningProcessor);
//        this.thread.start();
//        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
    }

    @Test
    public void testProcessWithoutModifyOpenOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.OPEN);
        this.openOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.openOrderList.getNext();
        Assert.assertEquals(OrderState.OPEN, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyPendingOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.PENDING);
        this.pendingOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.pendingOrderList.getNext();
        Assert.assertEquals(OrderState.PENDING, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyFailedOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FAILED);
        this.failedOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.failedOrderList.getNext();
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyFulfilledOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyClosedOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.CLOSED);
        this.closedOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.closedOrderList.getNext();
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testRunThrowableExceptionWhileTryingToGetMapAddressesOfComputeOrderInstance()
            throws InterruptedException {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        String instanceId = "fake-id";
        ComputeInstance computeOrderInstance =
                Mockito.spy(new ComputeInstance(instanceId));
        computeOrderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.doThrow(new RuntimeException("Any Exception")).when(this.tunnelingService)
                .getExternalServiceAddresses(order.getId());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);
    }

    @Test
    public void testRunConfiguredToFailedAttemptSshConnectivity() throws InterruptedException {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        String instanceId = "fake-id";
        ComputeInstance computeOrderInstance =
                Mockito.spy(new ComputeInstance(instanceId));
        computeOrderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
				.thenReturn(new HashMap<>());

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
        		SshTunnelConnectionData.class))).thenReturn(true);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Order test = this.spawningOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
    }

    private Order createOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        UserData userData = Mockito.mock(UserData.class);
        Order order = new ComputeOrder(federationUser, requestingMember, providingMember, 8, 1024,
                30, "fake_image_name", userData, "fake_public_key");
        return order;
    }
}
