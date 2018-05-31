package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.remote.xmpp.Event;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;

public class RemoteNotifyEventRequest implements RemoteRequest {

    private Order order;
    private Event event;

    public RemoteNotifyEventRequest(Order order, Event event) {
        this.order = order;
        this.event = event;
    }

    @Override
    public Object send() throws RemoteRequestException, OrderManagementException, UnauthorizedException {
        // TODO: implement XMPP messaging.
        return null;
    }
}
