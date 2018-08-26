package org.fogbowcloud.ras.core.processors;

import org.fogbowcloud.ras.core.BaseUnitTests;
import org.fogbowcloud.ras.core.OrderStateTransitioner;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class OpenProcessorTest extends BaseUnitTests {

    private OpenProcessor openProcessor;
    private Thread thread;
    private CloudConnector cloudConnector;

    @Before
    public void setUp() throws UnexpectedException {
        mockReadOrdersFromDataBase();

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        when(cloudConnectorFactory.getCloudConnector(anyString())).thenReturn(localCloudConnector);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID);

        this.thread = null;
        this.openProcessor = Mockito.spy(new OpenProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME));
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    //test case: test if the open processor is setting to spawning an open order when the request
    //instance method of instance provider returns an instance.
    @Test
    public void testProcessOpenLocalOrder() throws Exception {
        //set up
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        String id = "fake-id";
        Mockito.doReturn(id)
                .when(this.cloudConnector)
                .requestInstance(Mockito.any(Order.class));

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        //verify
        assertEquals(OrderState.SPAWNING, localOrder.getOrderState());

        // test if the open order list is empty and 
        // the spawningList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(localOrder, spawningOrdersList.getNext());
    }

    //test case: test if the open processor is setting to failed an open order when the request instance
    //method of instance provider returns a null instance.
    @Test
    public void testProcessOpenLocalOrderWithNullInstance() throws Exception {
        //set up
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        Mockito.doReturn(null)
                .when(this.cloudConnector)
                .requestInstance(Mockito.any(Order.class));

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        //verify
        assertEquals(OrderState.FAILED, localOrder.getOrderState());

        // test if the open order list is empty and the failedList is with the
        // localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertEquals(localOrder, failedOrdersList.getNext());
    }

    //test case: test if the open processor is setting to failed an open order when the request instance
    //method of instance provider throws an exception.
    @Test
    public void testProcessLocalOpenOrderRequestingException() throws Exception {
        //set up
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.cloudConnector)
                .requestInstance(Mockito.any(Order.class));

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();
        Thread.sleep(500);


        //verify
        assertEquals(OrderState.FAILED, localOrder.getOrderState());

        // test if the open order list is empty and 
        // the failedList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(localOrder, failedOrdersList.getNext());
    }

    //test case: test if the open processor is setting to pending an open intercomponent order.
    @Test
    public void testProcessOpenRemoteOrder() throws Exception {
        //set up
        Order remoteOrder = this.createRemoteOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(remoteOrder);

        Mockito.doReturn(null)
                .when(this.cloudConnector)
                .requestInstance(Mockito.any(Order.class));

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        //verify
        assertEquals(OrderState.PENDING, remoteOrder.getOrderState());

        // test if the open order list is empty and
        // the failedList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertSame(remoteOrder, pendingOrdersList.getNext());
    }

    //test case: test if the open processor is setting to fail an open intercomponent order when the request instance
    //method of intercomponent instance provider throws an exception.
    @Test
    public void testProcessRemoteOpenOrderRequestingException() throws Exception {
        //set up
        Order remoteOrder = this.createRemoteOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(remoteOrder);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.cloudConnector)
                .requestInstance(Mockito.any(Order.class));

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        //verify
        assertEquals(OrderState.FAILED, remoteOrder.getOrderState());

        // test if the open order list is empty and
        // the failedList is with the localOrder
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        assertTrue(this.listIsEmpty(openOrdersList));
        assertEquals(remoteOrder, failedOrdersList.getNext());
    }

    //test case: test if the open processor does not process an Order that is not in the open state.
    @Test
    public void testProcessNotOpenOrder() throws InterruptedException, UnexpectedException {
        //set up
        Order order = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(order);

        order.setOrderStateInTestMode(OrderState.PENDING);

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        //verify
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        assertEquals(OrderState.PENDING, order.getOrderState());
        assertFalse(this.listIsEmpty(openOrdersList));
    }

    //test case: test if the open processor still running if it try to process a null order.
    @Test
    public void testRunProcessWithNullOpenOrder() throws InterruptedException {
        //verify
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);
    }

    //test case: test if the open processor still run and do not change the order state if the method
    //processOpenOrder throws an exception.
    @Test
    public void testProcessOpenOrderThrowingAnException() throws Exception {
        //set up
        Order order = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(Exception.class)
                .when(this.openProcessor)
                .processOpenOrder(Mockito.any(Order.class));

        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(500);

        //verify
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
        openOrdersList.addItem(order);
        assertEquals(OrderState.OPEN, order.getOrderState());
        assertFalse(this.listIsEmpty(openOrdersList));
    }

    //test case: this method tests a race condition when this class thread has the order operation priority.
    @Test
    public void testRaceConditionWithThisThreadPriority() throws Exception {
        //set up
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        String id = "fake-id";
        Mockito.doReturn(id)
                .when(this.cloudConnector)
                .requestInstance(Mockito.any(Order.class));

        //exercise
        synchronized (localOrder) {
            this.thread = new Thread(this.openProcessor);
            this.thread.start();

            Thread.sleep(500);

            assertEquals(OrderState.OPEN, localOrder.getOrderState());
        }

        Thread.sleep(500);

        //verify
        assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
    }

    //test case: this method tests a race condition when this class thread has the order operation priority
    //and changes the open order to a different state.
    @Test
    public void testRaceConditionWithThisThreadPriorityAndNotOpenOrder()
            throws Exception {
        //set up
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        //exercise
        synchronized (localOrder) {
            this.thread = new Thread(this.openProcessor);
            this.thread.start();

            Thread.sleep(500);

            localOrder.setOrderStateInTestMode(OrderState.CLOSED);
        }

        Thread.sleep(500);

        //verify
        assertEquals(OrderState.CLOSED, localOrder.getOrderState());
    }

    //test case: this method tests a race condition when the attend open order thread has the order operation priority.
    @Test
    public void testRaceConditionWithOpenProcessorThreadPriority() throws Exception {
        //set up
        Order localOrder = this.createLocalOrder(getLocalMemberId());

        OrderStateTransitioner.activateOrder(localOrder);

        String id = "fake-id";

        Mockito.when(this.cloudConnector.requestInstance(Mockito.any(Order.class)))
                .thenAnswer(
                        new Answer<String>() {
                            @Override
                            public String answer(InvocationOnMock invocation)
                                    throws InterruptedException {
                                Thread.sleep(500);
                                return id;
                            }
                        });
        //exercise
        this.thread = new Thread(this.openProcessor);
        this.thread.start();

        Thread.sleep(400);

        synchronized (localOrder) {
            Thread.sleep(1000);
            assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
            localOrder.setOrderStateInTestMode(OrderState.OPEN);
        }

        //verify
        assertEquals(OrderState.OPEN, localOrder.getOrderState());
    }

    private boolean listIsEmpty(ChainedList list) {
        list.resetPointer();
        return list.getNext() == null;
    }
}
