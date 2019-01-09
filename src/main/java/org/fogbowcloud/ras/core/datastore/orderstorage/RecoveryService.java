package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class RecoveryService {
    private static final Logger LOGGER = Logger.getLogger(RecoveryService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStateChangeRepository orderTimestampRepository;

    public RecoveryService() {
    }

    public List<Order> readActiveOrders(OrderState orderState) {
        return orderRepository.findByOrderState(orderState);
    }

    public void save(Order order) throws UnexpectedException {
        if (orderRepository.exists(order.getId())) {
            throw new UnexpectedException(Messages.Exception.REQUEST_ALREADY_EXIST);
        }
        this.orderRepository.save(order);
    }

    public void update(Order order) throws UnexpectedException {
        if (!orderRepository.exists(order.getId())) {
            throw new UnexpectedException(Messages.Exception.INEXISTENT_REQUEST);
        }
        this.orderRepository.save(order);
    }

    public void updateStateTimestamp(Order order) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        OrderStateChange orderStateChange = new OrderStateChange(currentTimestamp, order, order.getOrderState());
        this.orderTimestampRepository.save(orderStateChange);
    }
}