package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Map;

@PrepareForTest({ DatabaseManager.class, CloudConnectorFactory.class, Thread.class })
public class CheckingDeletionProcessorTest extends BaseUnitTests {

    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> checkingDeletionOrderList;
    private CheckingDeletionProcessor processor;
    private OrderController orderController;

    private LoggerAssert loggerTestChecking = new LoggerAssert(CheckingDeletionProcessor.class);

    @Before
    public void setUp() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new CheckingDeletionProcessor(this.orderController,
                TestUtils.LOCAL_MEMBER_ID, ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME));
        
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.checkingDeletionOrderList = sharedOrderHolders.getCheckingDeletionOrdersList();
    }

    // test case: When calling the run method and throws an InterruptedException,
    // it must verify if It stop the loop.
    @Test
    public void testRunFailWhenStopThread() throws InterruptedException {
        // set up
        Mockito.doThrow(new InterruptedException()).when(this.processor).checkDeletion();

        // exercise
        this.processor.run();
    }

    // test case: When calling the processCheckingDeletionOrder method
    // with local Order and there is no more instance in the cloud,
    // it must verify if It changes the order context to CLOSED.
    @Test
    public void testProcessCheckingDeletionOrderSuccessfullyWhenOrderLocalAndInstanceNotFound()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.CHECKING_DELETION);

        LocalCloudConnector localCloudConnector = mockLocalCloudConnector();
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        Mockito.when(localCloudConnector.getInstance(Mockito.eq(order)))
                .thenThrow(new InstanceNotFoundException());

        // exercise
        this.processor.processCheckingDeletionOrder(order);

        // verify
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).closeOrder(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .updateOrderDependencies(Mockito.eq(order), Mockito.eq(Operation.DELETE));
        Assert.assertNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When calling the processCheckingDeletionOrder method
    // with local Order and throws a generic exception,
    // it must verify if It changes the order context to CLOSED.
    @Test
    public void testProcessCheckingDeletionOrderFailWhenOrderLocalAndThrowsGenericException()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.CHECKING_DELETION);

        LocalCloudConnector localCloudConnector = mockLocalCloudConnector();
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        String exceptionMessage = TestUtils.ANY_VALUE;
        FogbowException fogbowException = new FogbowException(exceptionMessage);
        Mockito.when(localCloudConnector.getInstance(Mockito.eq(order)))
                .thenThrow(fogbowException);

        String exceptionMessageExpected = String.format(
                Messages.Exception.GENERIC_EXCEPTION, exceptionMessage);

        // exercise
        this.processor.processCheckingDeletionOrder(order);

        // verify
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN)).closeOrder(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(order), Mockito.eq(Operation.DELETE));
        Assert.assertNotNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CHECKING_DELETION, order.getOrderState());
        this.loggerTestChecking.assertEqualsInOrder(Level.INFO, exceptionMessageExpected);
    }

    // test case: When calling the processCheckingDeletionOrder method
    // with remote Order and there is no more instance in the cloud,
    // it must verify if It keeps the same order context.
    @Test
    public void testProcessCheckingDeletionOrderSuccessfullyWhenOrderRemote()
            throws Exception {

        // set up
        Order orderRemote = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(orderRemote);
        OrderState orderState = OrderState.CHECKING_DELETION;
        OrderStateTransitioner.transition(orderRemote, orderState);

        LocalCloudConnector localCloudConnector = mockLocalCloudConnector();

        // exercise
        this.processor.processCheckingDeletionOrder(orderRemote);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).switchOffAuditing();
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN)).closeOrder(Mockito.eq(orderRemote));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(orderRemote), Mockito.eq(Operation.DELETE));
        Assert.assertNotNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(orderRemote.getId()));
        Assert.assertEquals(orderState, orderRemote.getOrderState());
    }

    // test case: When calling the processCheckingDeletionOrder method
    // with local Order with a unexpected state,
    // it must verify if It keeps the same order context.
    @Test
    public void testProcessCheckingDeletionOrderSuccessfullyWhenOrderHasUnexpectedState()
            throws Exception {

        // set up
        Order orderRemote = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(orderRemote);
        OrderState orderState = OrderState.FULFILLED;
        OrderStateTransitioner.transition(orderRemote, orderState);

        LocalCloudConnector localCloudConnector = mockLocalCloudConnector();

        // exercise
        this.processor.processCheckingDeletionOrder(orderRemote);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).switchOffAuditing();
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN)).closeOrder(Mockito.eq(orderRemote));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(orderRemote), Mockito.eq(Operation.DELETE));
        Assert.assertNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(orderRemote.getId()));
        Assert.assertEquals(orderState, orderRemote.getOrderState());
    }

    // test case: When calling the checkDeletion method and throws an UnexpectedException
    // it must verify if It logs an error message.
    @Test
    public void testCheckDeletionFailWhenThrowsUnexpectedException() throws InterruptedException, UnexpectedException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.checkingDeletionOrderList.addItem(order);

        String errorMessage = TestUtils.ANY_VALUE;
        UnexpectedException unexpectedException = new UnexpectedException(errorMessage);
        Mockito.doThrow(unexpectedException).when(this.processor).processCheckingDeletionOrder(Mockito.eq(order));

        // exercise
        this.processor.checkDeletion();

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, errorMessage);
    }

    // test case: When calling the checkDeletion method and there is no order in the checkingDeletionList,
    // it must verify if It does not call processCheckingDeletionOrder.
    // FIXME (chico) - Thread mock has a problem
    @Ignore
    @Test
    public void testCheckDeletionSuccessfullyWhenThereIsNoOrder()
            throws InterruptedException, UnexpectedException {

        // set up
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        this.processor.checkDeletion();

        // verify
        Mockito.verify(this.processor, Mockito.times(TestUtils.NEVER_RUN))
                .processCheckingDeletionOrder(Mockito.any(Order.class));
        this.loggerTestChecking.verifyIfEmpty();
    }

    // test case: When calling the checkDeletion method and throws an InterruptedException
    // it must verify if It rethrows the same exception.
    // FIXME (chico) - Thread mock has a problem
    @Ignore
    @Test(expected = InterruptedException.class)
    public void testCheckDeletionFailWhenThrowsInterruptedException() throws InterruptedException {
        // set up
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        this.processor.checkDeletion();
    }

    private LocalCloudConnector mockLocalCloudConnector() {
        LocalCloudConnector cloudConnector = Mockito.mock(LocalCloudConnector.class);
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.any(), Mockito.any())).thenReturn(cloudConnector);
        PowerMockito.mockStatic(CloudConnectorFactory.class);
        PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
        return cloudConnector;
    }
    
}
