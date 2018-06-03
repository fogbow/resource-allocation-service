package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteCreateOrderRequest;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteDeleteOrderRequest;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteGetOrderRequest;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteGetUserQuotaRequest;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
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
            UnauthorizedException, TokenCreationException, RequestException, RemoteRequestException,
            OrderManagementException {
        RemoteCreateOrderRequest remoteCreateOrderRequest = new RemoteCreateOrderRequest(this.packetSender, order);
        remoteCreateOrderRequest.send();
        
        return null;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException,
            PropertyNotSpecifiedException, RemoteRequestException, OrderManagementException {
    	RemoteDeleteOrderRequest remoteDeleteOrderRequest = new RemoteDeleteOrderRequest(this.packetSender, order);
		remoteDeleteOrderRequest.send();
    }

    @Override
    public Instance getInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException,
            PropertyNotSpecifiedException, RemoteRequestException {
    	RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(this.packetSender, order);
    	Instance instance = remoteGetOrderRequest.send();
        return instance;
    }

    @Override
    public ComputeQuota getComputeQuota(String federationMemberId, FederationUser federationUser) throws PropertyNotSpecifiedException,
            QuotaException, UnauthorizedException, TokenCreationException, RemoteRequestException {

        RemoteGetUserQuotaRequest remoteGetQuotaRequest = new RemoteGetUserQuotaRequest(this.packetSender, federationMemberId, federationUser);
        ComputeQuota computeQuota = (ComputeQuota) remoteGetQuotaRequest.send();
        return computeQuota;
    }
}
