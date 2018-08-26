package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecoveryService {

    @Autowired
    private OrderRepository orderRepository;

    public RecoveryService() {
    }

    public List<Order> readActiveOrders(OrderState orderState) {

        // If the state is closed, do a filter to not include orders with null instance id
        if (orderState == OrderState.CLOSED) {

            List<Order> filteredOrdersList = new ArrayList<>();

            for (Order order : orderRepository.findByOrderState(orderState)) {
                if (order.getInstanceId() != null) {
                    filteredOrdersList.add(order);
                }
            }

            return filteredOrdersList;
        }

        return orderRepository.findByOrderState(orderState);

    }

    public Order save(Order order) throws UnexpectedException {
        if (orderRepository.exists(order.getId())) {
            throw new UnexpectedException("Order already exists");
        }

        return orderRepository.save(order);
    }

    public Order update(Order order) throws UnexpectedException {
        if (!orderRepository.exists(order.getId())) {
            throw new UnexpectedException("Order doesn't exist");
        }

        return orderRepository.save(order);
    }
}