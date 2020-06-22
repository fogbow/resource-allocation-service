package cloud.fogbow.ras.core.plugins.authorization;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;

public class DefaultAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

    @Override
    public boolean isAuthorized(SystemUser requester, RasOperation operation) throws UnauthorizedRequestException {
        Order order = operation.getOrder();
        ResourceType type = operation.getResourceType();
        if (order != null) {
            // Check whether requester owns order
            SystemUser orderOwner = order.getSystemUser();
            if (!orderOwner.equals(requester)) {
                throw new UnauthorizedRequestException(Messages.Exception.REQUESTER_DOES_NOT_OWN_REQUEST);
            }
            // Check if requested type matches order type
            ResourceType orderType = order.getType();
            switch (orderType) {
                case NETWORK:
                    if (!type.equals(ResourceType.NETWORK) && !type.equals(ResourceType.SECURITY_RULE))
                        throw new UnauthorizedRequestException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
                    break;
                case PUBLIC_IP:
                    if (!type.equals(ResourceType.PUBLIC_IP) && !type.equals(ResourceType.SECURITY_RULE))
                        throw new UnauthorizedRequestException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
                    break;
                default:
                    if (!order.getType().equals(type))
                        throw new UnauthorizedRequestException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
                    break;
            }
        }
        return true;
    }
}
