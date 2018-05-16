package org.fogbowcloud.manager.core.datastore.service;

import org.fogbowcloud.manager.core.datastore.repository.OrderRepository;
import org.fogbowcloud.manager.core.datastore.repository.OrderTransitionRepository;
import org.fogbowcloud.manager.core.models.OrderTransition;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTransitionRepository transitionRepository;

    public Order save(Order order){
        order.setCreationTime(new Date().getTime());
        return this.orderRepository.save(order);
    }

    public Order update(Order order, OrderState targetState){
        OrderTransition transition = new OrderTransition(order, order.getOrderState(), targetState, new Date().getTime());
        transitionRepository.save(transition);
        return this.orderRepository.save(order);
    }

}
