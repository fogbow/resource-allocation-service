package org.fogbowcloud.manager.core.models.linkedlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Before;
import org.junit.Test;

public class SynchronizedDoublyLinkedListTest extends BaseUnitTests {

    private SynchronizedDoublyLinkedList list;

    @Before
    public void initialize() {
        this.list = new SynchronizedDoublyLinkedList();
    }

    @Test
    public void testAddFirst() {
        assertNull(this.list.getHead());
        assertNull(this.list.getCurrent());
        assertNull(this.list.getTail());

        Order order = createLocalOrder(getLocalMemberId());
        this.list.addItem(order);

        assertEquals(order, this.list.getHead().getOrder());
        assertEquals(order, this.list.getCurrent().getOrder());
        assertEquals(order, this.list.getTail().getOrder());
    }

    @Test
    public void testAddOrder() {
        assertNull(this.list.getHead());

        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);

        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        // Next node of the second order is null.
        assertNull(this.list.getNext());

        Order orderThree = createLocalOrder(getLocalMemberId());
        this.list.addItem(orderThree);

        // addItem() should have fixed this.current to point to the newly item added to the tail of the list.
        assertEquals(orderThree, this.list.getNext());
    }

    @Test
    public void testAddNullOrder() {
        assertNull(this.list.getHead());

        Order orderNull = null;
        try {
            this.list.addItem(orderNull);
            fail("Null order should not be added.");
        } catch (IllegalArgumentException e) {
            assertEquals("Attempting to add a null order.", e.getMessage());
        }
    }

    @Test
    public void testResetPointer() {
        assertNull(this.list.getHead());

        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);

        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());

        this.list.resetPointer();
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveNullOrder() {
        assertNull(this.list.getHead());

        Order orderNull = null;
        try {
            this.list.removeItem(orderNull);
            fail("Null order should not be removed.");
        } catch (IllegalArgumentException e) {
            assertEquals("Attempting to remove a null order.", e.getMessage());
        }
    }

    @Test
    public void testFindNodeToRemove() {
        assertNull(this.list.getHead());

        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());
        Order orderFour = createLocalOrder(getLocalMemberId());
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);
        this.list.addItem(orderFour);

        Node nodeToRemove = this.list.findNodeToRemove(orderTwo);
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderTwo, nodeToRemove.getOrder());
        assertEquals(orderOne, nodeToRemove.getPrevious().getOrder());
        assertEquals(orderThree, nodeToRemove.getNext().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());
    }

    @Test
    public void testRemoveItemOnHead() {
        assertNull(this.list.getHead());

        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());
        Order orderFour = createLocalOrder(getLocalMemberId());

        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);
        this.list.addItem(orderFour);

        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());

        assertEquals(orderOne, this.list.getCurrent().getOrder());
        this.list.removeItem(orderOne);
        assertEquals(orderTwo, this.list.getCurrent().getOrder());
        assertEquals(orderTwo, this.list.getHead().getOrder());
        assertNull(this.list.getHead().getPrevious());
        assertEquals(orderFour, this.list.getTail().getOrder());
        assertEquals(orderTwo, this.list.getNext());
        assertEquals(orderThree, this.list.getNext());
        assertEquals(orderFour, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveItemOnTail() {
        assertNull(this.list.getHead());
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());

        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);

        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderThree, this.list.getTail().getOrder());

        this.list.removeItem(orderThree);
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderTwo, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveItemOneElementOnList() {
        assertNull(this.list.getHead());
        Order orderOne = createLocalOrder(getLocalMemberId());

        this.list.addItem(orderOne);

        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderOne, this.list.getCurrent().getOrder());
        assertEquals(orderOne, this.list.getTail().getOrder());
        this.list.removeItem(orderOne);
        assertNull(this.list.getHead());
        assertNull(this.list.getCurrent());
        assertNull(this.list.getTail());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveItem() throws Exception {
        assertNull(this.list.getHead());

        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());
        Order orderFour = createLocalOrder(getLocalMemberId());

        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);
        this.list.addItem(orderFour);

        this.list.removeItem(orderThree);
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertEquals(orderFour, this.list.getNext());
        assertNull(this.list.getNext());

        this.list.resetPointer();
        this.list.removeItem(orderTwo);
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderFour, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testReinitializingList() {
        assertNull(this.list.getHead());

        Order orderOne = createLocalOrder(getLocalMemberId());
        this.list.addItem(orderOne);
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderOne, this.list.getCurrent().getOrder());
        assertEquals(orderOne, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertNull(this.list.getNext());

        this.list.removeItem(orderOne);
        assertNull(this.list.getHead());
        assertNull(this.list.getCurrent());
        assertNull(this.list.getTail());
        assertNull(this.list.getNext());

        orderOne = createLocalOrder(getLocalMemberId());
        this.list.addItem(orderOne);
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderOne, this.list.getCurrent().getOrder());
        assertEquals(orderOne, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertNull(this.list.getNext());
    }

}
