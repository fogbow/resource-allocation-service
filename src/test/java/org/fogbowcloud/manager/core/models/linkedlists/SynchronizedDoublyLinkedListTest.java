package org.fogbowcloud.manager.core.models.linkedlists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SynchronizedDoublyLinkedListTest extends BaseUnitTests {

    private SynchronizedDoublyLinkedList list;

    @Before
    public void initialize() {
        mockReadOrdersFromDataBase();
        this.list = new SynchronizedDoublyLinkedList();
    }

    @Test
    public void testAddFirst() {
        // verify
        assertNull(this.list.getHead());
        assertNull(this.list.getCurrent());
        assertNull(this.list.getTail());

        // set up
        Order order = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(order);

        // verify
        assertEquals(order, this.list.getHead().getOrder());
        assertEquals(order, this.list.getCurrent().getOrder());
        assertEquals(order, this.list.getTail().getOrder());
    }

    @Test
    public void testAddOrder() {
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);

        // verify
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        // Next node of the second order is null.
        assertNull(this.list.getNext());

        // exercise
        this.list.addItem(orderThree);

        // verify
        // addItem() should have fixed this.current to point to the newly item added
        // to the tail of the list.
        assertEquals(orderThree, this.list.getNext());
    }

    @Test
    public void testAddNullOrder() {
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderNull = null;
        try {
            // exericse
            this.list.addItem(orderNull);
            fail("Null order should not be added.");
        } catch (IllegalArgumentException e) {
            // verify
            assertEquals("Attempting to add a null order.", e.getMessage());
        }
    }

    @Test
    public void testResetPointer() {
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);

        // verify
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());

        // exercise
        this.list.resetPointer();

        // verify
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveNullOrder() {
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderNull = null;
        try {
            // exercise
            this.list.removeItem(orderNull);
            fail("Null order should not be removed.");
        } catch (IllegalArgumentException e) {
            // verify
            assertEquals("Attempting to remove a null order.", e.getMessage());
        }
    }

    @Test
    public void testFindNodeToRemove() {
        // verify
        assertNull(this.list.getHead());

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
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderTwo, nodeToRemove.getOrder());
        assertEquals(orderOne, nodeToRemove.getPrevious().getOrder());
        assertEquals(orderThree, nodeToRemove.getNext().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());
    }

    @Test
    public void testRemoveItemOnHead() {
        // verify
        assertNull(this.list.getHead());

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
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());

        assertEquals(orderOne, this.list.getCurrent().getOrder());

        // exercise
        this.list.removeItem(orderOne);

        // verify
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
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());
        Order orderThree = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);
        this.list.addItem(orderTwo);
        this.list.addItem(orderThree);

        // verify
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderThree, this.list.getTail().getOrder());

        // exercise
        this.list.removeItem(orderThree);

        // verify
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderTwo, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveItemOneElementOnList() {
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);

        // verify
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderOne, this.list.getCurrent().getOrder());
        assertEquals(orderOne, this.list.getTail().getOrder());

        // exercise
        this.list.removeItem(orderOne);

        // verify
        assertNull(this.list.getHead());
        assertNull(this.list.getCurrent());
        assertNull(this.list.getTail());
        assertNull(this.list.getNext());
    }

    @Test
    public void testRemoveItem() throws Exception {
        // verify
        assertNull(this.list.getHead());

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
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderTwo, this.list.getNext());
        assertEquals(orderFour, this.list.getNext());
        assertNull(this.list.getNext());

        // exercise
        this.list.resetPointer();
        this.list.removeItem(orderTwo);

        // verify
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderFour, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertEquals(orderFour, this.list.getNext());
        assertNull(this.list.getNext());
    }

    @Test
    public void testReinitializingList() {
        // verify
        assertNull(this.list.getHead());

        // set up
        Order orderOne = createLocalOrder(getLocalMemberId());
        Order orderTwo = createLocalOrder(getLocalMemberId());

        // exercise
        this.list.addItem(orderOne);

        // verify
        assertEquals(orderOne, this.list.getHead().getOrder());
        assertEquals(orderOne, this.list.getCurrent().getOrder());
        assertEquals(orderOne, this.list.getTail().getOrder());
        assertEquals(orderOne, this.list.getNext());
        assertNull(this.list.getNext());

        // exercise
        this.list.removeItem(orderOne);

        // verify
        assertNull(this.list.getHead());
        assertNull(this.list.getCurrent());
        assertNull(this.list.getTail());
        assertNull(this.list.getNext());

        // exercise
        this.list.addItem(orderTwo);

        // verify
        assertEquals(orderTwo, this.list.getHead().getOrder());
        assertEquals(orderTwo, this.list.getCurrent().getOrder());
        assertEquals(orderTwo, this.list.getTail().getOrder());
        assertEquals(orderTwo, this.list.getNext());
        assertNull(this.list.getNext());
    }
}
