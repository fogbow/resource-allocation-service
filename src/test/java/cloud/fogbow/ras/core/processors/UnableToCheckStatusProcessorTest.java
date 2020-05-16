package cloud.fogbow.ras.core.processors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class })
public class UnableToCheckStatusProcessorTest extends BaseUnitTests {

    private ChainedList<Order> unableToCheckStatus;
    private ChainedList<Order> fulfilledOrderList;
    private CloudConnector cloudConnector;
    private UnableToCheckStatusProcessor processor;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.testUtils.mockLocalCloudConnectorFromFactory();

        this.cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(TestUtils.LOCAL_MEMBER_ID,
                TestUtils.DEFAULT_CLOUD_NAME);

        this.processor = Mockito.spy(new UnableToCheckStatusProcessor(TestUtils.LOCAL_MEMBER_ID,
                ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.unableToCheckStatus = sharedOrderHolders.getUnableToCheckStatusOrdersList();

        this.thread = null;
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    // test case: When running thread in the UnableToCheckStatusProcessor with a
    // remote member ID, the processUnableToCheckStatusOrder method must not change
    // its state, remaining in the failed list.
    @Test
    public void testRunProcessLocalOrderWithRemoteMember() throws Exception {
        // set up
        Order order = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        this.processor = Mockito.spy(new UnableToCheckStatusProcessor(TestUtils.FAKE_REMOTE_MEMBER_ID,
                ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertEquals(order, this.unableToCheckStatus.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When calling the processUnableToCheckStatusOrder method for any
    // requesting state other than failed after a successful request, it must not
    // transition states by keeping the request in its source list.
    @Test
    public void testRunProcessLocalOrderNotFailed() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.unableToCheckStatus.getNext());

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);

        // verify
        Assert.assertEquals(order, this.fulfilledOrderList.getNext());
        Assert.assertNull(this.unableToCheckStatus.getNext());
    }

    // test case: When executing the thread in UnableToCheckStatusProcessor, if the
    // instance state is still Failed after a successful request, the
    // processUnableToCheckStatusOrder method should not change its state and it
    // must remain in the list of failures.
    @Test
    public void testRunProcessLocalOrderWithInstanceFailed() throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        OrderInstance orderInstance = new ComputeInstance(TestUtils.FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertNotNull(this.unableToCheckStatus.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When executing the thread in the UnableToCheckStatusProcessor, if
    // the instance is back to the Ready state, the processUnableToCheckStatusOrder
    // method must change OrderState from UnableToCheckStatus to Fulfilled and the
    // order must be removed from the unableToCheckStatus list and put in the
    // fulfilled list.
    @Test
    public void testRunProcessLocalOrderWithInstanceReady() throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        ComputeInstance computeInstance = new ComputeInstance(TestUtils.FAKE_INSTANCE_ID);
        computeInstance.setReady();

        Mockito.doReturn(computeInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertNotNull(this.fulfilledOrderList.getNext());
        Assert.assertNull(this.unableToCheckStatus.getNext());
    }

    // test case: During a thread running in UnableToCheckStatusProcessor, if any
    // errors occur when attempting to get a cloud provider instance, the
    // processUnableToCheckStatusOrder method will catch an exception.
    @Test
    public void testRunProcessLocalOrderToCatchExceptionWhileTryingToGetInstance()
            throws InterruptedException, FogbowException {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processUnableToCheckStatusOrder(order);
    }

    // test case: Check the throw of UnexpectedException when running the thread in
    // the UnableToCheckStatusProcessor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsUnexpectedException() throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new UnexpectedException()).when(this.processor).processUnableToCheckStatusOrder(order);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processUnableToCheckStatusOrder(order);
    }

    // test case: Check the throw of RuntimeException when running the thread in
    // the UnableToCheckStatusProcessor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsRuntimeException() throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processUnableToCheckStatusOrder(order);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processUnableToCheckStatusOrder(order);
    }

    // test case: When invoking the processUnableToCheckStatusOrder method with an
    // instance that failed after a successful request, it must change the order
    // state to FAILED_AFTER_SUCCESSFUL_REQUEST.
    @Test
    public void testProcessUnableToCheckStatusOrderChangeStateToFailed() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);
        
        OrderInstance orderInstance = new ComputeInstance(TestUtils.FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);

        // verify
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, order.getOrderState());
    }

    // test case: When calling the processUnableToCheckStatusOrder method and the
    // InstanceNotFoundException has been thrown, it must change the order state to
    // FAILED_AFTER_SUCCESSFUL_REQUEST.
    @Test
    public void testProcessUnableToCheckStatusOrderWithInstanceNotFound() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new InstanceNotFoundException()).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);

        // verify
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, order.getOrderState());
    }

    // test case: When calling the processUnableToCheckStatusOrder method with a
    // remote member ID, the order state should not be changed.
    @Test
    public void testProcessUnableToCheckStatusOrderWithRemoteMember() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);

        this.processor = Mockito.spy(new UnableToCheckStatusProcessor(TestUtils.FAKE_REMOTE_MEMBER_ID,
                ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME));

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);

        // verify
        Assert.assertEquals(OrderState.UNABLE_TO_CHECK_STATUS, order.getOrderState());
    }

}
