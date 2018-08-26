package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedOrderHolders {
    private static SharedOrderHolders instance;
    private Map<String, Order> activeOrdersMap;
    private SynchronizedDoublyLinkedList openOrders;
    private SynchronizedDoublyLinkedList spawningOrders;
    private SynchronizedDoublyLinkedList failedOrders;
    private SynchronizedDoublyLinkedList fulfilledOrders;
    private SynchronizedDoublyLinkedList pendingOrders;
    private SynchronizedDoublyLinkedList closedOrders;

    public SharedOrderHolders() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        this.activeOrdersMap = new ConcurrentHashMap<>();

        try {
            this.openOrders = databaseManager.readActiveOrders(OrderState.OPEN);
            addOrdersToMap(this.openOrders, this.activeOrdersMap);
            this.spawningOrders = databaseManager.readActiveOrders(OrderState.SPAWNING);
            addOrdersToMap(this.spawningOrders, this.activeOrdersMap);
            this.failedOrders = databaseManager.readActiveOrders(OrderState.FAILED);
            addOrdersToMap(this.failedOrders, this.activeOrdersMap);
            this.fulfilledOrders = databaseManager.readActiveOrders(OrderState.FULFILLED);
            addOrdersToMap(this.fulfilledOrders, this.activeOrdersMap);
            this.pendingOrders = databaseManager.readActiveOrders(OrderState.PENDING);
            addOrdersToMap(this.pendingOrders, this.activeOrdersMap);
            this.closedOrders = databaseManager.readActiveOrders(OrderState.CLOSED);
            addOrdersToMap(this.closedOrders, this.activeOrdersMap);
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage());
        }
    }

    private void addOrdersToMap(SynchronizedDoublyLinkedList ordersList, Map<String, Order> activeOrdersMap) {
        Order order;

        while ((order = ordersList.getNext()) != null) {
            activeOrdersMap.put(order.getId(), order);
        }
        ordersList.resetPointer();
    }

    public static SharedOrderHolders getInstance() {
        synchronized (SharedOrderHolders.class) {
            if (instance == null) {
                instance = new SharedOrderHolders();
            }
            return instance;
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

    public SynchronizedDoublyLinkedList getOrdersList(OrderState orderState) {
        SynchronizedDoublyLinkedList list = null;
        switch (orderState) {
            case OPEN:
                list = SharedOrderHolders.getInstance().getOpenOrdersList();
                break;
            case SPAWNING:
                list = SharedOrderHolders.getInstance().getSpawningOrdersList();
                break;
            case PENDING:
                list = SharedOrderHolders.getInstance().getPendingOrdersList();
                break;
            case FULFILLED:
                list = SharedOrderHolders.getInstance().getFulfilledOrdersList();
                break;
            case CLOSED:
                list = SharedOrderHolders.getInstance().getClosedOrdersList();
                break;
            case FAILED:
                list = SharedOrderHolders.getInstance().getFailedOrdersList();
                break;
            default:
                break;
        }
        return list;
    }
}
