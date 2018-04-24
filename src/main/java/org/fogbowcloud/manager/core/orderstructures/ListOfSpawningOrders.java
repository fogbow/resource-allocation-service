package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;

public class ListOfSpawningOrders {

    private static ListOfSpawningOrders INSTANCE;
    private SynchronizedDoublyLinkedList spawningOrders;

    private ListOfSpawningOrders() {
        this.spawningOrders =  new SynchronizedDoublyLinkedList();
    }

    public static ListOfSpawningOrders getInstance() {
        synchronized (ListOfSpawningOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new ListOfSpawningOrders();
            }
            return INSTANCE;
        }
    }

    public SynchronizedDoublyLinkedList getOrdersList() {
        return this.spawningOrders;
    }


}
