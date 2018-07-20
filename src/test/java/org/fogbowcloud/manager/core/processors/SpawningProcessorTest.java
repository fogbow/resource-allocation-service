package org.fogbowcloud.manager.core.processors;

import org.fogbowcloud.manager.core.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.util.connectivity.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.util.connectivity.SshConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.TunnelingServiceUtil;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class SpawningProcessorTest extends BaseUnitTests {

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_SOURCE = "fake-source";
    private static final String FAKE_TARGET = "fake-target";
    private static final String FAKE_DEVICE = "fake-device";

    private ChainedList failedOrderList;
    private ChainedList fulfilledOrderList;
    private ChainedList openOrderList;
    private ChainedList spawningOrderList;
    private CloudConnector cloudConnector;
    private SpawningProcessor spawningProcessor;
    private SshConnectivityUtil sshConnectivity;
    private Thread thread;
    private TunnelingServiceUtil tunnelingService;

    @Before
    public void setUp() {
        super.mockDB();

        HomeDir.getInstance().setPath(TEST_PATH);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString()))
                .thenReturn(localCloudConnector);

        this.cloudConnector = CloudConnectorFactory.getInstance()
                .getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID);

        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);

        this.spawningProcessor = Mockito.spy(new SpawningProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
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
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.OPEN);
        this.openOrderList.addItem(order);

        // exercise
        this.spawningProcessor.processSpawningOrder(order);

        // verify
        Assert.assertNotNull(this.openOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in SpawningProcessor, if there is no SSH connectivity as the
    // reverse tunnel, the method processSpawningOrder() must not change OrderState to Fulfilled and
    // must remain in Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderWithoutSSHConnectivity() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));
        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.anyString()))
                .thenReturn(new HashMap<>());
        Mockito.when(this.sshConnectivity
                .checkSSHConnectivity(Mockito.any(SshTunnelConnectionData.class)))
                .thenReturn(false);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        Assert.assertNotNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in SpawningProcessor without the external addresses of the
    // reverse tunnel, the method processSpawningOrder() must not change OrderState to Fulfilled and
    // must remain in Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderWithoutExternalAddresses() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));
        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.anyString()))
                .thenReturn(null);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        Assert.assertNotNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the OrderType is not a
    // Compute, the processSpawningOrder() method must immediately change the OrderState to
    // Fulfilled by adding in that list, and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessWhenOrderTypeIsNotCompute() throws Exception {

        // set up
        Order order = spyAttachmentOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the InstanceState Is not
    // Ready, the method processSpawningOrder() must not change OrderState to Fulfilled and must
    // remain in Spawning list.
    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsNotReady() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.DISPATCHED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();

        // verify
        Assert.assertNotNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: When running thread in the SpawningProcessor and the InstanceState is Ready, the
    // processSpawningOrder() method must change the OrderState to Fulfilled by adding in that list,
    // and removed from the Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderInstanceReachable() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));
        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
                .thenReturn(new HashMap<>());
        Mockito.when(this.sshConnectivity
                .checkSSHConnectivity(Mockito.any(SshTunnelConnectionData.class))).thenReturn(true);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

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
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        Assert.assertNull(this.failedOrderList.getNext());

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));
        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
                .thenReturn(new HashMap<>());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
    }

    private Order spyComputeOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        UserData userData = Mockito.mock(UserData.class);

        Order order = Mockito.spy(new ComputeOrder(federationUser, requestingMember,
                providingMember, 8, 1024, 30, FAKE_IMAGE_NAME, userData, FAKE_PUBLIC_KEY, null));

        return order;
    }

    private Order spyAttachmentOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;

        Order order = Mockito.spy(new AttachmentOrder(federationUser, requestingMember,
                providingMember, FAKE_SOURCE, FAKE_TARGET, FAKE_DEVICE));

        return order;
    }

}
