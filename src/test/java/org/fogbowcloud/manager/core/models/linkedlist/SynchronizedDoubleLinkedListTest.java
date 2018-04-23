package org.fogbowcloud.manager.core.models.linkedlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Before;
import org.junit.Test;

public class SynchronizedDoubleLinkedListTest {

	private SynchronizedDoubleLinkedList list;
	
	@Before
	public void initialize() {
		this.list = new SynchronizedDoubleLinkedList();
	}
	
	@Test
	public void testAddFirst() {
		assertNull(this.list.getHead());
		assertNull(this.list.getTail());
		
		Order order = createOrder("0");
		this.list.addItem(order);

		assertEquals(order, this.list.getHead().getOrder());
		assertEquals(order, this.list.getCurrent().getOrder());
		assertEquals(order, this.list.getTail().getOrder());
	}
	
	@Test
	public void testAddOrder() {
		assertNull(this.list.getNext());
		
		Order orderOne = createOrder("one");
		Order orderTwo = createOrder("two");
		this.list.addItem(orderOne);
		this.list.addItem(orderTwo);
		
		assertEquals(orderOne, this.list.getNext());
		assertEquals(orderTwo, this.list.getNext());
		// Next node of the second order is null.
		assertNull(this.list.getNext());
		
		Order orderThree = createOrder("three");
		this.list.addItem(orderThree);

		// To access the third order, we should have reseted the pointer, for now the pointer is on null.
		assertNull(this.list.getNext());
	}
	
	@Test
	public void testAddNullOrder() {
		assertNull(this.list.getNext());
		
		Order orderNull = null;
		try {
			this.list.addItem(orderNull);
			fail("Null order should not be added.");
		} catch (IllegalArgumentException e){
			assertEquals("Order cannot be null.", e.getMessage());
		}
	}
	
	@Test
	public void testResetPointer() {
		assertNull(this.list.getNext());
		
		Order orderOne = createOrder("one");
		Order orderTwo = createOrder("two");
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
		assertNull(this.list.getNext());

		Order orderNull = null;
		try {
			this.list.removeItem(orderNull);
			fail("Null order should not be removed.");
		} catch (IllegalArgumentException e){
			assertEquals("Order cannot be null.", e.getMessage());
		}
	}
	
	@Test
	public void testFindNodeToRemove() {
		assertNull(this.list.getNext());

		Order orderOne = createOrder("one");
		Order orderTwo = createOrder("two");
		Order orderThree = createOrder("three");
		Order orderFour = createOrder("four");
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
		Order orderOne = createOrder("one"); 
		Order orderTwo = createOrder("two"); 
		Order orderThree = createOrder("three");
		Order orderFour = createOrder("four");
		
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
		Order orderOne = createOrder("one"); 
		Order orderTwo = createOrder("two"); 
		Order orderThree = createOrder("three");

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
		Order orderOne = createOrder("one"); 
		
		this.list.addItem(orderOne);
		
		assertEquals(orderOne, this.list.getHead().getOrder());
		assertEquals(orderOne, this.list.getTail().getOrder());
		this.list.removeItem(orderOne);
		assertNull(this.list.getHead());
		assertNull(this.list.getTail());
		assertNull(this.list.getNext());
	}	
	
	@Test
	public void testRemoveItem() throws Exception {
		Order orderOne = createOrder("one"); 
		Order orderTwo = createOrder("two"); 
		Order orderThree = createOrder("three"); 
		Order orderFour = createOrder("Four"); 
		
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
	public void testReinitializingList(){
		Order orderOne = createOrder("one");
		this.list.addItem(orderOne);
		assertEquals(orderOne, this.list.getHead().getOrder());
		assertEquals(orderOne, this.list.getCurrent().getOrder());
		assertEquals(orderOne, this.list.getTail().getOrder());
		assertEquals(orderOne, this.list.getNext());

		this.list.removeItem(orderOne);
		assertNull(this.list.getHead());
		assertNull(this.list.getCurrent());
		assertNull(this.list.getTail());
		assertNull(this.list.getNext());

		orderOne = createOrder("one");
		this.list.addItem(orderOne);
		assertEquals(orderOne, this.list.getHead().getOrder());
		assertEquals(orderOne, this.list.getCurrent().getOrder());
		assertEquals(orderOne, this.list.getTail().getOrder());
		assertEquals(orderOne, this.list.getNext());
		assertNull(this.list.getNext());
	}
	
	private Order createOrder(String orderId) {
		Order order = new ComputeOrder();
		order.setId(orderId);
		return order; 
	}

}
