package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderRepository;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecoveryService {
    private static final Logger LOGGER = Logger.getLogger(RecoveryService.class);

    @Autowired
    private OrderRepository orderRepository;

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

}