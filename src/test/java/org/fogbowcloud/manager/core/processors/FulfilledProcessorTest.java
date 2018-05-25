package org.fogbowcloud.manager.core.processors;

import org.fogbowcloud.manager.core.BaseUnitTests;

public class FulfilledProcessorTest extends BaseUnitTests {

//    private FulfilledProcessor fulfilledProcessor;
//
//    private InstanceProvider localInstanceProvider;
//    private InstanceProvider remoteInstanceProvider;
//
//    private Properties properties;
//
//    private TunnelingServiceUtil tunnelingService;
//    private SshConnectivityUtil sshConnectivity;
//
//    private Thread thread;
//
//    private ChainedList fulfilledOrderList;
//    private ChainedList failedOrderList;
//
//    @Before
//    public void setUp() {
//        this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
//        this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);
//
//        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
//        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);
//
//        this.properties = new Properties();
//        this.properties.put(ConfigurationConstants.XMPP_ID_KEY, "local-member");
//
//        this.thread = null;
//
//        this.fulfilledProcessor =
//                Mockito.spy(
//                        new FulfilledProcessor(
//                                this.localInstanceProvider,
//                                this.remoteInstanceProvider,
//                                this.tunnelingService,
//                                this.sshConnectivity,
//                                this.properties));
//
//        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
//        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
//        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
//    }
//
//    @After
//    public void tearDown() {
//        if (this.thread != null) {
//            this.thread.interrupt();
//        }
//
//        super.tearDown();
//    }
//
//    /**
//     * Test if a fulfilled order of an active local compute instance has not being changed to failed
//     * if SSH connectivity is reachable.
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testRunProcessLocalComputeOrderInstanceReachable() throws Exception {
//        Order order = this.createLocalOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Instance orderInstance = Mockito.spy(new ComputeInstance("fake-id"));
//        orderInstance.setState(InstanceState.READY);
//        order.setInstanceId(orderInstance);
//
//        Mockito.doReturn(orderInstance)
//                .when(this.localInstanceProvider)
//                .getInstance(any(Order.class));
//
//        Mockito.doNothing()
//                .when((ComputeInstance) orderInstance)
//                .setExternalServiceAddresses(anyMapOf(String.class, String.class));
//
//        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeInstance.class)))
//                .thenReturn(true);
//
//        assertNull(this.failedOrderList.getNext());
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        assertNotNull(this.fulfilledOrderList.getNext());
//        assertNull(this.failedOrderList.getNext());
//    }
//
//    /**
//     * Test if a fulfilled order of an active remote compute instance has not being changed to
//     * failed if SSH connectivity is reachable.
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testRunProcessRemoteComputeOrderInstanceReachable() throws Exception {
//        Order order = this.createRemoteOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Instance orderInstance = Mockito.spy(new ComputeInstance("fake-id"));
//        orderInstance.setState(InstanceState.READY);
//        order.setInstanceId(orderInstance);
//
//        Mockito.doReturn(orderInstance)
//                .when(this.remoteInstanceProvider)
//                .getInstance(any(Order.class));
//
//        Mockito.doNothing()
//                .when((ComputeInstance) orderInstance)
//                .setExternalServiceAddresses(anyMapOf(String.class, String.class));
//
//        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeInstance.class)))
//                .thenReturn(true);
//
//        assertNull(this.failedOrderList.getNext());
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        assertNotNull(this.fulfilledOrderList.getNext());
//        assertNull(this.failedOrderList.getNext());
//    }
//
//    /**
//     * Test if a fulfilled order of an active local compute instance is changed to failed if SSH
//     * connectivity is not reachable.
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testRunProcessLocalComputeOrderInstanceNotReachable() throws Exception {
//        Order order = this.createLocalOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Instance orderInstance = Mockito.spy(new ComputeInstance("fake-id"));
//        orderInstance.setState(InstanceState.READY);
//        order.setInstanceId(orderInstance);
//
//        Mockito.doReturn(orderInstance)
//                .when(this.localInstanceProvider)
//                .getInstance(any(Order.class));
//
//        Mockito.doNothing()
//                .when((ComputeInstance) orderInstance)
//                .setExternalServiceAddresses(anyMapOf(String.class, String.class));
//
//        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeInstance.class)))
//                .thenReturn(false);
//
//        assertNull(this.failedOrderList.getNext());
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        assertNull(this.fulfilledOrderList.getNext());
//
//        Order test = this.failedOrderList.getNext();
//        assertNotNull(test);
//        assertEquals(order.getInstance(), test.getInstance());
//        assertEquals(OrderState.FAILED, test.getOrderState());
//    }
//
//    /**
//     * Test if a fulfilled order of an active remote compute instance is changed to failed if SSH
//     * connectivity is not reachable.
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testRunProcessRemoteComputeOrderInstanceNotReachable() throws Exception {
//        Order order = this.createRemoteOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Instance orderInstance = Mockito.spy(new ComputeInstance("fake-id"));
//        orderInstance.setState(InstanceState.READY);
//        order.setInstanceId(orderInstance);
//
//        Mockito.doReturn(orderInstance)
//                .when(this.remoteInstanceProvider)
//                .getInstance(any(Order.class));
//
//        Mockito.doNothing()
//                .when((ComputeInstance) orderInstance)
//                .setExternalServiceAddresses(anyMapOf(String.class, String.class));
//
//        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeInstance.class)))
//                .thenReturn(false);
//
//        assertNull(this.failedOrderList.getNext());
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        assertNull(this.fulfilledOrderList.getNext());
//
//        Order test = this.failedOrderList.getNext();
//        assertNotNull(test);
//        assertEquals(order.getInstance(), test.getInstance());
//        assertEquals(OrderState.FAILED, test.getOrderState());
//    }
//
//    /**
//     * Test if a fulfilled order of a failed local compute instance is definitely changed to failed.
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testRunProcessLocalComputeOrderInstanceFailed() throws Exception {
//        Order order = this.createLocalOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Instance orderInstance = Mockito.spy(new ComputeInstance("fake-id"));
//        orderInstance.setState(InstanceState.FAILED);
//        order.setInstanceId(orderInstance);
//
//        Mockito.doReturn(orderInstance)
//                .when(this.localInstanceProvider)
//                .getInstance(any(Order.class));
//
//        assertNull(this.failedOrderList.getNext());
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        assertNull(this.fulfilledOrderList.getNext());
//
//        Order test = this.failedOrderList.getNext();
//        assertNotNull(test);
//        assertEquals(order.getInstance(), test.getInstance());
//        assertEquals(OrderState.FAILED, test.getOrderState());
//    }
//
//    /**
//     * Test if a fulfilled order of an failed remote compute instance is definitely changed to
//     * failed.
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testRunProcessRemoteComputeOrderInstanceFailed() throws Exception {
//        Order order = this.createRemoteOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Instance orderInstance = Mockito.spy(new ComputeInstance("fake-id"));
//        orderInstance.setState(InstanceState.FAILED);
//        order.setInstanceId(orderInstance);
//
//        Mockito.doReturn(orderInstance)
//                .when(this.remoteInstanceProvider)
//                .getInstance(any(Order.class));
//
//        assertNull(this.failedOrderList.getNext());
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        assertNull(this.fulfilledOrderList.getNext());
//
//        Order test = this.failedOrderList.getNext();
//        assertNotNull(test);
//        assertEquals(order.getInstance(), test.getInstance());
//        assertEquals(OrderState.FAILED, test.getOrderState());
//    }
//
//    /**
//     * Test if the fulfilled processor still running and do not change the order state if the method
//     * processFulfilledOrder throw an order state transition exception.
//     *
//     * @throws OrderStateTransitionException
//     * @throws InterruptedException
//     */
//    @Test
//    public void testProcessFulfilledOrderThrowingOrderStateTransitionException()
//            throws OrderStateTransitionException, InterruptedException {
//        Order order = this.createLocalOrder();
//        order.setOrderState(OrderState.FULFILLED);
//
//        this.fulfilledOrderList.addItem(order);
//
//        Mockito.doThrow(OrderStateTransitionException.class)
//                .when(this.fulfilledProcessor)
//                .processFulfilledOrder(Mockito.any(Order.class));
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//
//        Thread.sleep(500);
//
//        Order test = this.fulfilledOrderList.getNext();
//        assertEquals(order.getInstance(), test.getInstance());
//        assertEquals(OrderState.FULFILLED, order.getOrderState());
//    }
//
//    @Test
//    public void testRunThrowableExceptionWhileTryingToProcessOrder()
//            throws InterruptedException, OrderStateTransitionException {
//        Order order = Mockito.mock(Order.class);
//        OrderState state = null;
//        order.setOrderState(state);
//        this.fulfilledOrderList.addItem(order);
//
//        Mockito.doThrow(new RuntimeException("Any Exception"))
//                .when(this.fulfilledProcessor)
//                .processFulfilledOrder(order);
//
//        this.thread = new Thread(this.fulfilledProcessor);
//        this.thread.start();
//        Thread.sleep(500);
//    }
//
//    @Test
//    public void testRunExceptionWhileTryingToProcessInstance() throws Exception {
//        Order order = this.createLocalOrder();
//        order.setOrderState(OrderState.FULFILLED);
//        this.fulfilledOrderList.addItem(order);
//
//        Mockito.doThrow(new OrderStateTransitionException("Any Exception"))
//                .when(this.fulfilledProcessor)
//                .processInstance(order);
//
//        this.fulfilledProcessor.processFulfilledOrder(order);
//    }
//
//    private Order createLocalOrder() {
//        Token federationToken = Mockito.mock(Token.class);
//        UserData userData = Mockito.mock(UserData.class);
//        String imageName = "fake-image-name";
//        String requestingMember =
//                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
//        String providingMember =
//                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
//        String publicKey = "fake-public-key";
//
//        Order localOrder =
//                new ComputeOrder(
//                        federationToken,
//                        requestingMember,
//                        providingMember,
//                        8,
//                        1024,
//                        30,
//                        imageName,
//                        userData,
//                        publicKey);
//        return localOrder;
//    }
//
//    private Order createRemoteOrder() {
//        Token federationToken = Mockito.mock(Token.class);
//        UserData userData = Mockito.mock(UserData.class);
//        String imageName = "fake-image-name";
//        String requestingMember =
//                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
//        String providingMember = "fake-remote-member";
//        String publicKey = "fake-public-key";
//
//        Order remoteOrder =
//                new ComputeOrder(
//                        federationToken,
//                        requestingMember,
//                        providingMember,
//                        8,
//                        1024,
//                        30,
//                        imageName,
//                        userData,
//                        publicKey);
//        return remoteOrder;
//    }
}
