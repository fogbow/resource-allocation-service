package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public class OrderToInstanceStateMapper {
    public static InstanceState map(OrderState orderState, InstanceType type)
            throws InstanceNotFoundException, UnexpectedException {
        switch (type) {
            case COMPUTE:
                return mapCompute(orderState);
            case VOLUME:
                return mapNetwork(orderState);
            case NETWORK:
                return mapVolume(orderState);
            case ATTACHMENT:
                return mapAttachment(orderState);
            default:
                String message = "Not supported order type " + type;
                throw new UnexpectedException(message);
        }
    }

    private static InstanceState mapCompute(OrderState orderState)
            throws InstanceNotFoundException, UnexpectedException {
        switch (orderState) {
            case OPEN:
            case PENDING:
                return InstanceState.INACTIVE;
            case SPAWNING:
                return InstanceState.SPAWNING;
            case FULFILLED:
                return InstanceState.READY;
            case FAILED:
                return InstanceState.FAILED;
            case CLOSED:
                throw new InstanceNotFoundException();
            default:
                throw new UnexpectedException("Invalid state " + orderState);
        }
    }

    // TODO: study if we need different states for Network and fix this code accordingly
    private static InstanceState mapNetwork(OrderState orderState)
            throws InstanceNotFoundException, UnexpectedException {
        switch (orderState) {
            case OPEN:
            case PENDING:
                return InstanceState.INACTIVE;
            case SPAWNING:
                return InstanceState.SPAWNING;
            case FULFILLED:
                return InstanceState.READY;
            case FAILED:
                return InstanceState.FAILED;
            case CLOSED:
                throw new InstanceNotFoundException();
            default:
                throw new UnexpectedException("Invalid state " + orderState);
        }
    }

    // TODO: study if we need different states for Volume and fix this code accordingly
    private static InstanceState mapVolume(OrderState orderState)
            throws InstanceNotFoundException, UnexpectedException {
        switch (orderState) {
            case OPEN:
            case PENDING:
                return InstanceState.INACTIVE;
            case SPAWNING:
                return InstanceState.SPAWNING;
            case FULFILLED:
                return InstanceState.READY;
            case FAILED:
                return InstanceState.FAILED;
            case CLOSED:
                throw new InstanceNotFoundException();
            default:
                throw new UnexpectedException("Invalid state " + orderState);
        }
    }

    // TODO: study if we need different states for Attachment and fix this code accordingly
    private static InstanceState mapAttachment(OrderState orderState)
            throws InstanceNotFoundException, UnexpectedException {
        switch (orderState) {
            case OPEN:
            case PENDING:
                return InstanceState.INACTIVE;
            case SPAWNING:
                return InstanceState.SPAWNING;
            case FULFILLED:
                return InstanceState.READY;
            case FAILED:
                return InstanceState.FAILED;
            case CLOSED:
                throw new InstanceNotFoundException();
            default:
                throw new UnexpectedException("Invalid state " + orderState);
        }
    }
}
