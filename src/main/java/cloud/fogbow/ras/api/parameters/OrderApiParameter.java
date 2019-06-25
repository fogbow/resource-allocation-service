package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.models.orders.Order;

public abstract class OrderApiParameter<T extends Order> {

    public final T getOrder() throws FogbowException {
        this.checkConsistency();
        return createOrder();
    }

    public abstract T createOrder() throws FogbowException;

    public abstract void checkConsistency() throws FogbowException;
}
