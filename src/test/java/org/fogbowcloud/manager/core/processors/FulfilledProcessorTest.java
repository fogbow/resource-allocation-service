package org.fogbowcloud.manager.core.processors;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
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
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
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

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String REMOTE_MEMBER_ID = "fake-intercomponent-member";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_SOURCE = "fake-source";
    private static final String FAKE_TARGET = "fake-target";
    private static final String FAKE_DEVICE = "fake-device";
    
    /** Maximum value that the thread should wait in sleep time */
    private static final int MAX_SLEEP_TIME = 33000;
    
    private static final int MIN_SLEEP_TIME = 500;

    private ChainedList failedOrderList;
    private ChainedList fulfilledOrderList;
    private FulfilledProcessor fulfilledProcessor;
    private LocalCloudConnector localCloudConnector;
    private Properties properties;
    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;
    private Thread thread;

    private Map<String, Integer> connectionAttempts;
    
    @Before
    public void setUp() {
        super.mockReadOrdersFromDataBase();
        
        HomeDir.getInstance().setPath(TEST_PATH);

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        
        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationConstants.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
        
        this.thread = null;
        
        this.connectionAttempts = new HashMap<>();
    }

    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunProcessWithExceededConnectionAttempts() throws InterruptedException, UnexpectedException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
         
        this.fulfilledProcessor = new FulfilledProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);

        Field declaredField = this.fulfilledProcessor.getClass().getDeclaredField("connectionAttempts");
        declaredField.setAccessible(true);
                
        this.connectionAttempts = (Map<String, Integer>) declaredField.get(this.fulfilledProcessor);
        
        for (int i = 0; i <= 1000; i++) {
            connectionAttempts.put(createOrder().getId(), i);
        }
              
        declaredField.set(this.fulfilledProcessor, this.connectionAttempts);
        
        Assert.assertEquals(this.connectionAttempts.size(), 1001);
        
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        
        Thread.sleep(MIN_SLEEP_TIME);

        Assert.assertEquals(this.connectionAttempts.size(), 0);
    }
    
    @Test
    public void testRunProcessLocalComputeOrderWithoutExternalAddresses()
            throws FogbowManagerException, UnexpectedException, InterruptedException {
        
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        Mockito.when(this.tunnelingService.getExternalServiceAddresses(Mockito.anyString()))
                .thenReturn(null);

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    @Test
    public void testRunProcessLocalWhenOrderTypeIsNotComputer()
            throws FogbowManagerException, UnexpectedException, InterruptedException {

        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String providingMember = LOCAL_MEMBER_ID;
        
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        Order order = new AttachmentOrder(federationUser, requestingMember, providingMember,
                FAKE_SOURCE, FAKE_TARGET, FAKE_DEVICE);
        
        order.setOrderState(OrderState.FULFILLED);
        
        this.fulfilledOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    @Test
    public void testRunProcessLocalComputeOrderWhenInstanceStateIsNotReady()
            throws FogbowManagerException, UnexpectedException, InterruptedException {
        
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.DISPATCHED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    @Test
    public void testRunProcessLocalComputeOrderWhenGetInstanceFailed()
            throws FogbowManagerException, UnexpectedException, InterruptedException {
        
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Mockito.doReturn(null).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

        Assert.assertNull(this.failedOrderList.getNext());

        spyFulfiledProcessor();

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Assert.assertNotNull(this.failedOrderList.getNext());
        Assert.assertNull(this.fulfilledOrderList.getNext());
    }

    @Test
    public void testProcessComputeOrderNotFulfilled()
            throws FogbowManagerException, UnexpectedException, InterruptedException {
        
        Order order = this.createOrder();
        order.setOrderState(OrderState.FAILED);
        this.failedOrderList.addItem(order);

        spyFulfiledProcessor();

        this.fulfilledProcessor.processFulfilledOrder(order);
        Assert.assertNotNull(this.failedOrderList.getNext());
    }

    @Test
    public void testRunProcessLocalComputeOrderWithoutLocalMember()
            throws FogbowManagerException, UnexpectedException, InterruptedException {
        
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        this.fulfilledProcessor = Mockito.spy(new FulfilledProcessor(REMOTE_MEMBER_ID,
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Assert.assertNotNull(this.fulfilledOrderList.getNext());
    }

    /**
     * Test if a fulfilled order of an active localidentity compute instance has not being changed
     * to failed if SSH connectivity is reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceReachable() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        Mockito.when(this.sshConnectivity
                .checkSSHConnectivity(Mockito.any(SshTunnelConnectionData.class))).thenReturn(true);

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Order test = this.fulfilledOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
    }

    /**
     * Test if a fulfilled order of an active localidentity compute instance is changed to failed if
     * SSH connectivity is not reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceNotReachable() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        Mockito.when(this.sshConnectivity
                .checkSSHConnectivity(Mockito.any(SshTunnelConnectionData.class)))
                .thenReturn(false);
        
        Assert.assertNull(this.failedOrderList.getNext());
        
        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        /** here may be a false positive depending on how long the machine will take to run the test */
        Thread.sleep(MAX_SLEEP_TIME);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
    }

    /**
     * Test if a fulfilled order of a failed localidentity compute instance is definitely changed to
     * failed.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceFailed() throws Exception {
        Order order = this.createOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Instance orderInstance = Mockito.spy(new ComputeInstance(FAKE_INSTANCE_ID));
        orderInstance.setState(InstanceState.FAILED);
        order.setInstanceId(FAKE_INSTANCE_ID);

        mockCloudConnectorFactory(orderInstance);

        Assert.assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);

        Assert.assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(order.getInstanceId(), test.getInstanceId());
        Assert.assertEquals(OrderState.FAILED, test.getOrderState());
    }

    @Test
    public void testRunThrowableExceptionWhileTryingToProcessOrder()
            throws InterruptedException, UnexpectedException {
        
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.fulfilledOrderList.addItem(order);

        spyFulfiledProcessor();

        Mockito.doThrow(new RuntimeException()).when(this.fulfilledProcessor)
                .processFulfilledOrder(order);

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);
    }

    @Test
    public void testThrowUnexpectedExceptionWhileTryingToProcessOrder()
            throws InterruptedException, UnexpectedException {
        
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.fulfilledOrderList.addItem(order);

        spyFulfiledProcessor();

        Mockito.doThrow(new UnexpectedException()).when(this.fulfilledProcessor)
                .processFulfilledOrder(order);

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(MIN_SLEEP_TIME);
    }

    private Order createOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = Mockito.mock(UserData.class);
        
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        Order order = new ComputeOrder(federationUser, requestingMember, providingMember, 8,
                1024, 30, FAKE_IMAGE_NAME, userData, FAKE_PUBLIC_KEY, null);
        
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
                this.tunnelingService, this.sshConnectivity,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME));
    }

}
