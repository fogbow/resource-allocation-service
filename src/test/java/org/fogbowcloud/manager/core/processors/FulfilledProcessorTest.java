package org.fogbowcloud.manager.core.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Properties;

import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.BehaviorPluginsHolder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.RemoteCloudConnector;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.util.connectivity.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.fogbowcloud.manager.util.connectivity.SshConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.TunnelingServiceUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class FulfilledProcessorTest extends BaseUnitTests {

    private FulfilledProcessor fulfilledProcessor;
    private AaController aaController;
    private RemoteCloudConnector remoteCloudConnector;
    private LocalCloudConnector localCloudConnector;
    private LocalIdentityPlugin localIdentityPlugin;
    private BehaviorPluginsHolder behaviorPluginsHolder;
    private OrderController orderController;
    private Properties properties;
    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;
    private Thread thread;
    private ChainedList fulfilledOrderList;
    private ChainedList failedOrderList;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationConstants.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        this.remoteCloudConnector = Mockito.mock(RemoteCloudConnector.class);

        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        // TODO review !
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);

        this.thread = null;

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }

        super.tearDown();
    }

    /**
     * Test if a fulfilled order of an active localidentity compute instance has not being changed to failed
     * if SSH connectivity is reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceReachable() throws Exception {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        String instanceId = "instanceid";
        Instance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        when(cloudConnectorFactory.getCloudConnector(anyString())).thenReturn(localCloudConnector);

        doReturn(orderInstance).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
                SshTunnelConnectionData.class))).thenReturn(true);

        assertNull(this.failedOrderList.getNext());

//        this.fulfilledProcessor.processFulfilledOrder(order);

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(500);

        assertNotNull(this.fulfilledOrderList.getNext());
        assertNull(this.failedOrderList.getNext());
    }

    /**
     * Test if a fulfilled order of an active localidentity compute instance is changed to failed if SSH
     * connectivity is not reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceNotReachable() throws Exception {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        String instanceId = "instanceid";
        Instance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.doReturn(orderInstance)
                .when(this.localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
                .thenReturn(new HashMap<>());

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
                SshTunnelConnectionData.class))).thenReturn(false);

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getInstanceId(), test.getInstanceId());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    /**
     * Test if a fulfilled order of a failed localidentity compute instance is definitely changed to failed.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceFailed() throws Exception {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        String instanceId = "instanceid";
        Instance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(instanceId);

        Mockito.doReturn(orderInstance)
                .when(this.localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.eq(order.getId())))
                .thenReturn(new HashMap<>());

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getInstanceId(), test.getInstanceId());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    @Test
    public void testRunThrowableExceptionWhileTryingToProcessOrder() throws InterruptedException, UnexpectedException {
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException("Any Exception"))
                .when(this.fulfilledProcessor)
                .processFulfilledOrder(order);

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(500);
    }

    private Order createLocalOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = Mockito.mock(UserData.class);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String publicKey = "fake-public-key";

        Order localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey,
                        null);
        return localOrder;
    }

    private Order createRemoteOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = Mockito.mock(UserData.class);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember = "fake-intercomponent-member";
        String publicKey = "fake-public-key";

        Order remoteOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey,
                        null);
        return remoteOrder;
    }

}