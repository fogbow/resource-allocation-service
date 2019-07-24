package cloud.fogbow.ras.core.processors;

import java.util.Properties;

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
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class FulfilledProcessorTest extends BaseUnitTests {

    private static final String DEFAULT_CLOUD_NAME = "default";
    private static final String FAKE_IDENTITY_PROVIDER_ID = "fake-identity-provider-id";
    private static final String FAKE_IMAGE_ID = "fake-image";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String REMOTE_MEMBER_ID = "fake-intercomponent-member";

    /**
     * Maximum value that the thread should wait in sleep time
     */
    private static final int MAX_SLEEP_TIME = 10000;
    private static final int DEFAULT_SLEEP_TIME = 500;

    private ChainedList<Order> failedOrderList;
    private ChainedList<Order> fulfilledOrderList;
    private FulfilledProcessor processor;
    private CloudConnector cloudConnector;
    private Properties properties;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {
        super.mockReadOrdersFromDataBase();

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationPropertyKeys.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);
        
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(localCloudConnector);

        this.cloudConnector = CloudConnectorFactory.getInstance()
                .getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID, DEFAULT_CLOUD_NAME);
        
        this.processor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                ConfigurationPropertyDefaults.FULFILLED_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();

        this.thread = null;
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    // test case: When running thread in FulfilledProcessor, if the instance state is Ready,
    // the method processFulfilledOrder() must not change OrderState to Failed and the order
    // must remain in the Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderWithInstanceReady()
            throws FogbowException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        ComputeInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.READY);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertNotNull(this.fulfilledOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState is Failed,
    // the processFulfilledOrder() method must change the OrderState to Failed by adding in that
    // list, and removed from the Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderWhenInstanceStateIsFailed()
            throws FogbowException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);

        ComputeInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, test.getOrderState());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: In calling the processFulfilledOrder() method for any order other than Fulfilled,
    // you must not make state transition by keeping the order in your source list.
    @Test
    public void testProcessComputeOrderNotFulfilled()
            throws FogbowException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        this.failedOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        // exercise
        this.processor.processFulfilledOrder(order);

        // verify
        Assert.assertEquals(order, this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor without a LocalMember, the method
    // processFulfilledOrder() must not change OrderState to Failed and must remain in Fulfilled
    // list.
    @Test
    public void testRunProcessLocalComputeOrderWithoutLocalMember()
            throws FogbowException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        this.processor = new FulfilledProcessor(REMOTE_MEMBER_ID,
                ConfigurationPropertyDefaults.FULFILLED_ORDERS_SLEEP_TIME);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertEquals(order, this.fulfilledOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState is Ready, the
    // method processFulfilledOrder() must not change OrderState to Failed and must remain in
    // Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderInstanceReachable() throws Exception {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        ComputeInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.READY);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertNotNull(this.fulfilledOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState is not Ready,
    // the processFulfilledOrder() method must change the OrderState to Failed by adding in that
    // list, and removed from the Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderInstanceNotReachable() throws Exception {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        ComputeInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();

        /**
         * here may be a false positive depending on how long the machine will take to run the test
         */
        Thread.sleep(MAX_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, test.getOrderState());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState is Failed,
    // the processFulfilledOrder() method must change the OrderState to Failed by adding in that
    // list, and removed from the Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderInstanceFailed() throws Exception {

        // set up
        Order order = this.createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        ComputeInstance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setHasFailed();

        Mockito.doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, test.getOrderState());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor with OrderState Null must throw a
    // ThrowableException.
    @Test
    public void testRunThrowableExceptionWhileTryingToProcessOrderStateNull()
            throws InterruptedException, FogbowException {

        // set up
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderStateInTestMode(state);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException()).when(this.processor).processFulfilledOrder(order);
        
        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processFulfilledOrder(order);
    }

    // test case: When running thread in the FulfilledProcessor with OrderState Null must throw a
    // UnexpectedException.
    @Test
    public void testThrowUnexpectedExceptionWhileTryingToProcessOrder()
            throws InterruptedException, FogbowException {

        // set up
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderStateInTestMode(state);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new UnexpectedException()).when(this.processor)
                .processFulfilledOrder(order);

        // exercise
        this.thread = new Thread(this.processor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.processor, Mockito.times(1)).processFulfilledOrder(order);
    }
    
    // test case: When invoking the processFulfilledOrder method and an error occurs
    // while trying to get an instance from the cloud provider, an
    // UnavailableProviderException will be throw.
    @Test(expected = UnavailableProviderException.class) // Verify
    public void testProcessFulfilledOrderThrowsUnavailableProviderException() throws FogbowException {
        // set up
        Order order = createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new UnavailableProviderException()).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.processor.processFulfilledOrder(order);
    }
    
    // test case: When calling the processFulfilledOrder method and the
    // order instance is not found, it must change the order state to
    // FAILED_AFTER_SUCCESSFUL_REQUEST.
    @Test
    public void testProcessFulfilledOrderWithInstanceNotFound() throws FogbowException {
        // set up
        Order order = createOrder();
        order.setOrderStateInTestMode(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new InstanceNotFoundException()).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.processor.processFulfilledOrder(order);

        // verify
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, order.getOrderState());
    }

    private Order createOrder() {
        SystemUser systemUser = new SystemUser(FAKE_USER_ID, FAKE_USER_NAME, FAKE_IDENTITY_PROVIDER_ID);
        String requestingMember = String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String providingMember = String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        
        return new ComputeOrder(systemUser, requestingMember, providingMember, DEFAULT_CLOUD_NAME, 
                FAKE_INSTANCE_NAME, 8, 1024, 30, FAKE_IMAGE_ID, super.mockUserData(), FAKE_PUBLIC_KEY, null);
    }

}
