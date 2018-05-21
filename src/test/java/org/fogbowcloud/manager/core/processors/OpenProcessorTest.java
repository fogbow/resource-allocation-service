package org.fogbowcloud.manager.core.processors;

import static org.junit.Assert.*;

import java.util.Properties;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class OpenProcessorTest extends BaseUnitTests {

    private OpenProcessor openProcessor;

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;

    private Properties properties;

    private Thread thread;

    @Before
    public void setUp() {
        this.properties = new Properties();
        this.properties.setProperty(
                ConfigurationConstants.XMPP_ID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
        this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

        this.thread = null;

        this.openProcessor =
                Mockito.spy(
                        new OpenProcessor(
                                this.localInstanceProvider,
                                this.remoteInstanceProvider,
                                this.properties));
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    /**
     * Test if the open processor is setting to spawning an open local order when the request
     * instance method of instance provider returns an instance.
     *
     * @throws Exception
     */
    @Test
    public void testProcessOpenLocalOrder() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(localOrder);

        String id = "fake-id";
        Mockito.doReturn(id)
                .when(this.localInstanceProvider)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.SPAWNING, localOrder.getOrderState());

        // test if the open order list is empty and the spawningList is with the
        // localOrder
        ChainedList spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(localOrder, spawningOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to failed an open local order when the request instance
     * method of instance provider returns a null instance.
     *
     * @throws Exception
     */
    @Test
    public void testProcessOpenLocalOrderWithNullInstance() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(localOrder);

        Mockito.doReturn(null)
                .when(this.localInstanceProvider)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.FAILED, localOrder.getOrderState());

        // test if the open order list is empty and the failedList is with the
        // localOrder
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertEquals(localOrder, failedOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to failed an open local order when the request instance
     * method of instance provider throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testProcessLocalOpenOrderRequestingException() throws Exception {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(localOrder);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.localInstanceProvider)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.FAILED, localOrder.getOrderState());

        // test if the open order list is empty and the failedList is with the
        // localOrder
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(localOrder, failedOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to pending an open remote order.
     *
     * @throws Exception
     */
    @Test
    public void testProcessOpenRemoteOrder() throws Exception {
        Order remoteOrder = this.createRemoteOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(remoteOrder);

        Mockito.doReturn(null)
                .when(this.remoteInstanceProvider)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.PENDING, remoteOrder.getOrderState());

        // test if the open order list is empty and the failedList is with the
        // localOrder
        ChainedList pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(remoteOrder, pendingOrdersList.getNext());
    }

    /**
     * Test if the open processor is setting to fail an open remote order when the request instance
     * method of remote instance provider throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testProcessRemoteOpenOrderRequestingException() throws Exception {
        Order remoteOrder = this.createRemoteOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(remoteOrder);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.remoteInstanceProvider)
                .requestInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertEquals(OrderState.FAILED, remoteOrder.getOrderState());

        // test if the open order list is empty and the failedList is with the
        // localOrder
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
    public void testProcessNotOpenOrder() throws InterruptedException {
        Order order = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(order);

        order.setOrderState(OrderState.PENDING);

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

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
            throws OrderStateTransitionException, InterruptedException {
        Order order = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(order);

        Mockito.doThrow(OrderStateTransitionException.class)
                .when(this.openProcessor)
                .processOpenOrder(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

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
            throws OrderStateTransitionException, InterruptedException {
        Order order = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(order);

        Mockito.doThrow(Exception.class)
                .when(this.openProcessor)
                .processOpenOrder(Mockito.any(Order.class));

        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

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

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(localOrder);

        String id = "fake-id";
        Mockito.doReturn(id)
                .when(this.localInstanceProvider)
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
            throws InterruptedException {
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(localOrder);

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

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(localOrder);

        String id = "fake-id";

        Mockito.when(this.localInstanceProvider.requestInstance(Mockito.any(Order.class)))
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
