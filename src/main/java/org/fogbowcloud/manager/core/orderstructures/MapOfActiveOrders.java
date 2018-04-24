package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.orders.Order;

import java.util.HashMap;
import java.util.Map;

public class MapOfActiveOrders {

    private static MapOfActiveOrders INSTANCE;
    private Map<String, Order> activeOrdersMap;

    private MapOfActiveOrders() {
        this.activeOrdersMap = new HashMap<String, Order>();
    }

    public static MapOfActiveOrders getInstance() {
        synchronized (MapOfActiveOrders.class) {
            if (INSTANCE == null) {
                INSTANCE = new MapOfActiveOrders();
            }
            return INSTANCE;
        }
    }

    public void addOrder(Order order) {
        this.activeOrdersMap.put( order.getId(), order);
    }

    public synchronized void deleteOrder(String orderId) {
        this.activeOrdersMap.remove(orderId);
    }

    public Order getOrder(String orderId) {

        Order order = null;

        if (this.activeOrdersMap.containsKey(orderId)) {
            order = this.activeOrdersMap.get(orderId);
        } else {
            // TODO: log or throws exception
        }

        return order;
    }

}
