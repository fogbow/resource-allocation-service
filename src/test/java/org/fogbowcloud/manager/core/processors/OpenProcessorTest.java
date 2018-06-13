package org.fogbowcloud.manager.core.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.BehaviorPluginsHolder;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.RemoteCloudConnector;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class OpenProcessorTest extends BaseUnitTests {

    private OpenProcessor openProcessor;
    
    private BehaviorPluginsHolder behaviorPluginsHolder;
    private LocalIdentityPlugin localIdentityPlugin;
    private AaController aaController;
    private OrderController orderController;
    private CloudPluginsHolder cloudPluginsHolder;
    
    private LocalCloudConnector localCloudConnector;
    private RemoteCloudConnector remoteCloudConnector;
    private Thread thread;
    
    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        initServiceConfig();
        this.localCloudConnector = Mockito.spy(new LocalCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID, this.aaController, this.orderController, this.cloudPluginsHolder));
        this.remoteCloudConnector = Mockito.mock(RemoteCloudConnector.class);
        this.thread = null;
        this.openProcessor = Mockito.spy(new OpenProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME));
    }
    
    private void initServiceConfig() {
        PluginInstantiationService instantiationInitService = PluginInstantiationService.getInstance();

        this.behaviorPluginsHolder = new BehaviorPluginsHolder(instantiationInitService);
        this.behaviorPluginsHolder.getLocalUserCredentialsMapperPlugin();
        this.localIdentityPlugin = new KeystoneV3IdentityPlugin();
        this.aaController = new AaController(this.localIdentityPlugin, this.behaviorPluginsHolder);
        this.orderController = new OrderController(getLocalMemberId());

        this.cloudPluginsHolder = new CloudPluginsHolder(instantiationInitService);
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

    /**
     * Test if the open processor is setting to spawning an open localidentity order when the request
     * instance method of instance provider returns an instance.
     *
     * @throws Exception
     */
    @Test
    public void testProcessOpenLocalOrder() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        String id = "fake-id";
        Mockito.doReturn(id)
                .when(this.localCloudConnector)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.SPAWNING, localOrder.getOrderState());

        // test if the open order list is empty and 
        // the spawningList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(localOrder, spawningOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to failed an open localidentity order when the request instance
     * method of instance provider returns a null instance.
     *
     * @throws Exception
     */
    @Test
    public void testProcessOpenLocalOrderWithNullInstance() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        Mockito.doReturn(null)
                .when(this.localCloudConnector)
                .requestInstance(Mockito.any(Order.class));

        this.openProcessor.processOpenOrder(localOrder);
        
//        this.thread = new Thread(this.openProcessor);
//        this.thread.start();
//        Thread.sleep(500);

        assertEquals(OrderState.FAILED, localOrder.getOrderState());

        // test if the open order list is empty and the failedList is with the
        // localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertEquals(localOrder, failedOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to failed an open localidentity order when the request instance
     * method of instance provider throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testProcessLocalOpenOrderRequestingException() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.localCloudConnector)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.FAILED, localOrder.getOrderState());

        // test if the open order list is empty and 
        // the failedList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(localOrder, failedOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to pending an open intercomponent order.
     *
     * @throws Exception
     */
    @Test
    public void testProcessOpenRemoteOrder() throws Exception {
        Order remoteOrder = this.createRemoteOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(remoteOrder);

        Mockito.doReturn(null)
                .when(this.remoteCloudConnector)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.PENDING, remoteOrder.getOrderState());

        // test if the open order list is empty and
        // the failedList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(remoteOrder, pendingOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to fail an open intercomponent order when the request instance
     * method of intercomponent instance provider throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testProcessRemoteOpenOrderRequestingException() throws Exception {
        Order remoteOrder = this.createRemoteOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(remoteOrder);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.remoteCloudConnector)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);        	

        assertEquals(OrderState.FAILED, remoteOrder.getOrderState());

        // test if the open order list is empty and
        // the failedList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertEquals(remoteOrder, failedOrdersList.getNext());
    }

    /**
     * Test if the open processor does not process an Order that is not in the open state.
     *
     * @throws InterruptedException
     */
    @Test
    public void testProcessNotOpenOrder() throws InterruptedException, OrderManagementException {
        Order order = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(order);

        order.setOrderState(OrderState.PENDING);

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        assertEquals(OrderState.PENDING, order.getOrderState());
        assertFalse(this.listIsEmpty(openOrdersList));
    }

    /**
     * Test if the open processor still running if it try to process a null order.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessWithNullOpenOrder() throws InterruptedException {
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);
    }

    /**
     * Test if the open processor still running and do not change the order state if the method
     * processOpenOrder throw an order state transition exception.
     *
     * @throws OrderStateTransitionException
     * @throws InterruptedException
     */
    @Test
    public void testProcessOpenOrderThrowingOrderStateTransitionException()
            throws Exception {
        Order order = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(OrderStateTransitionException.class)
                .when(this.openProcessor)
                .processOpenOrder(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        assertEquals(OrderState.OPEN, order.getOrderState());
        assertFalse(this.listIsEmpty(openOrdersList));
    }

    /**
     * Test if the open processor still running and do not change the order state if the method
     * processOpenOrder throw an exception.
     *
     * @throws OrderStateTransitionException
     * @throws InterruptedException
     */
    @Test
    public void testProcessOpenOrderThrowingAnException()
            throws Exception {
        Order order = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(Exception.class)
                .when(this.openProcessor)
                .processOpenOrder(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(order);
        assertEquals(OrderState.OPEN, order.getOrderState());
        assertFalse(this.listIsEmpty(openOrdersList));
    }

    /**
     * This method tests a race condition when this class thread has the order operation priority.
     *
     * @throws Exception
     */
    @Test
    public void testRaceConditionWithThisThreadPriority() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        String id = "fake-id";
        Mockito.doReturn(id)
                .when(this.localCloudConnector)
                .requestInstance(Mockito.any(Order.class));

        synchronized (localOrder) {
            this.thread = new Thread(this.openProcessor);
            this.thread.start();

            Thread.sleep(500);

            assertEquals(OrderState.OPEN, localOrder.getOrderState());
        }

        Thread.sleep(500);

        assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
    }

    /**
     * This method tests a race condition when this class thread has the order operation priority
     * and change the open order to a different state.
     */
    @Test
    public void testRaceConditionWithThisThreadPriorityAndNotOpenOrder()
            throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        synchronized (localOrder) {
            this.thread = new Thread(this.openProcessor);
            this.thread.start();

            Thread.sleep(500);

            localOrder.setOrderState(OrderState.CLOSED);
        }

        Thread.sleep(500);

        assertEquals(OrderState.CLOSED, localOrder.getOrderState());
    }

    /**
     * This method tests a race condition when the attend open order thread has the order operation
     * priority.
     *
     * @throws Exception
     */
    @Test
    public void testRaceConditionWithOpenProcessorThreadPriority() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        String id = "fake-id";

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        
        Mockito.when(this.localCloudConnector.requestInstance(Mockito.any(Order.class)))
                .thenAnswer(
                        new Answer<String>() {
                            @Override
                            public String answer(InvocationOnMock invocation)
                                    throws InterruptedException {
                                Thread.sleep(500);
                                return id;
                            }
                        });

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(400);

        synchronized (localOrder) {
            Thread.sleep(1000);
            assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
            localOrder.setOrderState(OrderState.OPEN);
        }

        assertEquals(OrderState.OPEN, localOrder.getOrderState());
    }

    private boolean listIsEmpty(ChainedList list) {
        list.resetPointer();
        return list.getNext() == null;
    }
}
