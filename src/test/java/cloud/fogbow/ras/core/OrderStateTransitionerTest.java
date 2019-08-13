package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, OrderStateTransitioner.class})
public class OrderStateTransitionerTest extends BaseUnitTests {

    private MockUtil mockUtil = new MockUtil();

    @After
    public void tearDown() {
        SharedOrderHolders instance = SharedOrderHolders.getInstance();

        // Clearing the lists if we are not mocking them
        if (!mockUtil.isMock(instance)) {
            super.tearDown();
        }
    }

    private Order createOrder(OrderState orderState) {
        Order order = createLocalOrder(getLocalMemberId());
        order.setOrderStateInTestMode(orderState);
        return order;
    }

    // test case: When calling the transition() method, it must change the state of Order
    // passed as parameter to a specific OrderState. This Order defined originally with Open, will
    // be changed to the Spawning state, removed from the open orders list, and added to the spawning
    // orders list.
    @Test
    public void testTransitionToChangeOrderStateOpenToSpawningWithRemoteProvider() throws UnexpectedException {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        super.mockReadOrdersFromDataBase();

        SharedOrderHolders orderHolders = SharedOrderHolders.getInstance();

        SynchronizedDoublyLinkedList<Order> openOrdersList = orderHolders.getOpenOrdersList();
        SynchronizedDoublyLinkedList<Order> spawningOrdersList = orderHolders.getSpawningOrdersList();

        Order order = createOrder(originState);
        String remoteMember = "fake-remote-member";
        order.setRequester(remoteMember);
        openOrdersList.addItem(order);


        RemoteNotifyEventRequest remoteNotifyEventRequest = Mockito.mock(RemoteNotifyEventRequest.class);
        PowerMockito.spy(OrderStateTransitioner.class);
        BDDMockito.given(OrderStateTransitioner.createRemoteNotifyEventRequest(Mockito.any(), Mockito.any()))
                .willReturn(remoteNotifyEventRequest);

        Assert.assertNull(spawningOrdersList.getNext());

        // exercise
        OrderStateTransitioner.transition(order, destinationState);

        // verify
        Assert.assertEquals(order, spawningOrdersList.getNext());
        Assert.assertNull(openOrdersList.getNext());
    }

    // test case: When calling the transition() method and the origin list of the 'Order' is Null,
    // an unexpected exception is thrown.
    @Test(expected = UnexpectedException.class) // verify
    public void testOriginListCannotBeFound() throws UnexpectedException {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        // Origin list will fail to be found
        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Mockito.when(ordersHolder.getOrdersList(originState)).thenReturn(null);

        Order order = createOrder(originState);

        // exercise
        OrderStateTransitioner.transition(order, destinationState);
    }

    // test case: When calling the transition() method and the destination list of the 'Order' is
    // Null, an unexpected exception is thrown.
    @Test(expected = UnexpectedException.class) // verify
    public void testDestinationListCannotBeFound() throws UnexpectedException {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        SharedOrderHolders ordersHolder = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(ordersHolder);

        Mockito.when(ordersHolder.getOrdersList(originState))
                .thenReturn(new SynchronizedDoublyLinkedList<>());

        // Destination list will fail to be found
        Mockito.when(ordersHolder.getOrdersList(destinationState)).thenReturn(null);

        Order order = createOrder(originState);

        // exercise
        OrderStateTransitioner.transition(order, destinationState);
    }

}
