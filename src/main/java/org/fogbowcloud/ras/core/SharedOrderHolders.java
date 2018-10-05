package org.fogbowcloud.ras.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedOrderHolders {
    private static final Logger LOGGER = Logger.getLogger(SharedOrderHolders.class);

    private static SharedOrderHolders instance;
    private Map<String, Order> activeOrdersMap;
    private SynchronizedDoublyLinkedList openOrders;
    private SynchronizedDoublyLinkedList spawningOrders;
    private SynchronizedDoublyLinkedList failedAfterSuccessfulRequestOrders;
    private SynchronizedDoublyLinkedList failedOnRequestOrders;
    private SynchronizedDoublyLinkedList fulfilledOrders;
    private SynchronizedDoublyLinkedList pendingOrders;
    private SynchronizedDoublyLinkedList closedOrders;

    public SharedOrderHolders() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        this.activeOrdersMap = new ConcurrentHashMap<>();

        try {
            this.openOrders = databaseManager.readActiveOrders(OrderState.OPEN);
            addOrdersToMap(this.openOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.OPEN, this.activeOrdersMap.size()));
            this.spawningOrders = databaseManager.readActiveOrders(OrderState.SPAWNING);
            addOrdersToMap(this.spawningOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.SPAWNING, this.activeOrdersMap.size()));
            this.failedAfterSuccessfulRequestOrders = databaseManager.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
            addOrdersToMap(this.failedAfterSuccessfulRequestOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST, this.activeOrdersMap.size()));
            this.failedOnRequestOrders = databaseManager.readActiveOrders(OrderState.FAILED_ON_REQUEST);
            addOrdersToMap(this.failedOnRequestOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.FAILED_ON_REQUEST, this.activeOrdersMap.size()));
            this.fulfilledOrders = databaseManager.readActiveOrders(OrderState.FULFILLED);
            addOrdersToMap(this.fulfilledOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.FULFILLED, this.activeOrdersMap.size()));
            this.pendingOrders = databaseManager.readActiveOrders(OrderState.PENDING);
            addOrdersToMap(this.pendingOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.PENDING, this.activeOrdersMap.size()));
            this.closedOrders = databaseManager.readActiveOrders(OrderState.CLOSED);
            addOrdersToMap(this.closedOrders, this.activeOrdersMap);
            LOGGER.info(String.format(Messages.Info.RECOVERING_LIST_OF_ORDERS, OrderState.CLOSED, this.activeOrdersMap.size()));
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

    public SynchronizedDoublyLinkedList getFailedAfterSuccessfulRequestOrdersList() {
        return failedAfterSuccessfulRequestOrders;
    }

    public SynchronizedDoublyLinkedList getFailedOnRequestOrdersList() {
        return failedOnRequestOrders;
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
            case FAILED_AFTER_SUCCESSUL_REQUEST:
                list = SharedOrderHolders.getInstance().getFailedAfterSuccessfulRequestOrdersList();
                break;
            case FAILED_ON_REQUEST:
                list = SharedOrderHolders.getInstance().getFailedOnRequestOrdersList();
                break;
            default:
                break;
        }
        return list;
    }
}
