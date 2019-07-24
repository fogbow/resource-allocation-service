package cloud.fogbow.ras.core.processors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class UnableToCheckStatusProcessorTest extends BaseUnitTests {

    private ChainedList<Order> unableToCheckStatus;
    private ChainedList<Order> fulfilledOrderList;
    private CloudConnector cloudConnector;
    private UnableToCheckStatusProcessor processor;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {
        super.mockReadOrdersFromDataBase();
        
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(localCloudConnector);

        this.cloudConnector = CloudConnectorFactory.getInstance()
                .getCloudConnector(LOCAL_MEMBER_ID, DEFAULT_CLOUD_NAME);

        this.processor = Mockito.spy(new UnableToCheckStatusProcessor(LOCAL_MEMBER_ID,
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
    public void testRunProcessLocalOrderWithRemoteMember() throws FogbowException, InterruptedException {
        // set up
        Order order = createRemoteOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        this.processor = Mockito.spy(new UnableToCheckStatusProcessor(FAKE_REMOTE_MEMBER_ID,
                ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertEquals(order, this.unableToCheckStatus.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When calling the processFulfilledOrder method for any requesting
    // state other than failed after a successful request, it must not transition
    // states by keeping the request in its source list.
    @Test
    public void testRunProcessLocalOrderNotFailed() throws FogbowException, InterruptedException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.FULFILLED);
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
    public void testRunProcessLocalOrderWithInstanceFailed() throws FogbowException, InterruptedException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        OrderInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

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
    public void testRunProcessLocalOrderWithInstanceReady() throws InterruptedException, FogbowException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        OrderInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setReady();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

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
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processUnableToCheckStatusOrder(order);
    }

    // test case: Check the throw of UnexpectedException when running the thread in
    // the UnableToCheckStatusProcessor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsUnexpectedException() throws InterruptedException, FogbowException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new UnexpectedException()).when(this.processor).processUnableToCheckStatusOrder(order);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processUnableToCheckStatusOrder(order);
    }

    // test case: Check the throw of RuntimeException when running the thread in
    // the UnableToCheckStatusProcessor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsRuntimeException() throws InterruptedException, FogbowException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processUnableToCheckStatusOrder(order);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processUnableToCheckStatusOrder(order);
    }

    // test case: When invoking the processUnableToCheckStatusOrder method with an
    // instance that failed after a successful request, it must change the order
    // state to FAILED_AFTER_SUCCESSFUL_REQUEST.
    @Test
    public void testProcessUnableToCheckStatusOrderChangeStateToFailed() throws FogbowException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);
        
        OrderInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);

        // verify
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, order.getOrderState());
    }

    // test case: When invoking the processUnableToCheckStatusOrder method and an
    // error occurs while trying to get an instance from the cloud provider, an
    // UnavailableProviderException will be throw.
    @Test(expected = UnavailableProviderException.class) // Verify
    public void testProcessUnableToCheckStatusOrderThrowsUnavailableProviderException() throws FogbowException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);

        Mockito.doThrow(new UnavailableProviderException()).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);
    }

    // test case: When calling the processUnableToCheckStatusOrder method and the
    // order instance is not found, it must change the order state to
    // FAILED_AFTER_SUCCESSFUL_REQUEST.
    @Test
    public void testProcessUnableToCheckStatusOrderWithInstanceNotFound() throws FogbowException {
        // set up
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.UNABLE_TO_CHECK_STATUS);
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
        Order order = createLocalOrder(getLocalMemberId());
        order.setInstanceId(FAKE_INSTANCE_ID);
        order.setOrderStateInTestMode(OrderState.UNABLE_TO_CHECK_STATUS);
        this.unableToCheckStatus.addItem(order);

        this.processor = Mockito.spy(new UnableToCheckStatusProcessor(FAKE_REMOTE_MEMBER_ID,
                ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME));

        // exercise
        this.processor.processUnableToCheckStatusOrder(order);

        // verify
        Assert.assertEquals(OrderState.UNABLE_TO_CHECK_STATUS, order.getOrderState());
    }

}
