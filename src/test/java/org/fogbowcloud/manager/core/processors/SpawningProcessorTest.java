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
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.HashMap;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

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

        this.tunnelingService = mock(TunnelingServiceUtil.class);
        this.sshConnectivity = mock(SshConnectivityUtil.class);

        LocalCloudConnector localCloudConnector = mock(LocalCloudConnector.class);

        CloudConnectorFactory cloudConnectorFactory = mock(CloudConnectorFactory.class);
        when(cloudConnectorFactory.getCloudConnector(anyString())).thenReturn(localCloudConnector);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.cloudConnector = CloudConnectorFactory.getInstance()
                .getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID);

        this.spawningProcessor = spy(new SpawningProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
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

    // test case: If trying to process a ComputeOrder not spawning, the method
    // processSpawningOrder() should not change OrderState to Fulfilled and
    // should remain in its source list.
    @Test
    public void testTryingProcessComputeOrderNotSpawning() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.OPEN);
        this.openOrderList.addItem(order);

        // exercise
        this.spawningProcessor.processSpawningOrder(order);

        // verify
        assertNotNull(this.openOrderList.getNext());
        assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: If run process ComputeOrder without SSH connectivity, the method
    // processSpawningOrder() should not change OrderState to Fulfilled and should
    // remain in Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderWithoutSSHConnectivity() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        doReturn(orderInstance).when(this.cloudConnector).getInstance(any(Order.class));
        when(this.tunnelingService.getExternalServiceAddresses(anyString()))
                .thenReturn(new HashMap<>());
        when(this.sshConnectivity.checkSSHConnectivity(any(SshTunnelConnectionData.class)))
                .thenReturn(false);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        assertNotNull(this.spawningOrderList.getNext());
        assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: if run process ComputeOrder without external addresses, the method
    // processSpawningOrder() should not change OrderState to Fulfilled and should
    // remain in Spawning list.
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
        doReturn(orderInstance).when(this.cloudConnector).getInstance(any(Order.class));
        when(this.tunnelingService.getExternalServiceAddresses(anyString())).thenReturn(null);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        assertNotNull(this.spawningOrderList.getNext());
        assertNull(this.fulfilledOrderList.getNext());
    }

    // test case: if run process when OrderType is not Compute, the method processSpawningOrder()
    // should change OrderState to Fulfilled adding on this list, and should be removed in the 
    // Spawning list.
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessWhenOrderTypeIsNotCompute() throws Exception {

        // set up
        Order order = spyAttachmentOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        // assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        doReturn(orderInstance).when(this.cloudConnector).getInstance(any(Order.class));

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        assertNull(this.spawningOrderList.getNext());

        Order test = this.fulfilledOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getInstanceId(), test.getInstanceId());
        assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    // test case: ...
    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsNotReady() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        Instance orderInstance = spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.DISPATCHED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        doReturn(orderInstance).when(this.cloudConnector).getInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();

        // verify
        assertNotNull(this.spawningOrderList.getNext());
        assertNull(this.fulfilledOrderList.getNext());
    }

    // test case:
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderInstanceReachable() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        // assertNull(this.fulfilledOrderList.getNext());

        Instance orderInstance = spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        doReturn(orderInstance).when(this.cloudConnector).getInstance(any(Order.class));
        when(this.tunnelingService.getExternalServiceAddresses(eq(order.getId())))
                .thenReturn(new HashMap<>());
        when(this.sshConnectivity.checkSSHConnectivity(any(SshTunnelConnectionData.class)))
                .thenReturn(true);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        assertNull(this.spawningOrderList.getNext());

        Order test = this.fulfilledOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getInstanceId(), test.getInstanceId());
        assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    // test case: ...
    @SuppressWarnings("static-access")
    @Test
    public void testRunProcessComputeOrderWhenInstanceStateIsFailed() throws Exception {

        // set up
        Order order = spyComputeOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        // assertNull(this.failedOrderList.getNext());

        Instance orderInstance = spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        doReturn(orderInstance).when(this.cloudConnector).getInstance(any(Order.class));
        when(this.tunnelingService.getExternalServiceAddresses(eq(order.getId())))
                .thenReturn(new HashMap<>());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        this.thread.sleep(500);

        // verify
        assertNull(this.spawningOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getInstanceId(), test.getInstanceId());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    private Order spyComputeOrder() {
        FederationUser federationUser = mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        UserData userData = mock(UserData.class);

        Order order = spy(new ComputeOrder(federationUser, requestingMember, providingMember, 8,
                1024, 30, FAKE_IMAGE_NAME, userData, FAKE_PUBLIC_KEY, null));

        return order;
    }

    private Order spyAttachmentOrder() {
        FederationUser federationUser = mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;

        Order order = spy(new AttachmentOrder(federationUser, requestingMember, providingMember,
                FAKE_SOURCE, FAKE_TARGET, FAKE_DEVICE));

        return order;
    }

}
