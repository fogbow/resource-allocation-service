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
import org.fogbowcloud.manager.util.connectivity.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.util.connectivity.SshConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.TunnelingServiceUtil;
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

    public static final String REMOTE_MEMBER_ID = "fake-intercomponent-member";
    public static final String FAKE_INSTANCE_ID = "fake-instance-id";
    
    private FulfilledProcessor fulfilledProcessor;
    private LocalCloudConnector localCloudConnector;
    private Properties properties;
    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;
    private Thread thread;
    private ChainedList fulfilledOrderList;
    private ChainedList failedOrderList;
    
    @Before
    public void setUp() {
        mockDB();
        HomeDir.getInstance().setPath("src/test/resources/private");

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationConstants.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
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

    @Test
    public void testRunProcessLocalComputeOrderWhenGetInstanceFailed() throws FogbowManagerException, UnexpectedException, InterruptedException {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Mockito.doReturn(null).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        
        Thread.sleep(500);

        Assert.assertNotNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }
    
    @Test
    public void testProcessComputeOrderNotFulfilled() throws FogbowManagerException, UnexpectedException, InterruptedException {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FAILED);
        this.failedOrderList.addItem(order);

        this.fulfilledProcessor.processFulfilledOrder(order);
        Assert.assertNotNull(this.failedOrderList.getNext());
    }
    
    @Test
    public void testRunProcessLocalComputeOrderWithoutLocalMember() throws FogbowManagerException, UnexpectedException, InterruptedException {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(REMOTE_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        
        Thread.sleep(500);

        Assert.assertNotNull(this.fulfilledOrderList.getNext());
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

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString())).thenReturn(localCloudConnector);

        Mockito.doReturn(orderInstance).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
                SshTunnelConnectionData.class))).thenReturn(true);

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        
        Thread.sleep(500);
        
        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
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

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString())).thenReturn(localCloudConnector);
        
        Mockito.doReturn(orderInstance)
                .when(this.localCloudConnector)
                .getInstance(Mockito.any(Order.class));
        
        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(Mockito.any(
                SshTunnelConnectionData.class))).thenReturn(false);

        Assert.assertNull(this.failedOrderList.getNext());
        
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        
        Thread.sleep(500);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
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

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString())).thenReturn(localCloudConnector);

        Mockito.doReturn(orderInstance).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
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
    
    @SuppressWarnings("unused")
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