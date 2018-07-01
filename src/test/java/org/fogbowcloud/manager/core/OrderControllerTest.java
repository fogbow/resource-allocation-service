package org.fogbowcloud.manager.core;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class OrderControllerTest extends BaseUnitTests {

    private OrderController ordersController;

    private Map<String, Order> activeOrdersMap;
    private ChainedList openOrdersList;
    private ChainedList pendingOrdersList;
    private ChainedList spawningOrdersList;
    private ChainedList fulfilledOrdersList;
    private ChainedList failedOrdersList;
    private ChainedList closedOrdersList;
    private String localMember = BaseUnitTests.LOCAL_MEMBER_ID;

    @Before
    public void setUp() {
        this.ordersController = new OrderController();

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();

        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        this.spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        this.closedOrdersList = sharedOrderHolders.getClosedOrdersList();
    }

    @Test
    public void testNewOrderRequest() throws UnexpectedException {
        ComputeOrder computeOrder = new ComputeOrder();
        @SuppressWarnings("unused")
        FederationUser federationUser = new FederationUser("fake-id", null);
        OrderStateTransitioner.activateOrder(computeOrder);
    }

    /**
     * There is no matching method in the 'OrdersController' class
     */
    @Ignore
    @Test
    public void testFailedNewOrderRequestOrderIsNull() {
        try {
            Order order = null;
            @SuppressWarnings("unused")
            FederationUser federationUser = new FederationUser("fake-id", null);
            OrderStateTransitioner.activateOrder(order);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test(expected = FogbowManagerException.class)
    public void testDeleteOrderStateClosed() throws UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.CLOSED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        this.ordersController.deleteOrder(computeOrder);
    }

    @Test
    public void testDeleteOrderStateFailed() throws UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.FAILED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStateFulfilled() throws UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.FULFILLED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStateSpawning() throws UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.SPAWNING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStatePending() throws UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.PENDING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStateOpen() throws UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.OPEN);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test(expected = FogbowManagerException.class)
    public void testDeleteNullOrder() throws UnexpectedException {
        this.ordersController.deleteOrder(null);
    }

    private String getComputeOrderCreationId(OrderState orderState) throws UnexpectedException {
        String orderId = null;

        FederationUser federationUser = new FederationUser("fake-id", null);
        ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
        computeOrder.setFederationUser(federationUser);
        computeOrder.setRequestingMember(this.localMember);
        computeOrder.setProvidingMember(this.localMember);
        computeOrder.setOrderState(orderState);

        orderId = computeOrder.getId();

        this.activeOrdersMap.put(orderId, computeOrder);

        switch (orderState) {
            case OPEN:
                this.openOrdersList.addItem(computeOrder);
                break;
            case PENDING:
                this.pendingOrdersList.addItem(computeOrder);
                break;
            case SPAWNING:
                this.spawningOrdersList.addItem(computeOrder);
                break;
            case FULFILLED:
                this.fulfilledOrdersList.addItem(computeOrder);
                break;
            case FAILED:
                this.failedOrdersList.addItem(computeOrder);
                break;
            case CLOSED:
                this.closedOrdersList.addItem(computeOrder);
        }

        return orderId;
    }
}
