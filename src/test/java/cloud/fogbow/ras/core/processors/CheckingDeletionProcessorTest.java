package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Map;
import java.util.Properties;

@PrepareForTest({ DatabaseManager.class, CloudConnectorFactory.class })
public class CheckingDeletionProcessorTest extends BaseUnitTests {

    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> checkingDeletionOrderList;
    private Properties properties;
    private CheckingDeletionProcessor processor;
    private OrderController orderController;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationPropertyKeys.PROVIDER_ID_KEY, TestUtils.LOCAL_MEMBER_ID);

        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new CheckingDeletionProcessor(this.orderController,
                TestUtils.LOCAL_MEMBER_ID, ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME));
        
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.checkingDeletionOrderList = sharedOrderHolders.getCheckingDeletionOrdersList();
        
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

    // test case: When running the thread in CheckingDeletionProcessor, orders will be removed
    // from the checkingDeletion list and active order map, and marked as CLOSED.
    @Test
    public void testProcessCheckingDeletionLocalOrderSuccessfullyWhenCloseOrder()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        // TODO (chico) - refactor it
        LocalCloudConnector cloudConnector = Mockito.mock(LocalCloudConnector.class);
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.any(), Mockito.any())).thenReturn(cloudConnector);
        PowerMockito.mockStatic(CloudConnectorFactory.class);
        PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);

        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.CHECKING_DELETION);

        OrderInstance anyInstance = null;
        Mockito.doNothing().when(cloudConnector).switchOffAuditing();
        Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenThrow(new InstanceNotFoundException());

        // exercise
        this.processor.checkDelection();

        // verify
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).closeOrder(Mockito.eq(order));
        Assert.assertNull(this.checkingDeletionOrderList.getNext());
        Assert.assertNull(this.activeOrdersMap.get(order.getId()));
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }
    
    // test case: Check the throw of UnexpectedException when running the thread in
    // the checkingDeletion processor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsUnexpectedException() throws InterruptedException, UnexpectedException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.checkingDeletionOrderList.addItem(order);

        Mockito.doThrow(new UnexpectedException()).when(this.processor).processCheckingDeletionOrder(Mockito.eq(order));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processCheckingDeletionOrder(Mockito.eq(order));
    }
    
    // test case: During a thread running in checkingDeletion processor, if any
    // errors occur, the processCheckingDeletionOrder method will catch an exception.
    @Test
    public void testRunProcessLocalOrderToCatchException() throws InterruptedException, UnexpectedException {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.checkingDeletionOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processCheckingDeletionOrder(Mockito.eq(order));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(TestUtils.DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processCheckingDeletionOrder(Mockito.eq(order));
    }
    
}
