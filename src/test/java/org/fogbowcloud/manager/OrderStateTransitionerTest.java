package org.fogbowcloud.manager;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, DatabaseManager.class})
public class OrderStateTransitionerTest extends BaseUnitTests {

    private MockUtil mockUtil = new MockUtil();

    private Order createOrder(OrderState orderState) {
        Order order = createLocalOrder(getLocalMemberId());
        order.setOrderState(orderState);
        return order;
    }

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
    }

    @After
    public void tearDown() {
        SharedOrderHolders instance = SharedOrderHolders.getInstance();

        /** Clearing the lists if we are not mocking them */
        if (!mockUtil.isMock(instance)) {
            for (OrderState state : OrderState.values()) {
                if (!state.equals(OrderState.DEACTIVATED)) {
                    SynchronizedDoublyLinkedList ordersList = instance.getOrdersList(state);

                    ordersList.resetPointer();
                    Order order;
                    while ((order = ordersList.getNext()) != null) {
                        ordersList.removeItem(order);
                    }
                }
            }
        }
    }

    // test case: When calling the transition() method passing an Order and a OrderState, this
    // order will be change to the specific OrderState, removing of the origin list, and adding in
    // that state list.
    @Test
    public void testValidTransition() throws UnexpectedException {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN))
                .thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING))
                .thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED))
                .thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED))
                .thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING))
                .thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.CLOSED))
                .thenReturn(new SynchronizedDoublyLinkedList());

        PowerMockito.mockStatic(DatabaseManager.class);
        BDDMockito.given(DatabaseManager.getInstance()).willReturn(databaseManager);

        SharedOrderHolders orderHolders = SharedOrderHolders.getInstance();

        SynchronizedDoublyLinkedList openOrdersList = orderHolders.getOpenOrdersList();
        SynchronizedDoublyLinkedList spawningOrdersList = orderHolders.getSpawningOrdersList();

        Order order = createOrder(originState);
        openOrdersList.addItem(order);

        // exercise
        OrderStateTransitioner.transition(order, destinationState);

        // verify
        Assert.assertNull(openOrdersList.getNext());
        Assert.assertEquals(order, spawningOrdersList.getNext());
    }

    // test case: When calling the transition() method and the origin list of the 'Order' is Null,
    // will be throws an unexpected exception.
    @Test(expected = UnexpectedException.class) // verify
    public void testOriginListCannotBeFound() throws UnexpectedException {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        /** Origin list will fail to be found */
        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Mockito.when(ordersHolder.getOrdersList(originState)).thenReturn(null);

        Order order = createOrder(originState);

        // exercise
        OrderStateTransitioner.transition(order, destinationState);
    }

    // test case: When calling the transition() method and the destination list of the 'Order' is
    // Null, will be throws an unexpected exception.
    @Test(expected = UnexpectedException.class) // verify
    public void testDestinationListCannotBeFound() throws UnexpectedException {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Mockito.when(ordersHolder.getOrdersList(originState))
                .thenReturn(new SynchronizedDoublyLinkedList());

        /** Destination list will fail to be found */
        Mockito.when(ordersHolder.getOrdersList(destinationState)).thenReturn(null);

        Order order = createOrder(originState);

        // exercise
        OrderStateTransitioner.transition(order, destinationState);
    }

    // test case: When calling the transition() method and fail to remove an Order of the origin
    // list, must not throws an UnexpectedException.
    @Test
    public void testOriginListRemovalFailure() {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        SynchronizedDoublyLinkedList origin = Mockito.mock(SynchronizedDoublyLinkedList.class);
        SynchronizedDoublyLinkedList destination = Mockito.mock(SynchronizedDoublyLinkedList.class);

        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Mockito.when(ordersHolder.getOrdersList(originState)).thenReturn(origin);
        Mockito.when(ordersHolder.getOrdersList(destinationState)).thenReturn(destination);

        Mockito.when(origin.removeItem(Mockito.any(Order.class))).thenReturn(false);

        Order order = createOrder(originState);

        try {

            // exercise
            OrderStateTransitioner.transition(order, destinationState);
        } catch (UnexpectedException e) {

            // verify
            Assert.fail();
        }
    }
}
