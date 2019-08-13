package cloud.fogbow.ras;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;

@PrepareForTest(DatabaseManager.class)
public class SharedOrderHoldersTest extends BaseUnitTests {

    private SharedOrderHolders instanceOne;
    private SharedOrderHolders instanceTwo;

    @Before
    public void initialize() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.instanceOne = SharedOrderHolders.getInstance();
        this.instanceTwo = SharedOrderHolders.getInstance();
    }

    // test case: As SynchronizedDoublyLinkedList is a sigleton object, when getting the
    // list twice (or more) it must point to the same reference, in other words,
    // they are the same object.
    @Test
    public void testGetSameListReference() {
        // set up
        SynchronizedDoublyLinkedList<Order> listFromInstanceOne = instanceOne.getOpenOrdersList();
        SynchronizedDoublyLinkedList<Order> listFromInstanceTwo = instanceTwo.getOpenOrdersList();

        // verify
        Assert.assertSame(listFromInstanceOne, listFromInstanceTwo);

        // exercise
        Order orderOne = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        listFromInstanceOne.addItem(orderOne);

        // verify
        Assert.assertSame(listFromInstanceOne.getCurrent(), listFromInstanceTwo.getCurrent());
        Assert.assertSame(orderOne, listFromInstanceOne.getCurrent().getValue());
        Assert.assertSame(orderOne, listFromInstanceTwo.getCurrent().getValue());

        // exercise
        Order orderTwo = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        listFromInstanceTwo.addItem(orderTwo);

        // verify
        Assert.assertSame(
                listFromInstanceOne.getCurrent().getNext(),
                listFromInstanceTwo.getCurrent().getNext());
        Assert.assertSame(orderTwo, listFromInstanceOne.getCurrent().getNext().getValue());
        Assert.assertSame(orderTwo, listFromInstanceTwo.getCurrent().getNext().getValue());
    }
}
