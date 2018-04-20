package org.fogbowcloud.manager.core.models.linkedlist;

import org.fogbowcloud.manager.core.models.orders.Order;

public class Cell {

	private Cell next;
	private Cell previous;
	private Order order;
	
	public Cell(Order order) {
		this.order = order;
	}
	
	public Cell(Cell previous, Order order, Cell next) {
		this.previous = previous;
		this.order = order;
		this.next = next;
	}

	public Cell getNext() {
		return next;
	}

	public void setNext(Cell next) {
		this.next = next;
	}

	public Cell getPrevious() {
		return previous;
	}

	public void setPrevious(Cell previous) {
		this.previous = previous;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}
	
}
