package cloud.fogbow.ras.core.processors;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

@PrepareForTest({DatabaseManager.class,
    CloudConnectorFactory.class,
    Thread.class,
    StoppingProcessor.class, 
    SharedOrderHolders.class})
public class StoppingProcessorTest extends BaseUnitTests {
    private ChainedList<Order> stoppingOrderList;
    private ChainedList<Order> remoteOrderList;
    private StoppingProcessor processor;
    private OrderController orderController;
    private Thread thread;
    
    private LoggerAssert loggerTestCheckingStoppableOrderListProcessor = new LoggerAssert(StoppableOrderListProcessor.class);

    @Rule
    public Timeout globalTimeout = new Timeout(100, TimeUnit.SECONDS);
    
    @Before
    public void setUp() throws InternalServerErrorException {
        this.testUtils.mockReadOrdersFromDataBase();

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.stoppingOrderList = sharedOrderHolders.getStoppingOrdersList();
        this.remoteOrderList = sharedOrderHolders.getRemoteProviderOrdersList();
        this.thread = null;
        
        this.orderController = Mockito.spy(new OrderController());
        this.processor = Mockito.spy(new StoppingProcessor(TestUtils.LOCAL_MEMBER_ID, 
                ConfigurationPropertyDefaults.STOPPING_ORDERS_SLEEP_TIME));
    }

    @After
    public void tearDown() throws InternalServerErrorException {
        this.testUtils.cleanList(this.stoppingOrderList);
    }
    
    // test case: When calling the run method and throws an InterruptedException,
    // it must verify if it stops the loop.
    @Test
    public void testRunFailWhenStopThread() throws InterruptedException {
        // set up
        Mockito.doThrow(new InterruptedException()).when(this.processor).doRun();

        // exercise
        this.processor.run();
    }

    // test case: When calling the processStopOrder method
    // with local Order and the order instance is stopped, 
    // it must verify if it changes the order context to STOPPED.
    @Test
    public void testProcessStopOrderSuccessfullyWhenOrderLocalAndInstanceNotFound()
            throws Exception {

        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.STOPPING);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();

        
        ComputeInstance orderInstance = Mockito.mock(ComputeInstance.class);
        Mockito.when(orderInstance.isStopped()).thenReturn(true);
        
        Mockito.when(localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(orderInstance);

        // exercise
        this.processor.processStopOrder(order);

        // verify
        Assert.assertNull(this.stoppingOrderList.getNext());
        Assert.assertEquals(OrderState.STOPPED, order.getOrderState());
    }

    // test case: When calling the processStopOrder method
    // with local Order and throws a generic exception,
    // it must verify if it changes the order context to FAILED_AFTER_SUCCESSFUL_REQUEST.
    @Test
    public void testProcessStopOrderFailWhenOrderLocalAndThrowsGenericException()
            throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(order);
        OrderStateTransitioner.transition(order, OrderState.STOPPING);

        LocalCloudConnector localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
        Mockito.doNothing().when(localCloudConnector).switchOffAuditing();
        Mockito.when(localCloudConnector.getInstance(Mockito.eq(order)))
                .thenThrow(new InstanceNotFoundException());

        // exercise
        this.processor.processStopOrder(order);

        // verify
        Assert.assertNull(this.stoppingOrderList.getNext());
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, order.getOrderState());
    }

    // test case: When calling the processStopOrder method
    // with remote Order should change the order state to PENDING.
    @Test
    public void testProcessStopOrderSuccessfullyWhenOrderRemote()
            throws Exception {
        // set up
        Order orderRemote = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(orderRemote);
        OrderState orderState = OrderState.STOPPING;
        OrderStateTransitioner.transition(orderRemote, orderState);

        // exercise
        this.processor.processStopOrder(orderRemote);

        // verify
        Assert.assertNull(this.stoppingOrderList.getNext());
        Assert.assertEquals(orderRemote, this.remoteOrderList.getNext());
        Assert.assertEquals(OrderState.PENDING, orderRemote.getOrderState());
    }

    // test case: When calling the doRun method and throws an InternalServerErrorException
    // it must verify if it logs an error message.
    @Test
    public void testStopFailWhenThrowsUnexpectedException() throws InterruptedException, FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        this.orderController.activateOrder(order);
        OrderState orderState = OrderState.STOPPING;
        OrderStateTransitioner.transition(order, orderState);

        String errorMessage = TestUtils.ANY_VALUE;
        InternalServerErrorException internalServerErrorException = new InternalServerErrorException(errorMessage);
        Mockito.doThrow(internalServerErrorException).when(this.processor).processStopOrder(Mockito.eq(order));

        // exercise
        this.processor.doRun();

        // verify
        this.loggerTestCheckingStoppableOrderListProcessor.assertEqualsInOrder(Level.ERROR, errorMessage);
    }
    
    // test case: When calling the doRun method and there is no order in the stoppingOrderList,
    // it must verify if it does not call processStopOrder.
    @Test
    public void testStopSuccessfullyWhenThereIsNoOrder()
            throws InterruptedException, FogbowException {
        // set up
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        this.processor.doRun();
        
        // verify
        Mockito.verify(this.processor, Mockito.times(TestUtils.NEVER_RUN))
                .processStopOrder(Mockito.any(Order.class));
        this.loggerTestCheckingStoppableOrderListProcessor.verifyIfEmpty();
    }
    
    // test case: this method tests if, after starting a thread using a
    // StoppingProcessor instance, the method 'stop' stops correctly the thread
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
