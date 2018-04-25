package org.fogbowcloud.manager.core.datastructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSharedStructure {

    private static DataSharedStructure instance;

    private Map<String, Order> activeOrdersMap;
    private SynchronizedDoublyLinkedList openOrders;
    private SynchronizedDoublyLinkedList spawningOrders;
    private SynchronizedDoublyLinkedList failedOrders;
    private SynchronizedDoublyLinkedList fulfilledOrders;
    private SynchronizedDoublyLinkedList pendingOrders;
    private SynchronizedDoublyLinkedList closedOrders;


    private DataSharedStructure() {

        this.activeOrdersMap = new ConcurrentHashMap<String, Order>();
        this.openOrders = new SynchronizedDoublyLinkedList();
        this.spawningOrders = new SynchronizedDoublyLinkedList();
        this.failedOrders = new SynchronizedDoublyLinkedList();
        this.fulfilledOrders = new SynchronizedDoublyLinkedList();
        this.pendingOrders = new SynchronizedDoublyLinkedList();
        this.closedOrders = new SynchronizedDoublyLinkedList();

    }

    public static DataSharedStructure getInstance() {
        synchronized (DataSharedStructure.class) {
            if (instance == null) {
                instance  = new DataSharedStructure();
            }
            return instance ;
        }
    }

    public Map<String, Order> getActiveOrdersMap() {
        return activeOrdersMap;
    }

    public SynchronizedDoublyLinkedList getOpenOrders() {
        return openOrders;
    }

    public SynchronizedDoublyLinkedList getSpawningOrders() {
        return spawningOrders;
    }

    public SynchronizedDoublyLinkedList getFailedOrders() {
        return failedOrders;
    }

    public SynchronizedDoublyLinkedList getFulfilledOrders() {
        return fulfilledOrders;
    }

    public SynchronizedDoublyLinkedList getPendingOrders() {
        return pendingOrders;
    }

    public SynchronizedDoublyLinkedList getClosedOrders() {
        return closedOrders;
    }

}

