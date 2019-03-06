package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.linkedlists.ChainedList;
import cloud.fogbow.ras.core.models.orders.*;
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
public class SpawningProcessorTest extends BaseUnitTests {

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";

    private static final int DEFAULT_SLEEP_TIME = 500;
    private static final int SPAWNING_SLEEP_TIME = 2000;

    private ChainedList failedOrderList;
    private ChainedList fulfilledOrderList;
    private ChainedList openOrderList;
    private ChainedList spawningOrderList;
    private CloudConnector cloudConnector;
    private SpawningProcessor spawningProcessor;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {

        super.mockReadOrdersFromDataBase();

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(localCloudConnector);

        this.cloudConnector = CloudConnectorFactory.getInstance()
                .getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID, "default");

        this.spawningProcessor = Mockito.spy(new SpawningProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                ConfigurationPropertyDefaults.SPAWNING_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();
        this.openOrderList = sharedOrderHolders.getOpenOrdersList();

        this.thread = null;
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    // test case: In calling the processSpawningOrder() method for any order other than spawning,
    // you must not make state transition by keeping the order in your source list.
    @Test
    public void testProcessComputeOrderNotSpawning() throws Exception {

        // set up
        Order order = createComputeOrder();
        order.setOrderStateInTestMode(OrderState.OPEN);
        this.openOrderList.addItem(order);

        // exercise
        this.spawningProcessor.processSpawningOrder(order);

        // verify
        Assert.assertEquals(order, this.openOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the OrderType is not a
    // Compute, the processSpawningOrder() method must immediately change the OrderState to
    // Fulfilled by adding in that list, and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessWhenOrderTypeIsNetwork() throws Exception {

        // set up
        Order order = new NetworkOrder();
        order.setRequester(BaseUnitTests.LOCAL_MEMBER_ID);
        order.setOrderStateInTestMode(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the OrderType is not a
    // Compute, the processSpawningOrder() method must immediately change the OrderState to
    // Fulfilled by adding in that list, and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessWhenOrderTypeIsVolume() throws Exception {

        // set up
        Order order = new VolumeOrder();
        order.setRequester(BaseUnitTests.LOCAL_MEMBER_ID);
        order.setOrderStateInTestMode(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the OrderType is not a
    // Compute, the processSpawningOrder() method must immediately change the OrderState to
    // Fulfilled by adding in that list, and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessWhenOrderTypeIsAttachment() throws Exception {

        // set up
        Order order = new AttachmentOrder();
        order.setRequester(BaseUnitTests.LOCAL_MEMBER_ID);
        order.setOrderStateInTestMode(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the InstanceState is not
    // Ready, the method processSpawningOrder() must not change OrderState to Fulfilled and must
    // remain in Spawning list.
    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsNotReady() throws Exception {

        // set up
        Order order = createComputeOrder();
        order.setOrderStateInTestMode(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.DISPATCHED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();

        // verify
        Assert.assertEquals(order, this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the InstanceState is Ready, the
    // processSpawningOrder() method must change the OrderState to Fulfilled by adding in that list,
    // and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderInstanceReachable() throws Exception {

        // set up
        Order order = createComputeOrder();
        order.setOrderStateInTestMode(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());
        String orderId = order.getId();

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(SPAWNING_SLEEP_TIME);

        // verify
        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the InstanceState is Failed,
    // the processSpawningOrder() method must change the OrderState to Failed by adding in that
    // list, and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsFailed() throws Exception {

        // set up
        Order order = createComputeOrder();
        order.setOrderStateInTestMode(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());
        String orderId = order.getId();

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
    }

    private Order createComputeOrder() {
        SystemUser systemUser = Mockito.mock(SystemUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;

        Order order = new ComputeOrder(systemUser, requestingMember,
                providingMember, "default", FAKE_INSTANCE_NAME, 8, 1024, 30, FAKE_IMAGE_NAME, mockUserData(), FAKE_PUBLIC_KEY, null);

        return order;
    }

}
