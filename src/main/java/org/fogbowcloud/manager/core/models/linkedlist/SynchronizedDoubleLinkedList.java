package org.fogbowcloud.manager.core.models.linkedlist;

import org.fogbowcloud.manager.core.models.orders.Order;

public class SynchronizedDoubleLinkedList implements ChainedList {

	private Node head;
	private Node tail;
	private Node current;

	public SynchronizedDoubleLinkedList() {
		this.head = this.tail = this.current = null;
	}

	public synchronized Node getCurrent() {
		return this.current;
	}

	protected Node getHead() {
		return this.head;
	}

	protected Node getTail() {
		return this.tail;
	}

	@Override
	public synchronized void addItem(Order order) {
		if (order == null) {
			throw new NullPointerException();
		}

		if (this.head == null) {
			addFirst(order);
		} else {
			Node newItem = new Node(this.tail, order, null);
			this.tail.setNext(newItem);
			this.tail = newItem;
		}
	}

	/**
	 * @param order - Never null
	 */
	protected synchronized void addFirst(Order order) {
		Node firstNode = new Node(null, order, null);
		this.tail = this.head = this.current = firstNode;
	}

	@Override
	public synchronized void resetPointer() {
		this.current = this.head;
	}

	@Override
	public synchronized Order getNext() {
		if (this.current == null) {
			return null;
		}

		Order orderCurrent = this.current.getOrder();
		Node nextNode = this.current.getNext();
		this.current = nextNode;
		return orderCurrent;
	}

	/**
	 * This method removes a given order. Note that this remove method should not
	 * modify the current pointer (i.e., after removing, the current pointer, must
	 * point to the same element before this operation), unless the order to be
	 * removed, is the one in current pointer. In this case, we must remove this
	 * order, and point current to the next one.
	 * 
	 * @param order
	 */
	@Override
	public synchronized boolean removeItem(Order order) {
		if (order == null) {
			return false;
		}
		// This Exception should be a particular one, not a general exception
		Node nodeToRemove = findNodeToRemove(order);
		if (nodeToRemove == null) {
			return false;
		}

		if (nodeToRemove == this.head) {
			removeOnHead();
		} else if (nodeToRemove == this.tail) {
			removeOnTail();
		} else {
			// check if this order is the current, and point current to the next node, if it
			// is;
			Node previousOrder = nodeToRemove.getPrevious();
			Node nextOrder = nodeToRemove.getNext();
			previousOrder.setNext(nextOrder);
			nextOrder.setPrevious(previousOrder);
		}
		return true;
	}

	/**
	 * @param order - Never null
	 */
	protected Node findNodeToRemove(Order order) {
		Node currentNode = this.head;
		while (currentNode != null) {
			String currentOrderId = currentNode.getOrder().getId();
			if (order.getId().equals(currentOrderId)) {
				return currentNode;
			}
			currentNode = currentNode.getNext();
		}
		return null;
	}

	private synchronized boolean removeOnHead() {
		if (this.head == null) {
			return false;
		}

		Node currentHead = this.head;
		this.head = currentHead.getNext();
		if (this.head == null) {
			this.tail = this.current = null;
			return true;
		}

		if (currentHead.equals(this.current)) {
			this.current = this.head;
		}
		if (this.head.getNext() == null) {
			this.tail = this.head;
		}

		return true;
	}

	private synchronized boolean removeOnTail() {
		if (this.tail == null) {
			return false;
		}

		Node previousTail = this.tail.getPrevious();
		boolean isLastObject = previousTail == null;
		if (isLastObject) {
			this.head = this.tail = this.current = null;
		} else {
			previousTail.setNext(null);
			this.tail = previousTail;
		}

		return true;
	}
}
