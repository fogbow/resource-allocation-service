package org.fogbowcloud.manager.core.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.RemoteCloudConnector;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ClosedProcessorTest extends BaseUnitTests {

    private ClosedProcessor closedProcessor;

    private RemoteCloudConnector remoteInstanceProvider;
    private LocalCloudConnector localInstanceProvider;

    private Properties properties;

    private Thread thread;
    private OrderController orderController;

    @Before
    public void setUp() {
        this.properties = new Properties();
        this.properties.setProperty(
                ConfigurationConstants.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        this.orderController = new OrderController("");

        this.localInstanceProvider = Mockito.mock(LocalCloudConnector.class);
        this.remoteInstanceProvider = Mockito.mock(RemoteCloudConnector.class);

        this.closedProcessor = Mockito.spy(new ClosedProcessor(this.orderController, ""));
    }

    @Override
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
        super.tearDown();
    }

    @Test
    public void testProcessClosedLocalOrder() throws Exception {
        String instanceId = "fake-id";
        Order localOrder = createLocalOrder(getLocalMemberId());
        localOrder.setInstanceId(instanceId);

        FederationUser federationUser = new FederationUser(0l, null);
        this.orderController.activateOrder(localOrder);

        OrderStateTransitioner.transition(localOrder, OrderState.CLOSED);

        Mockito.doNothing()
                .when(this.localInstanceProvider)
                .deleteInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.closedProcessor);
        this.thread.start();

        Thread.sleep(500);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        assertNull(activeOrdersMap.get(localOrder.getId()));

        closedOrders.resetPointer();
        assertNull(closedOrders.getNext());
    }

    @Test
    public void testProcessClosedLocalOrderFails() throws Exception {
        String instanceId = "fake-id";
        Order localOrder = createLocalOrder(getLocalMemberId());
        localOrder.setInstanceId(instanceId);

        FederationUser federationUser = new FederationUser(0l, null);
        this.orderController.activateOrder(localOrder);

        OrderStateTransitioner.transition(localOrder, OrderState.CLOSED);

        Mockito.doThrow(Exception.class)
                .when(this.localInstanceProvider)
                .deleteInstance(Mockito.any(Order.class));

        this.thread = new Thread(this.closedProcessor);
        this.thread.start();

        Thread.sleep(500);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        assertEquals(localOrder, activeOrdersMap.get(localOrder.getId()));

        closedOrders.resetPointer();
        assertEquals(localOrder, closedOrders.getNext());
    }
}
