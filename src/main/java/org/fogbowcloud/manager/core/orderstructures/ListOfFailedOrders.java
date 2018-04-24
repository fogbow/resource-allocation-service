package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;

public class ListOfFailedOrders {

    private static ListOfFailedOrders INSTANCE;
    private SynchronizedDoublyLinkedList failedOrders;

    private ListOfFailedOrders() {
        this.failedOrders =  new SynchronizedDoublyLinkedList();
    }

    public static ListOfFailedOrders getInstance() {
        synchronized (ListOfFailedOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new ListOfFailedOrders();
            }
            return INSTANCE;
        }
    }

    public SynchronizedDoublyLinkedList getOrdersList() {
        return this.failedOrders;
    }
}
