package org.fogbowcloud.manager.core.models.linkedlist;

import org.fogbowcloud.manager.core.models.orders.Order;

public class DoubleLinkedList implements ChainedList {

	private Cell head;
	private Cell tail;
	private Cell current;
	private int size;
	
	@Override
	public synchronized void addItem(Order order) {
		if (this.size == 0) {
			this.addFirst(order);
		} else {
			Cell newItem = new Cell(order);
			this.tail.setNext(newItem);
			newItem.setPrevious(this.tail);
			this.tail = newItem;
		}
		this.size++;
	}
	
	@Override
	public synchronized void resetPointer() throws Exception {
		if (size == 0) {
			throw new Exception("List is empty!");
		}
		this.current = this.head;		
	}
	
	@Override
	public synchronized Order getNext() throws Exception {
		if (this.size <= 0) {
			throw new Exception("List is empty!");
		}
		Order orderCurrent = this.current.getOrder();
		this.current = this.current.getNext();
		return orderCurrent;
	}
	
	@Override
	public boolean removeItem(Order order) throws Exception {
		this.resetPointer();
		do {
			if (this.current.getOrder().equals(order)) {
				if (this.current == this.head) {
					this.removeOnHead(order);
					return true;
				} else if (this.current == this.tail) {
					this.removeOnTail(order);
					return true;
				} else {
					Cell orderPrevious = this.current.getPrevious();
					Cell orderCurrent = orderPrevious.getNext();
					Cell orderNext = orderCurrent.getNext();
					orderPrevious.setNext(orderNext);
					orderNext.setPrevious(orderPrevious);
					this.size--;
					return true;
				}				
			}
			this.getNext();			
		} while (this.current != null); 
		return false;
	}

	private void addFirst(Order order) {
		Cell newItem = new Cell(this.head, order, this.tail);
		this.head = newItem;		
		if (this.size == 0) {
			this.tail = this.head;			
		} 
	}
	
	private void removeOnHead(Order order) {
		this.head = this.head.getNext();
		this.size--;
		if (this.size == 0) {
			this.tail = null;
		}
	}
	
	private void removeOnTail(Order order) {
		if (this.size == 1) {
			this.removeOnHead(order);
		} else {
			Cell penult = this.tail.getPrevious();
			penult.setNext(null);
			this.tail = penult;
			this.size--;
		}	
	}
	
}
