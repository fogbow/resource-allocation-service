package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@PrepareForTest({ DatabaseManager.class,
        CloudConnectorFactory.class,
        Thread.class,
        AssignedForDeletionProcessor.class })
public class AssignedForDeletionProcessorTest extends BaseUnitTests {

    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> assignedForDeletionOrderList;
    private ChainedList<Order> remoteOrderList;
    private AssignedForDeletionProcessor processor;
    private OrderController orderController;

    private LoggerAssert loggerTestChecking = new LoggerAssert(AssignedForDeletionProcessor.class);
    private LoggerAssert loggerTestCheckingstoppableOrderListProcessor = new LoggerAssert(StoppableOrderListProcessor.class);
    
    private Thread thread;
    
    @Rule
    public Timeout globalTimeout = new Timeout(100, TimeUnit.SECONDS);

    @Before
    public void setUp() throws InternalServerErrorException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new AssignedForDeletionProcessor(
                TestUtils.LOCAL_MEMBER_ID, ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.assignedForDeletionOrderList = sharedOrderHolders.getAssignedForDeletionOrdersList();
        this.remoteOrderList = sharedOrderHolders.getRemoteProviderOrdersList();
        this.thread = null;
    }

    // test case: When calling the doRun method and throws an InterruptedException,
    // it must verify if It stop the loop.
    @Test
    public void testRunFailWhenStopThread() throws InterruptedException {
        // set up
        Mockito.doThrow(new InterruptedException()).when(this.processor).doRun();

        // exercise
        this.processor.run();
    }

    // test case: When calling the processAssignedForDeletionOrder method with a remote provider
    // the order state should change to PENDING
    @Test
    public void testProcessAssignedForDeletionWithARemoteProvider()
            throws Exception {

        // set up
        Order order = this.testUtils.createRemoteOrder(TestUtils.ANY_VALUE);
        this.orderController.activateOrder(order);
        OrderState anotherOrderState = OrderState.ASSIGNED_FOR_DELETION;
        OrderStateTransitioner.transition(order, anotherOrderState);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();

        // exercise
        this.processor.processAssignedForDeletionOrder(order);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).deleteInstance(Mockito.any());
        Assert.assertNull(this.assignedForDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.PENDING, order.getOrderState());
        Assert.assertEquals(order, this.remoteOrderList.getNext());
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, Messages.Exception.UNEXPECTED_ERROR);
    }

    // test case: When calling the processAssignedForDeletionOrder method
    // with local Order and it performs the deletion successfully,
    // it must verify if It changes the order context to CHECKING_DELETION.
    @Test
    public void testProcessAssignedForDeletionOrderSuccessfullyWhenOrderLocalAndDeletionExceptionOk()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.ANY_VALUE);
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        Mockito.doNothing().when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).deleteInstance(Mockito.any());
        Assert.assertNull(this.assignedForDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CHECKING_DELETION, order.getOrderState());
    }

    // test case: When calling the processAssignedForDeletionOrder method
    // with local Order and it throws an InstanceNotFoundException when performs deletion operation,
    // it must verify if It changes the order context to CHECKING_DELETION.
    @Test
    public void testProcessAssignedForDeletionOrderSuccessfullyWhenOrderLocalAndThereIsNoInstance()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.ANY_VALUE);
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        Mockito.doThrow(new InstanceNotFoundException()).when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).deleteInstance(Mockito.any());
        Assert.assertNull(this.assignedForDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CHECKING_DELETION, order.getOrderState());
    }

    // test case: When calling the processAssignedForDeletionOrder method
    // with local Order without instanceId,
    // it must verify if It changes the order context to CHECKING_DELETION.
    @Test
    public void testProcessAssignedForDeletionOrderSuccessfullyWhenOrderLocalWithoutInstanceId()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        Mockito.doThrow(new InstanceNotFoundException()).when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);

        // verify
        Assert.assertNull(this.assignedForDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CHECKING_DELETION, order.getOrderState());
    }

    // test case: When calling the processAssignedForDeletionOrder method
    // with local Order and it throws a FogbowException when performs deletion operation,
    // it must verify if It throws a FogbowException.
    @Test(expected = FogbowException.class)
    public void testProcessAssignedForDeletionOrderFailWhenOrderLocalAndThrowsException()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.ANY_VALUE);
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        FogbowException fogbowException = new FogbowException("");
        Mockito.doThrow(fogbowException).when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);
    }

    // test case: When calling the doRun method and throws a Throwable
    // it must verify if It logs an error message.
    @Test
    public void testAssignForDeletionFailWhenThrowsThrowable() throws InterruptedException, FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.assignedForDeletionOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processAssignedForDeletionOrder(Mockito.eq(order));

        // exercise
        this.processor.doRun();

        // verify
        this.loggerTestCheckingstoppableOrderListProcessor.assertEqualsInOrder(Level.ERROR, Messages.Exception.UNEXPECTED_ERROR);
    }

    // test case: When calling the doRun method and throws an InternalServerErrorException
    // it must verify if It logs an error message.
    @Test
    public void testAssignForDeletionFailWhenThrowsUnexpectedException() throws FogbowException, InterruptedException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.assignedForDeletionOrderList.addItem(order);

        String errorMessage = TestUtils.ANY_VALUE;
        InternalServerErrorException internalServerErrorException = new InternalServerErrorException(errorMessage);
        Mockito.doThrow(internalServerErrorException).when(this.processor).processAssignedForDeletionOrder(Mockito.eq(order));

        // exercise
        this.processor.doRun();

        // verify
        this.loggerTestCheckingstoppableOrderListProcessor.assertEqualsInOrder(Level.ERROR, errorMessage);
    }

    // test case: When calling the doRun method and there is no order in the processAssignedForDeletionOrder,
    // it must verify if It does not call processAssignedForDeletionOrder.
    @Test
    public void testAssignForDeletionSuccessfullyWhenThereIsNoOrder()
            throws InterruptedException, FogbowException {

        // set up
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        this.processor.doRun();

        // verify
        Mockito.verify(this.processor, Mockito.times(TestUtils.NEVER_RUN))
                .processAssignedForDeletionOrder(Mockito.any(Order.class));
        this.loggerTestChecking.verifyIfEmpty();
        this.loggerTestCheckingstoppableOrderListProcessor.verifyIfEmpty();
    }
    
    // test case: this method tests if, after starting a thread using an 
    // AssignedForDeletionProcessor instance, the method 'stop' stops correctly the thread
    @Test
    public void testStop() throws InterruptedException, FogbowException {
        this.thread = new Thread(this.processor);
        this.thread.start();
        
        while (!this.processor.isActive()) ;
                
        this.processor.stop();
        this.thread.join();
        
        Assert.assertFalse(this.thread.isAlive());
    }

}
