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
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Map;

@PrepareForTest({DatabaseManager.class,
        CloudConnectorFactory.class,
        Thread.class,
        CheckingDeletionProcessor.class})
public class CheckingDeletionProcessorTest extends BaseUnitTests {

    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> checkingDeletionOrderList;
    private ChainedList<Order> remoteOrderList;
    private CheckingDeletionProcessor processor;
    private OrderController orderController;

    private LoggerAssert loggerTestChecking = new LoggerAssert(CheckingDeletionProcessor.class);

    @Before
    public void setUp() throws InternalServerErrorException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new CheckingDeletionProcessor(this.orderController,
                TestUtils.LOCAL_MEMBER_ID, ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.checkingDeletionOrderList = sharedOrderHolders.getCheckingDeletionOrdersList();
        this.remoteOrderList = sharedOrderHolders.getRemoteProviderOrdersList();
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

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
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

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        String exceptionMessage = TestUtils.ANY_VALUE;
        FogbowException fogbowException = new FogbowException(exceptionMessage);
        Mockito.when(localCloudConnector.getInstance(Mockito.eq(order)))
                .thenThrow(fogbowException);

        String exceptionMessageExpected = String.format(
                Messages.Exception.GENERIC_EXCEPTION_S, exceptionMessage);

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
    // with remote Order should change the order state to PENDING.
    @Test
    public void testProcessCheckingDeletionOrderSuccessfullyWhenOrderRemote()
            throws Exception {

        // set up
        Order orderRemote = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(orderRemote);
        OrderState orderState = OrderState.CHECKING_DELETION;
        OrderStateTransitioner.transition(orderRemote, orderState);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();

        // exercise
        this.processor.processCheckingDeletionOrder(orderRemote);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).switchOffAuditing();
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN)).closeOrder(Mockito.eq(orderRemote));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(orderRemote), Mockito.eq(Operation.DELETE));
        Assert.assertNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(orderRemote.getId()));
        Assert.assertEquals(orderRemote, this.remoteOrderList.getNext());
        Assert.assertEquals(OrderState.PENDING, orderRemote.getOrderState());
    }

    // test case: When calling the processCheckingDeletionOrder method
    // with a remote provider, the order should change state to REMOTE.
    @Test
    public void testProcessCheckingDeletionWithARemoteProvider() throws Exception {

        // set up
        Order orderRemote = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(orderRemote);
        OrderState orderState = OrderState.CHECKING_DELETION;
        OrderStateTransitioner.transition(orderRemote, orderState);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();

        // exercise
        this.processor.processCheckingDeletionOrder(orderRemote);

        // verify
        Mockito.verify(localCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).switchOffAuditing();
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN)).closeOrder(Mockito.eq(orderRemote));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(orderRemote), Mockito.eq(Operation.DELETE));
        Assert.assertNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(orderRemote.getId()));
        Assert.assertEquals(OrderState.PENDING, orderRemote.getOrderState());
        Assert.assertEquals(orderRemote, this.remoteOrderList.getNext());
    }

    // test case: When calling the checkDeletion method and throws an InternalServerErrorException
    // it must verify if it logs an error message.
    @Test
    public void testCheckDeletionFailWhenThrowsUnexpectedException() throws InterruptedException, InternalServerErrorException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.checkingDeletionOrderList.addItem(order);

        String errorMessage = TestUtils.ANY_VALUE;
        InternalServerErrorException internalServerErrorException = new InternalServerErrorException(errorMessage);
        Mockito.doThrow(internalServerErrorException).when(this.processor).processCheckingDeletionOrder(Mockito.eq(order));

        // exercise
        this.processor.checkDeletion();

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, errorMessage);
    }

    // test case: When calling the checkDeletion method and there is no order in the checkingDeletionList,
    // it must verify if It does not call processCheckingDeletionOrder.
    @Test
    public void testCheckDeletionSuccessfullyWhenThereIsNoOrder()
            throws InterruptedException, InternalServerErrorException {

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
    @Test(expected = InterruptedException.class)
    public void testCheckDeletionFailWhenThrowsInterruptedException() throws InterruptedException {
        // set up
        PowerMockito.spy(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        this.processor.checkDeletion();
    }

}
