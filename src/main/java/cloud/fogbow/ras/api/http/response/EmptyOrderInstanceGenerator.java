package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;

public class EmptyOrderInstanceGenerator {

    public static OrderInstance createEmptyInstance(Order order) throws UnexpectedException {
        switch (order.getType()) {
            case COMPUTE:
                return new ComputeInstance(order.getId());
            case VOLUME:
                return new VolumeInstance(order.getId());
            case NETWORK:
                return new NetworkInstance(order.getId());
            case ATTACHMENT:
                return new AttachmentInstance(order.getId());
            case PUBLIC_IP:
                return new PublicIpInstance(order.getId());
            default:
                throw new UnexpectedException(Messages.Exception.UNSUPPORTED_REQUEST_TYPE);
        }
    }

}
