package org.fogbowcloud.ras.core.models.linkedlists;

import org.fogbowcloud.ras.core.BaseUnitTests;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SynchronizedDoublyLinkedListTest extends BaseUnitTests {

    private SynchronizedDoublyLinkedList list;

    @Before
    public void initialize() throws UnexpectedException {
        mockReadOrdersFromDataBase();
        this.list = new SynchronizedDoublyLinkedList();
    }

    // test case: Adding an element to the list should reflect on getHead()
    // getCurrent() and getTail().
    @Test
    public void testAddFirst() {
        // verify
        Assert.assertNull(this.list.getHead());
        Assert.assertNull(this.list.getCurrent());
        Assert.assertNull(this.list.getTail());

        // set up
        Order order = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(order);

        // verify
        Assert.assertEquals(order, this.list.getHead().getOrder());
        Assert.assertEquals(order, this.list.getCurrent().getOrder());
        Assert.assertEquals(order, this.list.getTail().getOrder());
    }

    // test case: When a new element is added to the list and the current pointer
    // already points to the end of it, a newly added element should be the next element.
    @Test
    public void testAddOrder() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);

        // verify
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertEquals(orderTwo, this.list.getNext());
        // Next node of the second order is null.
        Assert.assertNull(this.list.getNext());

        // exercise
        this.list.addItem(orderThree);

        // verify
        // addItem() should have fixed this.current to point to the newly item added
        // to the tail of the list.
        Assert.assertEquals(orderThree, this.list.getNext());
    }

    // test case: Adding a null element to the list should throw an IllegalArgumentException.
    @Test
    public void testAddNullOrder() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderNull = null;
        try {
            // exercise
            this.list.addItem(orderNull);
            Assert.fail("Null order should not be added.");
        } catch (IllegalArgumentException e) {
            // verify
            Assert.assertEquals(Messages.Exception.ATTEMPTING_TO_ADD_A_NULL_REQUEST, e.getMessage());
        }
    }

    // test case: Navigating the list once, reseting the pointer and navigating again should
    // produce the same order of visited elements.
    @Test
    public void testResetPointer() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);

        // verify
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertEquals(orderTwo, this.list.getNext());
        Assert.assertNull(this.list.getNext());

        // exercise
        this.list.resetPointer();

        // verify
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertEquals(orderTwo, this.list.getNext());
        Assert.assertNull(this.list.getNext());
    }

    // test case: Removing a null order should throw an IllegalArgumentException.
    @Test
    public void testRemoveNullOrder() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderNull = null;
        try {
            // exercise
            this.list.removeItem(orderNull);
            Assert.fail("Null order should not be removed.");
        } catch (IllegalArgumentException e) {
            // verify
            Assert.assertEquals(Messages.Exception.ATTEMPTING_TO_REMOVE_A_NULL_REQUEST, e.getMessage());
        }
    }

    // test case: Searching for an element that was added to the list should
    // return that element.
    @Test
    public void testFindNodeToRemove() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());
        Order orderFour = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);
        this.list.addItem(orderFour);

        Node nodeToRemove = this.list.findNodeToRemove(orderTwo);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderTwo, nodeToRemove.getOrder());
        Assert.assertEquals(orderOne, nodeToRemove.getPrevious().getOrder());
        Assert.assertEquals(orderThree, nodeToRemove.getNext().getOrder());
        Assert.assertEquals(orderFour, this.list.getTail().getOrder());
    }

    // test case: Removing an element that is the head of the list should
    // update the references of the pointers head and current.
    @Test
    public void testRemoveItemOnHead() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());
        Order orderFour = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);
        this.list.addItem(orderFour);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderFour, this.list.getTail().getOrder());

        Assert.assertEquals(orderOne, this.list.getCurrent().getOrder());

        // exercise
        this.list.removeItem(orderOne);

        // verify
        Assert.assertEquals(orderTwo, this.list.getCurrent().getOrder());
        Assert.assertEquals(orderTwo, this.list.getHead().getOrder());
        Assert.assertNull(this.list.getHead().getPrevious());
        Assert.assertEquals(orderFour, this.list.getTail().getOrder());
        Assert.assertEquals(orderTwo, this.list.getNext());
        Assert.assertEquals(orderThree, this.list.getNext());
        Assert.assertEquals(orderFour, this.list.getNext());
        Assert.assertNull(this.list.getNext());
    }

    // test case: Removing an element that is the tail of the list should update
    // the tail pointer of the list.
    @Test
    public void testRemoveItemOnTail() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderThree, this.list.getTail().getOrder());

        // exercise
        this.list.removeItem(orderThree);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderTwo, this.list.getTail().getOrder());
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertEquals(orderTwo, this.list.getNext());
        Assert.assertNull(this.list.getNext());
    }

    // test case: A list containing one element should have the head, current,
    // tail and next pointers to null.
    @Test
    public void testRemoveItemOneElementOnList() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderOne, this.list.getCurrent().getOrder());
        Assert.assertEquals(orderOne, this.list.getTail().getOrder());

        // exercise
        this.list.removeItem(orderOne);

        // verify
        Assert.assertNull(this.list.getHead());
        Assert.assertNull(this.list.getCurrent());
        Assert.assertNull(this.list.getTail());
        Assert.assertNull(this.list.getNext());
    }

    // test case: Removing elements from the middle of the list should keep
    // its consistency. E.g.: removing 2 from [1, 2, 3] should produce [1, 3].
    @Test
    public void testRemoveItem() throws Exception {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());
        Order orderFour = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);
        this.list.addItem(orderFour);
        this.list.removeItem(orderThree);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderFour, this.list.getTail().getOrder());
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertEquals(orderTwo, this.list.getNext());
        Assert.assertEquals(orderFour, this.list.getNext());
        Assert.assertNull(this.list.getNext());

        // exercise
        this.list.resetPointer();
        this.list.removeItem(orderTwo);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderFour, this.list.getTail().getOrder());
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertEquals(orderFour, this.list.getNext());
        Assert.assertNull(this.list.getNext());
    }

    // test case: A list that had all of its elements removed should behave like
    // a newly created list.
    @Test
    public void testReinitializingList() {
        // verify
        Assert.assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);

        // verify
        Assert.assertEquals(orderOne, this.list.getHead().getOrder());
        Assert.assertEquals(orderOne, this.list.getCurrent().getOrder());
        Assert.assertEquals(orderOne, this.list.getTail().getOrder());
        Assert.assertEquals(orderOne, this.list.getNext());
        Assert.assertNull(this.list.getNext());

        // exercise
        this.list.removeItem(orderOne);

        // verify
        Assert.assertNull(this.list.getHead());
        Assert.assertNull(this.list.getCurrent());
        Assert.assertNull(this.list.getTail());
        Assert.assertNull(this.list.getNext());

        // exercise
        this.list.addItem(orderTwo);

        // verify
        Assert.assertEquals(orderTwo, this.list.getHead().getOrder());
        Assert.assertEquals(orderTwo, this.list.getCurrent().getOrder());
        Assert.assertEquals(orderTwo, this.list.getTail().getOrder());
        Assert.assertEquals(orderTwo, this.list.getNext());
        Assert.assertNull(this.list.getNext());
    }
}
