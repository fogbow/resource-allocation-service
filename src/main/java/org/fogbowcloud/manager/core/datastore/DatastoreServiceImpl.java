package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.datastore.repository.OrderRepository;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.token.Token;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class DatastoreServiceImpl implements ManagerDatastore {

    private OrderRepository orderRepository;
    private String TYPE_COMPUTE_TERM = "compute";

    public DatastoreServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order addOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Iterable<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrdersByUserId(Long id) {
        return orderRepository.findOne(id);
    }

    @Override
    public Order getOrdersByToken(Token token) {
        return null;
    }

    @Override
    public List<Order> getOrderByState(OrderState orderState) {
        return orderRepository.findByOrderState(orderState.name());
    }

    @Override
    public void updateOrder(Order order) {

    }

    @Override
    public void deleteOrder(Order order) {

    }

    @Override
    public List<Order> findAllComputeOrder() {
        return orderRepository.findByType(TYPE_COMPUTE_TERM);
    }

    @Override
    public Collection<NetworkOrder> findAllNetworkOrder() {
        return null;
    }

    @Override
    public Collection<StorageOrder> findAllStorageOrder() {
        return null;
    }

}
