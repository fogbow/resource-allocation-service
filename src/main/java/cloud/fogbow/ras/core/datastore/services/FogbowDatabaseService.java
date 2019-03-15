package cloud.fogbow.ras.core.datastore.services;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.datastore.StorableObjectTruncateHelper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.TransactionSystemException;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

// TODO We might want to move this class to the Common project
public class FogbowDatabaseService<T> {
    public static final String SIZE_ERROR_MESSAGE_PREFIX = "size must be between";

    public <S extends T> void safeSave(S o, JpaRepository<T, ?> repository) throws UnexpectedException {
        try {
            repository.save(o);
        } catch (RuntimeException e) {
            if (isSizeViolation(e)) {
                // transaction needs to be cleared before saving again
                safeFlush(repository);

                StorableObjectTruncateHelper<S> truncateHelper = new StorableObjectTruncateHelper<>(o.getClass());
                S truncated = truncateHelper.truncate(o);

                repository.save(truncated);
            } else {
                throw new UnexpectedException("", e);
            }
        }
    }

    /**
     * Clears the transaction silently.
     *
     * @param repository
     */
    private void safeFlush(JpaRepository repository) {
        try {
            repository.flush();
        } catch (Exception e1) {
        }
    }

    private boolean isSizeViolation(RuntimeException e) {
        if (e instanceof ConstraintViolationException) {
            return isSizeViolation((ConstraintViolationException) e);
        } else if (e instanceof TransactionSystemException) {
            return isSizeViolation((TransactionSystemException) e);
        }
        return false;
    }

    private boolean isSizeViolation(TransactionSystemException e) {
        Throwable e1 = e.getCause();
        if (e1 != null && e1 instanceof RollbackException) {
            Throwable e2 = e1.getCause();
            if (e2 != null && e2 instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = (ConstraintViolationException) e2;
                return isSizeViolation(constraintViolationException);
            }
        }
        return false;
    }

    private boolean isSizeViolation(ConstraintViolationException e) {
        ConstraintViolationException constraintViolationException = e;
        Set<ConstraintViolation<?>> constraintViolations = constraintViolationException.getConstraintViolations();
        if (constraintViolations.iterator().hasNext()) {
            ConstraintViolation<?> constraintViolation = constraintViolations.iterator().next();
            return constraintViolation.getMessage().startsWith(SIZE_ERROR_MESSAGE_PREFIX);
        }
        return false;
    }
}
