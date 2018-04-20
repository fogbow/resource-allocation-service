package org.fogbowcloud.manager.core.model.linkedlist;

import static org.junit.Assert.*;

import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoubleLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Before;
import org.junit.Test;

public class SynchronizedDoubleLinkedListTest {

	private ChainedList list;
	
	@Before
	public void initialize() {
		list = new SynchronizedDoubleLinkedList();
	}
	
	@Test(expected=Exception.class)
	public void testCallResetPointWithAddItem() throws Exception {
		list.resetPointer();
	}
	
	@Test
	public void testAddItemAndGetNext() throws Exception {
		Order compute1 = new ComputeOrder(); compute1.setId("0001"); list.addItem(compute1);		
		list.resetPointer();
		assertEquals("0001", list.getNext().getId());
	}
	
	@Test
	public void testremoveItemOnHead() throws Exception {
		Order compute1 = new ComputeOrder(); compute1.setId("0001"); list.addItem(compute1);		
		Order compute2 = new ComputeOrder(); compute2.setId("0002"); list.addItem(compute2);
		Order compute3 = new ComputeOrder(); compute3.setId("0003"); list.addItem(compute3);
		
		list.removeItem(compute1);
		list.resetPointer();
		assertEquals("0002", list.getNext().getId());
	}
	
	@Test
	public void testremoveItem() throws Exception {
		Order compute1 = new ComputeOrder(); compute1.setId("0001"); list.addItem(compute1);		
		Order compute2 = new ComputeOrder(); compute2.setId("0002"); list.addItem(compute2);
		Order compute3 = new ComputeOrder(); compute3.setId("0003"); list.addItem(compute3);
		
		list.removeItem(compute2);
		list.resetPointer();
		assertEquals("0001", list.getNext().getId());
	}
	
	@Test
	public void testremoveItemOnTail() throws Exception {
		Order compute1 = new ComputeOrder(); compute1.setId("0001"); list.addItem(compute1);		
		Order compute2 = new ComputeOrder(); compute2.setId("0002"); list.addItem(compute2);
		Order compute3 = new ComputeOrder(); compute3.setId("0003"); list.addItem(compute3);
		
		list.removeItem(compute3);
		list.resetPointer();
		assertEquals("0001", list.getNext().getId());
	}

}
