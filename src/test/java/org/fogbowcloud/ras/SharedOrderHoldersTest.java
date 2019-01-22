package org.fogbowcloud.ras;

import org.fogbowcloud.ras.core.BaseUnitTests;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.ras.core.models.orders.Order;
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
    public void initialize() throws UnexpectedException {
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
        Assert.assertSame(listFromInstanceOne, listFromInstanceTwo);

        // exercise
        Order orderOne = createLocalOrder(getLocalMemberId());
        listFromInstanceOne.addItem(orderOne);

        // verify
        Assert.assertSame(listFromInstanceOne.getCurrent(), listFromInstanceTwo.getCurrent());
        Assert.assertSame(orderOne, listFromInstanceOne.getCurrent().getOrder());
        Assert.assertSame(orderOne, listFromInstanceTwo.getCurrent().getOrder());

        // exercise
        Order orderTwo = createLocalOrder(getLocalMemberId());
        listFromInstanceTwo.addItem(orderTwo);

        // verify
        Assert.assertSame(
                listFromInstanceOne.getCurrent().getNext(),
                listFromInstanceTwo.getCurrent().getNext());
        Assert.assertSame(orderTwo, listFromInstanceOne.getCurrent().getNext().getOrder());
        Assert.assertSame(orderTwo, listFromInstanceTwo.getCurrent().getNext().getOrder());
    }
}
