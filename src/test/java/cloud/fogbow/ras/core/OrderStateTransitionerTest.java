package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, DatabaseManager.class, OrderStateTransitioner.class})
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
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setOrderStateInTestMode(orderState);
        return order;
    }

    // test case: When calling the transition() method, it must invoke
    // doTransition() to change order's state.
    @Test
    public void testTransitionToChangeOrderStateOpenToSpawning() throws Exception {

        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        this.testUtils.mockReadOrdersFromDataBase();

        Order order = createOrder(originState);

        PowerMockito.spy(OrderStateTransitioner.class);
        PowerMockito.doNothing().when(OrderStateTransitioner.class,
                "doTransition", Mockito.any(), Mockito.any());

        // exercise
        OrderStateTransitioner.transition(order, destinationState);

        // verify
        PowerMockito.verifyStatic(OrderStateTransitioner.class, Mockito.times(1));
        OrderStateTransitioner.doTransition(Mockito.any(), Mockito.any());
    }

    // test case: When calling the transition() method, it must change the state of Order
    // passed as parameter to a specific OrderState. This Order defined originally with Open, will
    // be changed to the Spawning state, removed from the open orders list, and added to the spawning
    // orders list.
    @Test
    public void testDoTransitionToChangeOrderStateOpenToSpawning() throws UnexpectedException {
        // set up
        OrderState originState = OrderState.OPEN;
        OrderState destinationState = OrderState.SPAWNING;

        this.testUtils.mockReadOrdersFromDataBase();

        SharedOrderHolders orderHolders = SharedOrderHolders.getInstance();

        SynchronizedDoublyLinkedList<Order> openOrdersList = orderHolders.getOpenOrdersList();
        SynchronizedDoublyLinkedList<Order> spawningOrdersList = orderHolders.getSpawningOrdersList();

        Order order = createOrder(originState);
        openOrdersList.addItem(order);

        Assert.assertNull(spawningOrdersList.getNext());

        // exercise
        OrderStateTransitioner.doTransition(order, destinationState);

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
        OrderStateTransitioner.doTransition(order, destinationState);
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
        OrderStateTransitioner.doTransition(order, destinationState);
    }

}
