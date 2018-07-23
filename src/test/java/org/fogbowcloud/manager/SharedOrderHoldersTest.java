package org.fogbowcloud.manager;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseManager.class)
public class SharedOrderHoldersTest extends BaseUnitTests {

    private SharedOrderHolders instanceOne;
    private SharedOrderHolders instanceTwo;

    @Before
    public void initialize() {
        mockReadOrdersFromDataBase();

        this.instanceOne = SharedOrderHolders.getInstance();
        this.instanceTwo = SharedOrderHolders.getInstance();
    }

    // test case: As SynchronizedDoublyLinkedList is a sigleton object, when getting the
    // list twice (or more) it must point to the same reference, in other words,
    // they are the same object.
    @Test
    public void testGetSameListReference() {
        // set up
        SynchronizedDoublyLinkedList listFromInstanceOne = instanceOne.getOpenOrdersList();
        SynchronizedDoublyLinkedList listFromInstanceTwo = instanceTwo.getOpenOrdersList();

        // verify
        Assert.assertEquals(listFromInstanceOne, listFromInstanceTwo);

        // exercise
        Order orderOne = createLocalOrder(getLocalMemberId());
        listFromInstanceOne.addItem(orderOne);

        // verify
        Assert.assertEquals(listFromInstanceOne.getCurrent(), listFromInstanceTwo.getCurrent());
        Assert.assertEquals(orderOne, listFromInstanceOne.getCurrent().getOrder());
        Assert.assertEquals(orderOne, listFromInstanceTwo.getCurrent().getOrder());

        // exercise
        Order orderTwo = createLocalOrder(getLocalMemberId());
        listFromInstanceTwo.addItem(orderTwo);

        // verify
        Assert.assertEquals(
                listFromInstanceOne.getCurrent().getNext(),
                listFromInstanceTwo.getCurrent().getNext());
        Assert.assertEquals(orderTwo, listFromInstanceOne.getCurrent().getNext().getOrder());
        Assert.assertEquals(orderTwo, listFromInstanceTwo.getCurrent().getNext().getOrder());
    }
}
