package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderRepository;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

@Service
public class RecoveryService {
    private static final Logger LOGGER = Logger.getLogger(RecoveryService.class);

    public static final String SIZE_ERROR_MESSAGE_PREFIX = "size must be between";

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

        try {
            this.orderRepository.save(order);
        } catch (TransactionSystemException e) {
            if (isSizeViolation(e)) {

            } else {
                throw new UnexpectedException("", e);
            }
        }
    }

    public void update(Order order) throws UnexpectedException {
        if (!orderRepository.exists(order.getId())) {
            throw new UnexpectedException(Messages.Exception.INEXISTENT_REQUEST);
        }
        this.orderRepository.save(order);
    }

    // TODO move this to a class with generics to hold the type of return and input
    // e.g. StorableObjectTruncator<Order>
    private Order getStorableObject(Order order) {
        // new = constructor.call()
        // for field in fields:
        //   if field has not @Column:
        //     new.field = old.field
        //   else:
        //     fieldMaxSize = field.getMaxSize()
        //     new.field = truncate(old.field, fieldMaxSize)
        // return new;
        return null;
    }

    private boolean isSizeViolation(TransactionSystemException e) {
        Throwable e1 = e.getCause();
        if (e1 != null && e1 instanceof RollbackException) {
            Throwable e2 = e1.getCause();
            if (e2 != null && e2 instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = (ConstraintViolationException) e2;
                Set<ConstraintViolation<?>> constraintViolations = constraintViolationException.getConstraintViolations();
                if (constraintViolations.iterator().hasNext()) {
                    ConstraintViolation<?> constraintViolation = constraintViolations.iterator().next();
                    if (constraintViolation.getMessage().startsWith(SIZE_ERROR_MESSAGE_PREFIX)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}