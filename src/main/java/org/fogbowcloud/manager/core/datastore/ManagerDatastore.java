package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.StorageOrder;
import org.fogbowcloud.manager.core.models.token.Token;

import java.util.Collection;
import java.util.List;

public interface ManagerDatastore {

    public Order addOrder(Order order);

    public Iterable<Order> getAllOrders();

    public Order getOrdersByUserId(Long id);

    public Order getOrdersByToken(Token token);

    public List<Order> getOrderByState(OrderState orderState);

    public void updateOrder(Order order);

    public void deleteOrder(Order order);

    public List<Order> findAllComputeOrder();

    public Collection<NetworkOrder> findAllNetworkOrder();

    public Collection<StorageOrder> findAllStorageOrder();
}