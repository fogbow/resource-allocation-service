package org.fogbowcloud.ras.core.models.linkedlists;

import org.fogbowcloud.ras.core.models.orders.Order;

public class Node {
    private Node next;
    private Node previous;
    private Order order;

    public Node(Node previous, Order order, Node next) {
        this.previous = previous;
        this.order = order;
        this.next = next;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public Node getPrevious() {
        return previous;
    }

    public void setPrevious(Node previous) {
        this.previous = previous;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
