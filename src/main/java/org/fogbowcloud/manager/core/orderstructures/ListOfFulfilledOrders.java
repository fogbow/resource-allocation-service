package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;

public class ListOfFulfilledOrders {

    private static ListOfFulfilledOrders INSTANCE;
    private SynchronizedDoublyLinkedList fulfilledOrders;

    private ListOfFulfilledOrders() {
        this.fulfilledOrders = new SynchronizedDoublyLinkedList();
    }

    public static ListOfFulfilledOrders getInstance() {
        synchronized (ListOfFulfilledOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new ListOfFulfilledOrders();
            }
            return INSTANCE;
        }
    }

    public SynchronizedDoublyLinkedList getOrdersList() {
        return this.fulfilledOrders;
    }
}
