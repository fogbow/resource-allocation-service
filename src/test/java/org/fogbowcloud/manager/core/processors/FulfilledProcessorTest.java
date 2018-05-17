package org.fogbowcloud.manager.core.processors;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Properties;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class FulfilledProcessorTest extends BaseUnitTests {

    private FulfilledProcessor fulfilledProcessor;

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;

    private Properties properties;

    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;

    private Thread thread;

    private ChainedList fulfilledOrderList;
    private ChainedList failedOrderList;

    @Before
    public void setUp() {
        this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
        this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

        this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
        this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);

        this.properties = new Properties();
        this.properties.put(ConfigurationConstants.XMPP_ID_KEY, "local-member");

        this.thread = null;

        this.fulfilledProcessor = Mockito
                .spy(new FulfilledProcessor(this.localInstanceProvider, this.remoteInstanceProvider,
                        this.tunnelingService, this.sshConnectivity, this.properties));

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
     * Test if a fulfilled order of an active local compute instance has not
     * being changed to failed if SSH connectivity is reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceReachable() throws Exception {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        OrderInstance orderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
        orderInstance.setState(InstanceState.ACTIVE);
        order.setOrderInstance(orderInstance);

        Mockito.doReturn(orderInstance).when(this.localInstanceProvider).getInstance(any(Order.class));

        Mockito.doNothing().when((ComputeOrderInstance) orderInstance)
                .setExternalServiceAddresses(anyMapOf(String.class, String.class));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeOrderInstance.class))).thenReturn(true);

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNotNull(this.fulfilledOrderList.getNext());
        assertNull(this.failedOrderList.getNext());
    }

    /**
     * Test if a fulfilled order of an active remote compute instance has not
     * being changed to failed if SSH connectivity is reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessRemoteComputeOrderInstanceReachable() throws Exception {
        Order order = this.createRemoteOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        OrderInstance orderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
        orderInstance.setState(InstanceState.ACTIVE);
        order.setOrderInstance(orderInstance);

        Mockito.doReturn(orderInstance).when(this.remoteInstanceProvider).getInstance(any(Order.class));

        Mockito.doNothing().when((ComputeOrderInstance) orderInstance)
                .setExternalServiceAddresses(anyMapOf(String.class, String.class));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeOrderInstance.class))).thenReturn(true);

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNotNull(this.fulfilledOrderList.getNext());
        assertNull(this.failedOrderList.getNext());
    }

    /**
     * Test if a fulfilled order of an active local compute instance is changed
     * to failed if SSH connectivity is not reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceNotReachable() throws Exception {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        OrderInstance orderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
        orderInstance.setState(InstanceState.ACTIVE);
        order.setOrderInstance(orderInstance);

        Mockito.doReturn(orderInstance).when(this.localInstanceProvider).getInstance(any(Order.class));

        Mockito.doNothing().when((ComputeOrderInstance) orderInstance)
                .setExternalServiceAddresses(anyMapOf(String.class, String.class));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeOrderInstance.class))).thenReturn(false);

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getOrderInstance(), test.getOrderInstance());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    /**
     * Test if a fulfilled order of an active remote compute instance is changed
     * to failed if SSH connectivity is not reachable.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessRemoteComputeOrderInstanceNotReachable() throws Exception {
        Order order = this.createRemoteOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        OrderInstance orderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
        orderInstance.setState(InstanceState.ACTIVE);
        order.setOrderInstance(orderInstance);

        Mockito.doReturn(orderInstance).when(this.remoteInstanceProvider).getInstance(any(Order.class));

        Mockito.doNothing().when((ComputeOrderInstance) orderInstance)
                .setExternalServiceAddresses(anyMapOf(String.class, String.class));

        Mockito.when(this.sshConnectivity.checkSSHConnectivity(any(ComputeOrderInstance.class))).thenReturn(false);

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getOrderInstance(), test.getOrderInstance());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    /**
     * Test if a fulfilled order of a failed local compute instance is
     * definitely changed to failed.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessLocalComputeOrderInstanceFailed() throws Exception {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        OrderInstance orderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
        orderInstance.setState(InstanceState.FAILED);
        order.setOrderInstance(orderInstance);

        Mockito.doReturn(orderInstance).when(this.localInstanceProvider).getInstance(any(Order.class));

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getOrderInstance(), test.getOrderInstance());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    /**
     * Test if a fulfilled order of an failed remote compute instance is
     * definitely changed to failed.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRunProcessRemoteComputeOrderInstanceFailed() throws Exception {
        Order order = this.createRemoteOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        OrderInstance orderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
        orderInstance.setState(InstanceState.FAILED);
        order.setOrderInstance(orderInstance);

        Mockito.doReturn(orderInstance).when(this.remoteInstanceProvider).getInstance(any(Order.class));

        assertNull(this.failedOrderList.getNext());

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(this.fulfilledOrderList.getNext());

        Order test = this.failedOrderList.getNext();
        assertNotNull(test);
        assertEquals(order.getOrderInstance(), test.getOrderInstance());
        assertEquals(OrderState.FAILED, test.getOrderState());
    }

    /**
     * Test if the fulfilled processor still running and do not change the order
     * state if the method processFulfilledOrder throw an order state transition
     * exception.
     *
     * @throws OrderStateTransitionException
     * @throws InterruptedException
     */
    @Test
    public void testProcessFulfilledOrderThrowingOrderStateTransitionException()
            throws OrderStateTransitionException, InterruptedException {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(OrderStateTransitionException.class).when(this.fulfilledProcessor)
                .processFulfilledOrder(Mockito.any(Order.class));

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();

        Thread.sleep(500);

        Order test = this.fulfilledOrderList.getNext();
        assertEquals(order.getOrderInstance(), test.getOrderInstance());
        assertEquals(OrderState.FULFILLED, order.getOrderState());
    }

    @Test
    public void testRunThrowableExceptionWhileTryingToProcessOrder() throws InterruptedException, OrderStateTransitionException {
        Order order = Mockito.mock(Order.class);
        OrderState state = null;
        order.setOrderState(state);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new RuntimeException("Any Exception")).when(this.fulfilledProcessor)
                .processFulfilledOrder(order);

        this.thread = new Thread(this.fulfilledProcessor);
        this.thread.start();
        Thread.sleep(500);
    }

    @Test
    public void testRunExceptionWhileTryingToProcessInstance() throws OrderStateTransitionException {
        Order order = this.createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);
        this.fulfilledOrderList.addItem(order);

        Mockito.doThrow(new OrderStateTransitionException("Any Exception")).when(this.fulfilledProcessor)
                .processInstance(order);

        this.fulfilledProcessor.processFulfilledOrder(order);
    }

    private Order createLocalOrder() {
        Token localToken = Mockito.mock(Token.class);
        Token federationToken = Mockito.mock(Token.class);
        UserData userData = Mockito.mock(UserData.class);
        String imageName = "fake-image-name";
        String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
        String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
        String publicKey = "fake-public-key";

        Order localOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
                imageName, userData, publicKey);
        return localOrder;
    }

    private Order createRemoteOrder() {
        Token localToken = Mockito.mock(Token.class);
        Token federationToken = Mockito.mock(Token.class);
        UserData userData = Mockito.mock(UserData.class);
        String imageName = "fake-image-name";
        String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
        String providingMember = "fake-remote-member";
        String publicKey = "fake-public-key";

        Order remoteOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024,
                30, imageName, userData, publicKey);
        return remoteOrder;
    }
}
