package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;

public class ListOfOpenOrders {

    private static ListOfOpenOrders INSTANCE;
    private SynchronizedDoublyLinkedList openOrders;

    private ListOfOpenOrders() {
        this.openOrders = new SynchronizedDoublyLinkedList();
    }

    public static ListOfOpenOrders getInstance() {
        synchronized (ListOfOpenOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new ListOfOpenOrders();
            }
            return INSTANCE;
        }
    }

    public SynchronizedDoublyLinkedList getOrdersList() {
        return this.openOrders;
    }

}
