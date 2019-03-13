package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.models.orders.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.TransactionSystemException;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

public class FogbowDatabaseService {
    public static final String SIZE_ERROR_MESSAGE_PREFIX = "size must be between";

    public void safeSave(Order o, JpaRepository repository) throws UnexpectedException {
        try {
            repository.save(o);
        } catch (TransactionSystemException e) {
            if (isSizeViolation(e)) {
                StorableObjectTruncateHelper<Order> truncateHelper = new StorableObjectTruncateHelper<>(o.getClass());
                Order truncated = truncateHelper.truncate(o);
                repository.save(truncated);
            } else {
                throw new UnexpectedException("", e);
            }
        }
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
