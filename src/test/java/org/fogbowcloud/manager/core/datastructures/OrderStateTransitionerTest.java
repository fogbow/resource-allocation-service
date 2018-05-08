package org.fogbowcloud.manager.core.datastructures;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.mockito.internal.util.MockUtil;

import static org.mockito.BDDMockito.*;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SharedOrderHolders.class)
public class OrderStateTransitionerTest extends BaseUnitTests {

    private MockUtil mockUtil = new MockUtil();

    private Order createOrder(OrderState orderState) {
        Order order = this.createLocalOrder();
        order.setOrderState(orderState);

        OrderInstance orderInstance = new OrderInstance("fakeId");
        order.setOrderInstance(orderInstance);

        return order;
    }

    @Override
    public void tearDown() {
        SharedOrderHolders instance = SharedOrderHolders.getInstance();

        // Clearing the lists if we are not mocking them
        if (!mockUtil.isMock(instance)) {
            for (OrderState state : OrderState.values()) {
                SynchronizedDoublyLinkedList ordersList = instance.getOrdersList(state);

                ordersList.resetPointer();
                Order order;
                while ((order = ordersList.getNext()) != null) {
                    ordersList.removeItem(order);
                }
            }
        }
    }

    @Test
    public void testValidTransition() throws OrderStateTransitionException {
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        SharedOrderHolders orderHolders = SharedOrderHolders.getInstance();
        SynchronizedDoublyLinkedList openOrdersList = orderHolders.getOpenOrdersList();
        SynchronizedDoublyLinkedList spawningOrdersList = orderHolders.getSpawningOrdersList();

        Order order = createOrder(originState);
        openOrdersList.addItem(order);

        OrderStateTransitioner.transition(order, destinationState);
        assertNull(openOrdersList.getNext());
        assertEquals(order, spawningOrdersList.getNext());
    }

    @Test
    public void testTransitioningToTheCurrentState() {
        SharedOrderHolders orderHolders = SharedOrderHolders.getInstance();
        SynchronizedDoublyLinkedList openOrdersList = orderHolders.getOpenOrdersList();
        Order order = createOrder(OrderState.OPEN);
        openOrdersList.addItem(order);

        try {
            OrderStateTransitioner.transition(order, OrderState.OPEN);
            fail("Transitioning to the current state should not be permitted.");
        } catch (OrderStateTransitionException e) {
        }
    }

    @Test(expected = RuntimeException.class)
    public void testOriginListCannotBeFound() throws OrderStateTransitionException {
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        // origin list will fail to be found
        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);
        when(ordersHolder.getOrdersList(originState)).thenReturn(null);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Order order = createOrder(originState);
        OrderStateTransitioner.transition(order, destinationState);
    }

    @Test(expected = RuntimeException.class)
    public void testDestinationListCannotBeFound() throws OrderStateTransitionException {
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);
        when(ordersHolder.getOrdersList(originState)).thenReturn(new SynchronizedDoublyLinkedList());

        // destination list will fail to be found
        when(ordersHolder.getOrdersList(destinationState)).thenReturn(null);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Order order = createOrder(originState);
        OrderStateTransitioner.transition(order, destinationState);
    }

    @Test(expected = OrderStateTransitionException.class)
    public void testOriginListRemovalFailure() throws OrderStateTransitionException {
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        SynchronizedDoublyLinkedList origin = Mockito.mock(SynchronizedDoublyLinkedList.class);
        when(origin.removeItem(any(Order.class))).thenReturn(false);

        SynchronizedDoublyLinkedList destination = Mockito.mock(SynchronizedDoublyLinkedList.class);

        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);
        when(ordersHolder.getOrdersList(originState)).thenReturn(origin);
        when(ordersHolder.getOrdersList(destinationState)).thenReturn(destination);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Order order = createOrder(originState);
        OrderStateTransitioner.transition(order, destinationState);
    }
    
    private Order createLocalOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = "local-member";
		String providingMember = "local-member";
		String publicKey = "fake-public-key";
		Order localOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				imageName, userData, publicKey);
		return localOrder;
	}
}