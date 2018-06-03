package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteCreateOrderRequest;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteDeleteOrderRequest;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteGetOrderRequest;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.jamppa.component.PacketSender;

public class RemoteCloudConnector implements CloudConnector {

    private static final Logger LOGGER = Logger.getLogger(RemoteCloudConnector.class);

    PacketSender packetSender;

    public RemoteCloudConnector(PacketSender packetSender) {
        this.packetSender = packetSender;
    }

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException, RequestException, RemoteRequestException, OrderManagementException {
        RemoteCreateOrderRequest request = new RemoteCreateOrderRequest(this.packetSender, order);
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
