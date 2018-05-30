package org.fogbowcloud.manager.core.instanceprovider;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.remote.xmpp.requesters.CreateRemoteCompute;
import org.fogbowcloud.manager.api.remote.xmpp.requesters.RemoteRequest;
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
        switch (order.getType()) {
            case COMPUTE:
                CreateRemoteCompute request = new CreateRemoteCompute(this.packetSender, order);
                request.send();
                break;
            case VOLUME:
                break;
            case NETWORK:
                break;
            case ATTACHMENT:
                break;
            default:
                throw new IllegalArgumentException("Not implemented");
        }
        return null;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException {

    }

    @Override
    public Instance getInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException {
        return null;
    }
}
