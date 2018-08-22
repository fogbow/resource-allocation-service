package org.fogbowcloud.manager.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.datastore.orderstorage.RecoveryService;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;


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
        
//        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
//                "token-value", "fake-id", "fake-user");
//    	
//    	Order computeOrder = new ComputeOrder(federationUserToken,
//                "requestingMember", "fake-localidentity-member", 8, 1024,
//                30, "fake_image_name", new UserData("extraUserDataFile",
//                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
//        computeOrder.setOrderStateInTestMode(OrderState.OPEN);
//        
//        try {
//        	databaseManager.add(computeOrder);
//		} catch (Exception e) {
//			System.out.println(e.getMessage());
//		}
//        
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
