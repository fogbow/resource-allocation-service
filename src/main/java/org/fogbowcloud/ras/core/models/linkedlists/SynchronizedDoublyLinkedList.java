package org.fogbowcloud.ras.core.models.linkedlists;

import org.fogbowcloud.ras.core.models.orders.Order;

public class SynchronizedDoublyLinkedList implements ChainedList {
    private Node head;
    private Node tail;
    private Node current;

    public SynchronizedDoublyLinkedList() {
        this.head = this.tail = this.current = null;
    }

    public synchronized Node getCurrent() {
        return this.current;
    }

    public synchronized Node getHead() {
        return this.head;
    }

    public synchronized Node getTail() {
        return this.tail;
    }

    @Override
    public synchronized void addItem(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Attempting to add a null order.");
        }
        if (this.head == null) {
            Node firstNode = new Node(null, order, null);
            this.tail = this.head = this.current = firstNode;
        } else {
            Node newItem = new Node(this.tail, order, null);
            this.tail.setNext(newItem);
            this.tail = newItem;
            /**
             * The check below is useful when current pointer just passed by all the list and was
             * pointing to null, and a new order was inserted at the end of this list. So, current
             * should point to the new inserted order (new tail), instead of null.
             */
            if (this.current == null) {
                this.current = this.tail;
            }
        }
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
        this.current = this.current.getNext();
        return orderCurrent;
    }

    /**
     * This method removes a given order. Note that this remove method should not modify the current
     * pointer (i.e., after removing, the current pointer, must point to the same element before
     * this operation), unless the order to be removed, is the one in current pointer. In this case,
     * we must remove this order, and point current to the next one.
     *
     * @param order
     * @return True if it was removed from the list. False, if another thread removed this order
     * from the list and the order couldn't be find.
     */
    @Override
    public synchronized boolean removeItem(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Attempting to remove a null order.");
        }
        Node nodeToRemove = findNodeToRemove(order);
        if (nodeToRemove == null) {
            return false;
        }
        if (nodeToRemove.getPrevious() != null) {
            nodeToRemove.getPrevious().setNext(nodeToRemove.getNext());
        } else { // removing the head
            this.head = nodeToRemove.getNext();
        }
        if (nodeToRemove.getNext() != null) {
            nodeToRemove.getNext().setPrevious(nodeToRemove.getPrevious());
        } else { // removing the tail
            this.tail = nodeToRemove.getPrevious();
        }
        if (this.current
                == nodeToRemove) { // fix current, if current was pointing to cell just removed
            this.current = nodeToRemove.getNext();
        }
        return true;
    }

    /**
     * @param order - Never null
     */
    protected synchronized Node findNodeToRemove(Order order) {
        Node currentNode = this.head;
        while (currentNode != null) {
            if (order == currentNode.getOrder()) {
                return currentNode;
            }
            currentNode = currentNode.getNext();
        }
        return null;
    }
}
