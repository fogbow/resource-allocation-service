package org.fogbowcloud.manager.core.processors;

import java.util.Properties;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class FulfilledProcessorTest extends BaseUnitTests {

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String REMOTE_MEMBER_ID = "fake-intercomponent-member";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_SOURCE = "fake-source";
    private static final String FAKE_TARGET = "fake-target";
    private static final String FAKE_DEVICE = "fake-device";

    /** Maximum value that the thread should wait in sleep time */
    private static final int MAX_SLEEP_TIME = 30000;
    private static final int DEFAULT_SLEEP_TIME = 500;

    private ChainedList failedOrderList;
    private ChainedList fulfilledOrderList;
    private FulfilledProcessor fulfilledProcessor;
    private LocalCloudConnector localCloudConnector;
    private Properties properties;
    private Thread thread;

    @Before
    public void setUp() {

        super.mockReadOrdersFromDataBase();
        
        HomeDir.getInstance().setPath(TEST_PATH);

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();

        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationConstants.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();

        this.thread = null;
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    // test case: When running thread in FulfilledProcessor without the external addresses of the
    // reverse tunnel, the method processFulfilledOrder() must not change OrderState to Failed and
    // must remain in Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderWithoutExternalAddresses()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Assert.assertNotNull(this.fulfilledOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the OrderType is not a
    // Compute, the method processFulfilledOrder() must not change OrderState to Failed and
    // must remain in Fulfilled list.
    @Test
    public void testRunProcessLocalWhenOrderTypeIsNotComputer()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        // set up
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String providingMember = LOCAL_MEMBER_ID;
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        Order order = new AttachmentOrder(federationUser, requestingMember, providingMember,
                FAKE_SOURCE, FAKE_TARGET, FAKE_DEVICE);

        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        // verify
        Assert.assertNotNull(this.fulfilledOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState Is not
    // Ready, the method processFulfilledOrder() must not change OrderState to Failed and must
    // remain in Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderWhenInstanceStateIsNotReady()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.DISPATCHED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        // verify
        Assert.assertNotNull(this.fulfilledOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState is Failed,
    // the processFulfilledOrder() method must change the OrderState to Failed by adding in that
    // list, and removed from the Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderWhenInstanceStateIsFailed()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        // exercise
        Mockito.doReturn(null).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));
        spyFulfiledProcessor();

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: In calling the processFulfilledOrder() method for any order other than Fulfilled,
    // you must not make state transition by keeping the order in your source list.
    @Test
    public void testProcessComputeOrderNotFulfilled()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderState(OrderState.FAILED);
        this.failedOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        // exercise
        spyFulfiledProcessor();
        this.fulfilledProcessor.processFulfilledOrder(order);

        // verify
        Assert.assertEquals(order, this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor without a LocalMember, the method
    // processFulfilledOrder() must not change OrderState to Failed and must remain in Fulfilled
    // list.
    @Test
    public void testRunProcessLocalComputeOrderWithoutLocalMember()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        // set up
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        this.fulfilledProcessor = new FulfilledProcessor(REMOTE_MEMBER_ID,
                //this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

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
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        mockCloudConnectorFactory(orderInstance);
        this.thread = new Thread(this.fulfilledProcessor);
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
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        /**
         * here may be a false positive depending on how long the machine will take to run the test
         */
        Thread.sleep(MAX_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor and the InstanceState is Failed,
    // the processFulfilledOrder() method must change the OrderState to Failed by adding in that
    // list, and removed from the Fulfilled list.
    @Test
    public void testRunProcessLocalComputeOrderInstanceFailed() throws Exception {

        // set up
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the FulfilledProcessor with OrderState Null must throw a
    // ThrowableException.
    @Test
    public void testRunThrowableExceptionWhileTryingToProcessOrderStateNull()
            throws InterruptedException, UnexpectedException {

        // set up
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.fulfilledOrderList.addItem(order);

        spyFulfiledProcessor();

        // exercise
        Mockito.doThrow(new RuntimeException()).when(this.fulfilledProcessor)
                .processFulfilledOrder(order);

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.fulfilledProcessor, Mockito.times(1)).processFulfilledOrder(order);
    }

    // test case: When running thread in the FulfilledProcessor with OrderState Null must throw a
    // UnexpectedException.
    @Test
    public void testThrowUnexpectedExceptionWhileTryingToProcessOrder()
            throws InterruptedException, UnexpectedException {

        // set up
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.fulfilledOrderList.addItem(order);

        spyFulfiledProcessor();
       
        Mockito.doThrow(new UnexpectedException()).when(this.fulfilledProcessor)
                .processFulfilledOrder(order);

        // exercise
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.fulfilledProcessor, Mockito.times(1)).processFulfilledOrder(order);
    }

    private Order createOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = Mockito.mock(UserData.class);

        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        Order order = new ComputeOrder(federationUser, requestingMember, providingMember, 8, 1024,
                30, FAKE_IMAGE_NAME, userData, FAKE_PUBLIC_KEY, null);

        return order;
    }

    private void mockCloudConnectorFactory(Instance orderInstance)
            throws FogbowManagerException, UnexpectedException {

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString()))
                .thenReturn(localCloudConnector);

        Mockito.doReturn(orderInstance).when(this.localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        spyFulfiledProcessor();
    }

    private void spyFulfiledProcessor() {
        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                //this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));
    }

}
