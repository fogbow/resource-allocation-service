package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public class DatabaseManager implements StableStorage {

    private static DatabaseManager instance;

    private DatabaseManager() {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
//        propertiesHolder.getProperty();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    @Override
    public void add(Order order) {

    }

    @Override
    public void update(Order order) {

    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) {
        if (orderState.equals(OrderState.CLOSED)) {
            // returns only orders with instanceId different than null
        }

        return new SynchronizedDoublyLinkedList();
    }
}
