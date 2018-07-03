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
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.fogbowcloud.manager.util.connectivity.SshConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.TunnelingServiceUtil;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sun.security.jca.GetInstance;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class SpawningProcessorTest extends BaseUnitTests {

    private CloudConnector cloudConnector;
    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;
    private SpawningProcessor spawningProcessor;
    private Thread thread;
    private ChainedList spawningOrderList;
    private ChainedList fulfilledOrderList;
    private ChainedList failedOrderList;
    private ChainedList openOrderList;
    private ChainedList pendingOrderList;
    private ChainedList closedOrderList;
    private BehaviorPluginsHolder behaviorPluginsHolder;
    private LocalIdentityPlugin localIdentityPlugin;
    private AaController aaController;
    private OrderController orderController;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        
        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        when(cloudConnectorFactory.getCloudConnector(anyString())).thenReturn(localCloudConnector);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(BaseUnitTests.LOCAL_MEMBER_ID);

        this.spawningProcessor = spy(
                new SpawningProcessor(BaseUnitTests.LOCAL_MEMBER_ID, this.tunnelingService, this.sshConnectivity,
                        DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME));
        
        this.thread = null;

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
        this.openOrderList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrderList = sharedOrderHolders.getPendingOrdersList();
        this.closedOrderList = sharedOrderHolders.getClosedOrdersList();
    }
    
    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    @Test
    public void testRunProcessComputeOrderInstanceActive() throws Exception {
        Order order = this.createMockedOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        doReturn(false).when(order).isRequesterRemote(anyString());

        String instanceId = "fake-id";
        Instance orderInstance = spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
				.thenReturn(new HashMap<>());

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
        		SshTunnelConnectionData.class))).thenReturn(true);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.fulfilledOrderList.getNext();
        assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    @Test
    public void testRunProcessComputeOrderInstanceInactive() throws InterruptedException {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        String instanceId = "fake-id";
        ComputeInstance computeOrderInstance =
                spy(new ComputeInstance(instanceId));
        computeOrderInstance.setState(InstanceState.INACTIVE);
        order.setInstanceId(instanceId);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Order test = this.spawningOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
    }

    @Test
    public void testRunProcessComputeOrderInstanceFailed() throws Exception {
        Order order = this.createMockedOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);
        doReturn(false).when(order).isRequesterRemote(anyString());
        
        String instanceId = "fake-id";
        Instance orderInstance = spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(instanceId);
        
        Mockito.doReturn(orderInstance).when(this.cloudConnector)
                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
				.thenReturn(new HashMap<>());

        Assert.assertNull(this.failedOrderList.getNext());
        
        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Assert.assertNull(this.spawningOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
    }

    @Test
    public void testProcessWithoutModifyOpenOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.OPEN);
        this.openOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.openOrderList.getNext();
        Assert.assertEquals(OrderState.OPEN, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyPendingOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.PENDING);
        this.pendingOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.pendingOrderList.getNext();
        Assert.assertEquals(OrderState.PENDING, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyFailedOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FAILED);
        this.failedOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.failedOrderList.getNext();
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyFulfilledOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
    }

    @Test
    public void testProcessWithoutModifyClosedOrderState() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.CLOSED);
        this.closedOrderList.addItem(order);

        this.spawningProcessor.processSpawningOrder(order);

        Order test = this.closedOrderList.getNext();
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
        Assert.assertNull(this.spawningOrderList.getNext());
        Assert.assertNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testRunConfiguredToFailedAttemptSshConnectivity() throws InterruptedException {
        Order order = this.createOrder();
        order.setOrderState(OrderState.SPAWNING);
        this.spawningOrderList.addItem(order);

        String instanceId = "fake-id";
        ComputeInstance computeOrderInstance =
                spy(new ComputeInstance(instanceId));
        computeOrderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
				.thenReturn(new HashMap<>());

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
        		SshTunnelConnectionData.class))).thenReturn(true);

        this.thread = new Thread(this.spawningProcessor);
        this.thread.start();
        Thread.sleep(500);

        Order test = this.spawningOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
    }

    private Order createOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        UserData userData = Mockito.mock(UserData.class);
        Order order = new ComputeOrder(federationUser, requestingMember, providingMember, 8, 1024,
                30, "fake_image_name", userData, "fake_public_key", null);
        return order;
    }

    private Order createMockedOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String requestingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        String providingMember = BaseUnitTests.LOCAL_MEMBER_ID;
        UserData userData = Mockito.mock(UserData.class);
        Order order = spy(new ComputeOrder(federationUser, requestingMember, providingMember, 8, 1024,
                30, "fake_image_name", userData, "fake_public_key", null));
        return order;
    }
}
