package org.fogbowcloud.manager.core.instanceprovider;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.remote.xmpp.requesters.RemoteCreateOrderRequest;
import org.fogbowcloud.manager.api.remote.xmpp.requesters.RemoteDeleteOrderRequest;
import org.fogbowcloud.manager.api.remote.xmpp.requesters.RemoteGetOrderRequest;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.jamppa.component.PacketSender;

public class RemoteInstanceProvider implements InstanceProvider {

    private static final Logger LOGGER = Logger.getLogger(RemoteInstanceProvider.class);

    PacketSender packetSender;

    public RemoteInstanceProvider(PacketSender packetSender) {
        this.packetSender = packetSender;
    }

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException, RequestException, RemoteRequestException, OrderManagementException {
        RemoteCreateOrderRequest request = new RemoteCreateOrderRequest(this.packetSender, order);
        // TODO Understand the semantics of send. What are the guarantees after send returns. What are the possible return values (specially in case of failure).
        request.send();
        
        return null;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, RemoteRequestException, OrderManagementException {
    	RemoteDeleteOrderRequest request = new RemoteDeleteOrderRequest(this.packetSender, order);
		request.send();
    }

    @Override
    public Instance getInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, RemoteRequestException {
    	RemoteGetOrderRequest request = new RemoteGetOrderRequest(this.packetSender, order);
    	Instance instance = request.send();
        return instance;
    }
}
