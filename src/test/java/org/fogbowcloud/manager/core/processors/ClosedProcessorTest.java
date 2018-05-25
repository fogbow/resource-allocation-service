package org.fogbowcloud.manager.core.processors;

import org.fogbowcloud.manager.core.BaseUnitTests;

public class ClosedProcessorTest extends BaseUnitTests {
//
//    private ClosedProcessor closedProcessor;
//
//    private RemoteInstanceProvider remoteInstanceProvider;
//    private LocalInstanceProvider localInstanceProvider;
//
//    private Properties properties;
//
//    private Thread thread;
//    private OrderController orderController;
//
//    @Before
//    public void setUp() {
//        this.properties = new Properties();
//        this.properties.setProperty(
//                ConfigurationConstants.XMPP_ID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);
//
//        this.orderController = new OrderController(properties, localInstanceProvider, remoteInstanceProvider);
//
//        this.localInstanceProvider = Mockito.mock(LocalInstanceProvider.class);
//        this.remoteInstanceProvider = Mockito.mock(RemoteInstanceProvider.class);
//
//        this.closedProcessor =
//                Mockito.spy(
//                        new ClosedProcessor(
//                                this.localInstanceProvider,
//                                this.remoteInstanceProvider,
//                                this.orderController,
//                                this.properties));
//    }
//
//    @Override
//    public void tearDown() {
//        if (this.thread != null) {
//            this.thread.interrupt();
//            this.thread = null;
//        }
//        super.tearDown();
//    }
//
//    @Test
//    public void testProcessClosedLocalOrder() throws Exception {
//        Instance orderInstance = new Instance("fake-id");
//        Order localOrder = createLocalOrder(getLocalMemberId());
//        localOrder.setInstanceId(orderInstance);
//
//        Token federationToken = null;
//        this.orderController.activateOrder(localOrder, federationToken);
//
//        OrderStateTransitioner.transition(localOrder, OrderState.CLOSED);
//
//        Mockito.doNothing()
//                .when(this.localInstanceProvider)
//                .deleteInstance(Mockito.any(Instance.class));
//
//        this.thread = new Thread(this.closedProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
//        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();
//        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
//        assertNull(activeOrdersMap.get(localOrder.getId()));
//
//        closedOrders.resetPointer();
//        assertNull(closedOrders.getNext());
//    }
//
//    @Test
//    public void testProcessClosedLocalOrderFails() throws Exception {
//        Instance orderInstance = new Instance("fake-id");
//        Order localOrder = createLocalOrder(getLocalMemberId());
//        localOrder.setInstanceId(orderInstance);
//
//        Token federationToken = null;
//        this.orderController.activateOrder(localOrder, federationToken);
//
//        OrderStateTransitioner.transition(localOrder, OrderState.CLOSED);
//
//        Mockito.doThrow(Exception.class)
//                .when(this.localInstanceProvider)
//                .deleteInstance(Mockito.any(Instance.class));
//
//        this.thread = new Thread(this.closedProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
//        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();
//        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
//        assertEquals(localOrder, activeOrdersMap.get(localOrder.getId()));
//
//        closedOrders.resetPointer();
//        assertEquals(localOrder, closedOrders.getNext());
//    }
}
