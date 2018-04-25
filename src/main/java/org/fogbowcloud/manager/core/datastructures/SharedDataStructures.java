package org.fogbowcloud.manager.core.datastructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedDataStructures {

    private static SharedDataStructures instance;

    private Map<String, Order> activeOrdersMap;
    private SynchronizedDoublyLinkedList openOrders;
    private SynchronizedDoublyLinkedList spawningOrders;
    private SynchronizedDoublyLinkedList failedOrders;
    private SynchronizedDoublyLinkedList fulfilledOrders;
    private SynchronizedDoublyLinkedList pendingOrders;
    private SynchronizedDoublyLinkedList closedOrders;


    private SharedDataStructures() {

        this.activeOrdersMap = new ConcurrentHashMap<String, Order>();
        this.openOrders = new SynchronizedDoublyLinkedList();
        this.spawningOrders = new SynchronizedDoublyLinkedList();
        this.failedOrders = new SynchronizedDoublyLinkedList();
        this.fulfilledOrders = new SynchronizedDoublyLinkedList();
        this.pendingOrders = new SynchronizedDoublyLinkedList();
        this.closedOrders = new SynchronizedDoublyLinkedList();

    }

    public static SharedDataStructures getInstance() {
        synchronized (SharedDataStructures.class) {
            if (instance == null) {
                instance  = new SharedDataStructures();
            }
            return instance ;
        }
    }

    public Map<String, Order> getActiveOrdersMap() {
        return activeOrdersMap;
    }

    public SynchronizedDoublyLinkedList getOpenOrdersList() {
        return openOrders;
    }

    public SynchronizedDoublyLinkedList getSpawningOrdersList() {
        return spawningOrders;
    }

    public SynchronizedDoublyLinkedList getFailedOrdersList() {
        return failedOrders;
    }

    public SynchronizedDoublyLinkedList getFulfilledOrdersList() {
        return fulfilledOrders;
    }

    public SynchronizedDoublyLinkedList getPendingOrdersList() {
        return pendingOrders;
    }

    public SynchronizedDoublyLinkedList getClosedOrdersList() {
        return closedOrders;
    }

}

