package cloud.fogbow.ras.core.datastore.services;

import cloud.fogbow.common.datastore.FogbowDatabaseService;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderRepository;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecoveryService extends FogbowDatabaseService<Order> {
    private static final Logger LOGGER = Logger.getLogger(RecoveryService.class);

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> readActiveOrders(OrderState orderState) {
        return orderRepository.findByOrderState(orderState);
    }

    public void save(Order order) throws InternalServerErrorException {
        if (this.orderRepository.exists(order.getId())) {
            throw new InternalServerErrorException(Messages.Exception.REQUEST_ALREADY_EXIST);
        }
        order.serializeSystemUser();
        safeSave(order, this.orderRepository);
    }

    public void update(Order order) throws InternalServerErrorException {
        if (!this.orderRepository.exists(order.getId())) {
            throw new InternalServerErrorException(Messages.Exception.NON_EXISTENT_REQUEST);
        }
        safeSave(order, this.orderRepository);
    }

}