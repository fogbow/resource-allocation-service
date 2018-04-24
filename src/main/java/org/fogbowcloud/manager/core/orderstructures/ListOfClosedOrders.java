package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;

public class ListOfClosedOrders {

    private static ListOfClosedOrders INSTANCE;
    private SynchronizedDoublyLinkedList closedOrders;

    private ListOfClosedOrders() {
        this.closedOrders =  new SynchronizedDoublyLinkedList();
    }

    public static ListOfClosedOrders getInstance() {
        synchronized (ListOfClosedOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new ListOfClosedOrders();
            }
            return INSTANCE;
        }
    }

    public SynchronizedDoublyLinkedList getOrdersList() {
        return this.closedOrders;
    }

}
