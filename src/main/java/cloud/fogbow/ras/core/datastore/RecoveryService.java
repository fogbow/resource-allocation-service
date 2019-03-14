package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderRepository;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

import javax.persistence.Column;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RecoveryService extends FogbowDatabaseService<Order> {
    private static final Logger LOGGER = Logger.getLogger(RecoveryService.class);

    @Autowired
    private OrderRepository orderRepository;

    public RecoveryService() {
    }

    public List<Order> readActiveOrders(OrderState orderState) {
        return orderRepository.findByOrderState(orderState);
    }

    public void save(Order order) throws UnexpectedException {
        if (this.orderRepository.exists(order.getId())) {
            throw new UnexpectedException(Messages.Exception.REQUEST_ALREADY_EXIST);
        }
        safeSave(order, this.orderRepository);
    }

    public void update(Order order) throws UnexpectedException {
        if (!this.orderRepository.exists(order.getId())) {
            throw new UnexpectedException(Messages.Exception.INEXISTENT_REQUEST);
        }
        safeSave(order, this.orderRepository);
    }

}