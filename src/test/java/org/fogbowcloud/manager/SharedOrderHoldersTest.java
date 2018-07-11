package org.fogbowcloud.manager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseManager.class)
public class SharedOrderHoldersTest extends BaseUnitTests {

    private SharedOrderHolders instanceOne;
    private SharedOrderHolders instanceTwo;

    @Before
    public void initialize() {
        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FAILED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList());

        PowerMockito.mockStatic(DatabaseManager.class);
        given(DatabaseManager.getInstance()).willReturn(databaseManager);

        this.instanceOne = SharedOrderHolders.getInstance();
        this.instanceTwo = SharedOrderHolders.getInstance();
    }

    @Test
    public void testGetSameListReference() {
        SynchronizedDoublyLinkedList listFromInstanceOne = instanceOne.getOpenOrdersList();
        SynchronizedDoublyLinkedList listFromInstanceTwo = instanceTwo.getOpenOrdersList();
        assertEquals(listFromInstanceOne, listFromInstanceTwo);

        Order orderOne = createLocalOrder(getLocalMemberId());
        listFromInstanceOne.addItem(orderOne);
        assertEquals(listFromInstanceOne.getCurrent(), listFromInstanceTwo.getCurrent());
        assertEquals(orderOne, listFromInstanceOne.getCurrent().getOrder());
        assertEquals(orderOne, listFromInstanceTwo.getCurrent().getOrder());

        Order orderTwo = createLocalOrder(getLocalMemberId());
        listFromInstanceTwo.addItem(orderTwo);
        assertEquals(
                listFromInstanceOne.getCurrent().getNext(),
                listFromInstanceTwo.getCurrent().getNext());
        assertEquals(orderTwo, listFromInstanceOne.getCurrent().getNext().getOrder());
        assertEquals(orderTwo, listFromInstanceTwo.getCurrent().getNext().getOrder());
    }
}
