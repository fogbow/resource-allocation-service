package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({ RemoteCloudConnector.class,
        DatabaseManager.class,
        CloudConnectorFactory.class,
        Thread.class,
        RemoteOrdersStateSynchronizationProcessor.class })
public class RemoteOrdersStateSynchronizationProcessorTest extends BaseUnitTests {

    private ChainedList<Order> remoteOrderList;
    private RemoteOrdersStateSynchronizationProcessor processor;

    private LoggerAssert loggerTestChecking = new LoggerAssert(RemoteOrdersStateSynchronizationProcessor.class);
    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws InternalServerErrorException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.processor = Mockito.spy(new RemoteOrdersStateSynchronizationProcessor(
                TestUtils.LOCAL_MEMBER_ID, ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.remoteOrderList = sharedOrderHolders.getRemoteProviderOrdersList();
    }

    // test case: When calling the processRemoteProviderOrder method with remote FULFILLED order,
    // it must verify if It set the remote value at the same order.
    @Test
    public void testProcessRemoteProviderOrderSuccessfullyWhenIsRemoteOrder() throws FogbowException {

        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.isProviderLocal(Mockito.any())).thenReturn(false);
        Mockito.when(order.getOrderState()).thenReturn(OrderState.FULFILLED);

        RemoteCloudConnector remoteCloudConnector = this.testUtils.mockRemoteCloudConnectorFromFactory();
        Order remoteOrder = Mockito.mock(Order.class);
        OrderState remoteOrderState = OrderState.FULFILLED;
        Mockito.when(remoteOrder.getOrderState()).thenReturn(remoteOrderState);
        Mockito.when(remoteCloudConnector.getRemoteOrder(Mockito.eq(order))).thenReturn(remoteOrder);

        Mockito.doNothing().when(order).updateFromRemote(Mockito.eq(remoteOrder));
        Mockito.doNothing().when(order).setOrderState(Mockito.eq(remoteOrderState));

        // exercise
        this.processor.processRemoteProviderOrder(order);

        // verify
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).updateFromRemote(Mockito.eq(remoteOrder));
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).setOrderState(Mockito.eq(remoteOrderState));
    }

    // test case: When calling the processRemoteProviderOrder method with remote order and
    // it throws an FogbowException, it must verify if It logs an info message.
    @Test
    public void testProcessRemoteProviderOrderFail() throws FogbowException {

        // set up
        Order order = Mockito.spy(this.testUtils.createRemoteOrder(TestUtils.ANY_VALUE));
        order.setOrderState(OrderState.FULFILLED);

        RemoteCloudConnector remoteCloudConnector = this.testUtils.mockRemoteCloudConnectorFromFactory();
        String errorMessageException = TestUtils.ANY_VALUE;
        FogbowException fogbowException = new FogbowException(errorMessageException);
        Mockito.when(remoteCloudConnector.getRemoteOrder(Mockito.eq(order))).thenThrow(fogbowException);

        String infoMessageExpected = String.format(Messages.Exception.GENERIC_EXCEPTION_S, errorMessageException);

        // exercise
        this.processor.processRemoteProviderOrder(order);

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.WARN, infoMessageExpected);
    }

    // test case: When calling the processRemoteProviderOrder method with local FULFILLED order,
    // it must verify if It do not do anything and continue the loop.
    @Test
    public void testProcessRemoteProviderOrderSuccessfullyWhenIsLocalOrderAndFulfilled() throws FogbowException {

        // set up
        Order order = Mockito.spy(this.testUtils.createLocalOrder(TestUtils.LOCAL_MEMBER_ID));
        order.setOrderState(OrderState.FULFILLED);

        RemoteCloudConnector remoteCloudConnector = this.testUtils.mockRemoteCloudConnectorFromFactory();

        // exercise
        this.processor.processRemoteProviderOrder(order);

        // verify
        Mockito.verify(order, Mockito.times(TestUtils.NEVER_RUN)).getOrderState();
        Mockito.verify(remoteCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).getRemoteOrder(Mockito.eq(order));
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, Messages.Exception.UNEXPECTED_ERROR);

    }

    // test case: When calling the processRemoteProviderOrder method with remote FAILED_ON_REQUEST order,
    // it must verify if It do not do anything and continue the loop.
    @Test
    public void testProcessRemoteProviderOrderSuccessfullyWhenIsRemoteOrderFailed() throws FogbowException {

        // set up
        Order order = Mockito.spy(this.testUtils.createLocalOrder(TestUtils.ANY_VALUE));
        order.setOrderState(OrderState.FAILED_ON_REQUEST);

        RemoteCloudConnector remoteCloudConnector = this.testUtils.mockRemoteCloudConnectorFromFactory();

        // exercise
        this.processor.processRemoteProviderOrder(order);

        // verify
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).getOrderState();
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).isProviderLocal(Mockito.any());
        Mockito.verify(remoteCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).getRemoteOrder(Mockito.eq(order));
    }

    // test case: When calling the processRemoteProviderOrder method with remote ASSIGNED_FOR_DELETION order,
    // it must verify if It do not do anything and continue the loop.
    @Test
    public void testProcessRemoteProviderOrderSuccessfullyWhenIsRemoteOrderAssignedForDelection()
            throws FogbowException {

        // set up
        Order order = Mockito.spy(this.testUtils.createLocalOrder(TestUtils.ANY_VALUE));
        order.setOrderState(OrderState.ASSIGNED_FOR_DELETION);

        RemoteCloudConnector remoteCloudConnector = this.testUtils.mockRemoteCloudConnectorFromFactory();

        // exercise
        this.processor.processRemoteProviderOrder(order);

        // verify
        Mockito.verify(order, Mockito.times(TestUtils.RUN_TWICE)).getOrderState();
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).isProviderLocal(Mockito.any());
        Mockito.verify(remoteCloudConnector, Mockito.times(TestUtils.NEVER_RUN)).getRemoteOrder(Mockito.eq(order));
    }

    // test case: When calling the synchronizeWithRemote method and throws a Throwable
    // it must verify if It logs an error message.
    @Test
    public void testAssignForDeletionFailWhenThrowsThrowable() throws InterruptedException, InternalServerErrorException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.remoteOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processRemoteProviderOrder(Mockito.eq(order));

        // exercise
        this.processor.synchronizeWithRemote();

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, Messages.Exception.UNEXPECTED_ERROR);
    }

    // test case: When calling the synchronizeWithRemote method and throws an InternalServerErrorException
    // it must verify if It logs an error message.
    @Test
    public void testAssignForDeletionFailWhenThrowsUnexpectedException() throws InterruptedException, InternalServerErrorException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.remoteOrderList.addItem(order);

        String errorMessage = TestUtils.ANY_VALUE;
        InternalServerErrorException internalServerErrorException = new InternalServerErrorException(errorMessage);
        Mockito.doThrow(internalServerErrorException).when(this.processor).processRemoteProviderOrder(Mockito.eq(order));

        // exercise
        this.processor.synchronizeWithRemote();

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.ERROR, errorMessage);
    }

    // test case: When calling the synchronizeWithRemote method and there is no order in the processAssignedForDeletionOrder,
    // it must verify if It does not call processAssignedForDeletionOrder.
    @Test
    public void testAssignForDeletionSuccessfullyWhenThereIsNoOrder() throws InterruptedException, InternalServerErrorException {

        // set up
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        this.processor.synchronizeWithRemote();

        // verify
        Mockito.verify(this.processor, Mockito.times(TestUtils.NEVER_RUN))
                .processRemoteProviderOrder(Mockito.any(Order.class));
        this.loggerTestChecking.verifyIfEmpty();
    }

}
