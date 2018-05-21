package org.fogbowcloud.manager;

import static org.junit.Assert.*;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Before;
import org.junit.Test;

public class SharedOrderHoldersTest extends BaseUnitTests {

    private SharedOrderHolders instanceOne;
    private SharedOrderHolders instanceTwo;

    @Before
    public void initialize() {
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
