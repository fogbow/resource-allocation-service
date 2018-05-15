package org.fogbowcloud.manager.core.threads;

import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.*;

public class TestClosedComputeOrdersProcessor extends BaseUnitTests {

    private ClosedComputeOrdersProcessor closedComputeOrdersProcessor;

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;

    private Properties properties;

    private Thread thread;

    @Before
    public void setUp() {
        String localMemberId = "local-member";
        this.properties = new Properties();
        this.properties.setProperty(ConfigurationConstants.XMPP_ID_KEY, localMemberId);

        this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
        this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

        this.closedComputeOrdersProcessor = Mockito
                .spy(new ClosedComputeOrdersProcessor(this.properties, this.localInstanceProvider, this.remoteInstanceProvider));
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
        OrderInstance orderInstance = new OrderInstance("fake-id");
        Order localOrder = createLocalOrder(getLocalMemberId());
        localOrder.setOrderInstance(orderInstance);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();

        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        activeOrdersMap.put(localOrder.getId(), localOrder);
        closedOrders.addItem(localOrder);

        Mockito.doNothing().when(this.localInstanceProvider)
                .deleteInstance(Mockito.any(Token.class), Mockito.any(OrderInstance.class));

        this.thread = new Thread(this.closedComputeOrdersProcessor);
        this.thread.start();

        Thread.sleep(500);

        assertNull(activeOrdersMap.get(localOrder.getId()));

        closedOrders.resetPointer();
        assertNull(closedOrders.getNext());
    }

    @Test
    public void testProcessClosedLocalOrderFails() throws Exception {
        OrderInstance orderInstance = new OrderInstance("fake-id");
        Order localOrder = createLocalOrder(getLocalMemberId());
        localOrder.setOrderInstance(orderInstance);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();

        activeOrdersMap.put(localOrder.getId(), localOrder);
        closedOrders.addItem(localOrder);

        Mockito.doThrow(Exception.class).when(this.localInstanceProvider)
                .deleteInstance(Mockito.any(Token.class), Mockito.any(OrderInstance.class));

        this.thread = new Thread(this.closedComputeOrdersProcessor);
        this.thread.start();

        assertEquals(localOrder, activeOrdersMap.get(localOrder.getId()));

        closedOrders.resetPointer();
        assertEquals(localOrder, closedOrders.getNext());
    }

}
