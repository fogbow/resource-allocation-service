package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedOrderHolders {
    private static final Logger LOGGER = Logger.getLogger(SharedOrderHolders.class);

    private static SharedOrderHolders instance;

    private Map<String, Order> activeOrdersMap;
    private SynchronizedDoublyLinkedList<Order> openOrders;
    private SynchronizedDoublyLinkedList<Order> spawningOrders;
    private SynchronizedDoublyLinkedList<Order> failedAfterSuccessfulRequestOrders;
    private SynchronizedDoublyLinkedList<Order> failedOnRequestOrders;
    private SynchronizedDoublyLinkedList<Order> fulfilledOrders;
    private SynchronizedDoublyLinkedList<Order> pendingOrders;
    private SynchronizedDoublyLinkedList<Order> closedOrders;

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

    private void addOrdersToMap(SynchronizedDoublyLinkedList<Order> ordersList, Map<String, Order> activeOrdersMap) {
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

    public SynchronizedDoublyLinkedList<Order> getOpenOrdersList() {
        return openOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getSpawningOrdersList() {
        return spawningOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getFailedAfterSuccessfulRequestOrdersList() {
        return failedAfterSuccessfulRequestOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getFailedOnRequestOrdersList() {
        return failedOnRequestOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getFulfilledOrdersList() {
        return fulfilledOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getPendingOrdersList() {
        return pendingOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getClosedOrdersList() {
        return closedOrders;
    }

    public SynchronizedDoublyLinkedList<Order> getOrdersList(OrderState orderState) {
        SynchronizedDoublyLinkedList<Order> list = null;
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
