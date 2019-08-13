package cloud.fogbow.ras.core.processors;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

@PrepareForTest({ DatabaseManager.class })
public class ClosedProcessorTest extends BaseUnitTests {

    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> closedOrderList;
    private ClosedProcessor processor;
    private OrderController orderController;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new ClosedProcessor(this.orderController,
                ConfigurationPropertyDefaults.CLOSED_ORDERS_SLEEP_TIME));
        
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.closedOrderList = sharedOrderHolders.getClosedOrdersList();
        
        this.thread = null;
    }

    @Override
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
        super.tearDown();
    }

    // test case: When running the thread in ClosedProcessor, orders will be removed
    // from the closed list and active order map, and marked as DEACTIVATED.
    @Test
    public void testProcessClosedLocalOrder() throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.CLOSED);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.orderController, Mockito.times(1)).deactivateOrder(Mockito.eq(order));
        Assert.assertNull(closedOrderList.getNext());
        Assert.assertNull(activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.DEACTIVATED, order.getOrderState());
    }
    
    // test case: Check the throw of UnexpectedException when running the thread in
    // the closed processor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsUnexpectedException() throws InterruptedException, UnexpectedException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.closedOrderList.addItem(order);

        Mockito.doThrow(new UnexpectedException()).when(this.processor).processClosedOrder(Mockito.eq(order));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processClosedOrder(Mockito.eq(order));
    }
    
    // test case: During a thread running in closed processor, if any
    // errors occur, the processClosedOrder method will catch an exception.
    @Test
    public void testRunProcessLocalOrderToCatchException() throws InterruptedException, UnexpectedException {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.closedOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processClosedOrder(Mockito.eq(order));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processClosedOrder(Mockito.eq(order));
    }
    
}
