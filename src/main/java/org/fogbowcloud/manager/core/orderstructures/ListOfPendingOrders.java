package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;

public class ListOfPendingOrders {

    private static ListOfPendingOrders INSTANCE;
    private SynchronizedDoublyLinkedList pendingOrders;

    private ListOfPendingOrders() {
        this.pendingOrders =  new SynchronizedDoublyLinkedList();
    }

    public static ListOfPendingOrders getInstance() {
        synchronized (ListOfPendingOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new ListOfPendingOrders();
            }
            return INSTANCE;
        }
    }

    public SynchronizedDoublyLinkedList getOrdersList() {
        return this.pendingOrders;
    }

}
