package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Map;

@PrepareForTest({ DatabaseManager.class, CloudConnectorFactory.class })
public class AssignedForDeletionProcessorTest extends BaseUnitTests {

    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> checkingAssignedForDeletionOrderList;
    private AssignedForDeletionProcessor processor;
    private OrderController orderController;

    @Before
    public void setUp() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new AssignedForDeletionProcessor(
                TestUtils.LOCAL_MEMBER_ID, ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.checkingAssignedForDeletionOrderList = sharedOrderHolders.getAssignedForDeletionOrdersList();
    }

    // test case: When calling the run method and throws an InterruptedException,
    // it must verify if It stop the loop.
    @Test
    public void testRunFailWhenStopThread() throws InterruptedException {
        // set up
        Mockito.doThrow(new InterruptedException()).when(this.processor).assignForDeletion();

        // exercise
        this.processor.run();
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
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        Mockito.doNothing().when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);

        // verify
        Assert.assertNull(this.checkingAssignedForDeletionOrderList.getNext());
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
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        Mockito.doThrow(new InstanceNotFoundException()).when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);

        // verify
        Assert.assertNull(this.checkingAssignedForDeletionOrderList.getNext());
        Assert.assertNotNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CHECKING_DELETION, order.getOrderState());
    }

    // test case: When calling the processAssignedForDeletionOrder method
    // with local Order and it throws a FogbowException when performs deletion operation,
    // it must verify if It throws a FogbowException.
    @Test(expected = FogbowException.class)
    public void testProcessAssignedForDeletionOrderSuccessfullyWhenOrderLocalAndThrowsException()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.ANY_VALUE);
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        FogbowException fogbowException = new FogbowException();
        Mockito.doThrow(fogbowException).when(localCloudConnector).deleteInstance(Mockito.eq(order));

        // exercise
        this.processor.processAssignedForDeletionOrder(order);
    }

}
