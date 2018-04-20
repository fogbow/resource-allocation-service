package org.fogbowcloud.manager.core.models.linkedlist;

import org.fogbowcloud.manager.core.models.orders.Order;

public class SynchronizedDoubleLinkedList implements ChainedList {

	private Node head;
	private Node tail;
	private Node current;
	private int size;

	public SynchronizedDoubleLinkedList() {
		head = tail = current = null;
		size = 0;
	}

	public synchronized Node getCurrent() {
		return current;
	}

	public synchronized int getSize() {
		return size;
	}

	@Override
	public synchronized void addItem(Order order) {
		if (this.size == 0) {
			addFirst(order);
		} else {
			Node newItem = new Node(tail, null, null);
			tail.setNext(newItem);
			tail.setOrder(order);
			tail = newItem;
		}
		this.size++;
	}

	private synchronized void addFirst(Order order) {
		tail = new Node();
		head = new Node(null, order, tail);
		current = head;
		tail.setPrevious(head);
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

	/**
	 * This method removes a given order. Note that this remove method should not modify the current pointer
	 * (i.e., after removing, the current pointer, must point to the same element before this operation), unless
	 * the order to be removed, is the one in current pointer. In this case, we must remove this order, and point
	 * current to the next one.
	 * @param order
	 * @throws Exception
	 */
	@Override
	public synchronized void removeItem(Order order) throws Exception {
		// This Exception should be a particular one, not a general exception
		Node nodeToRemove = findNodeToRemove();
		if (nodeToRemove == head) {
			removeOnHead(order);
		} else if (nodeToRemove == tail) {
			removeOnTail(order);
		} else {
			//check if this order is the current, and point current to the next node, if it is;
			Node previousOrder = nodeToRemove.getPrevious();
			Node nextOrder = nodeToRemove.getNext();
			previousOrder.setNext(nextOrder);
			nextOrder.setPrevious(previousOrder);
		}
		this.size--;
	}

	private Node findNodeToRemove() {
		// TODO
		return null;
	}

	private synchronized  void removeOnHead(Order order) {
		this.head = this.head.getNext();
		this.size--;
		if (this.size == 0) {
			this.tail = null;
		}
	}

	private synchronized void removeOnTail(Order order) {
		if (this.size == 1) {
			this.removeOnHead(order);
		} else {
			Node penult = this.tail.getPrevious();
			penult.setNext(null);
			this.tail = penult;
			this.size--;
		}	
	}
	
}
