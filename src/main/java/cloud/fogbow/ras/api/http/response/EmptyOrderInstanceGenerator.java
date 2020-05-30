package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;

public class EmptyOrderInstanceGenerator {
    public static OrderInstance createEmptyInstance(Order order) throws UnexpectedException {
        OrderInstance instance = null;
        switch (order.getType()) {
            case COMPUTE:
                instance = new ComputeInstance(order.getId());
                break;
            case VOLUME:
                instance = new VolumeInstance(order.getId());
                break;
            case NETWORK:
                instance = new NetworkInstance(order.getId());
                break;
            case ATTACHMENT:
                instance = new AttachmentInstance(order.getId());
                break;
            case PUBLIC_IP:
                instance = new PublicIpInstance(order.getId());
                break;
            default:
                throw new UnexpectedException(Messages.Exception.UNSUPPORTED_REQUEST_TYPE);
        }
        return instance;
    }
}
