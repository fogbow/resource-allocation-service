package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.*;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.jamppa.component.PacketSender;

import java.util.Collection;

public class RemoteCloudConnector implements CloudConnector {

    private static final Logger LOGGER = Logger.getLogger(RemoteCloudConnector.class);

    private String destinationMember;

    public RemoteCloudConnector(String memberId) {
        this.destinationMember = memberId;
    }

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException, RequestException, RemoteRequestException,
            OrderManagementException {
        RemoteCreateOrderRequest remoteCreateOrderRequest = new RemoteCreateOrderRequest(order);
        remoteCreateOrderRequest.send();
        
        return null;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException,
            PropertyNotSpecifiedException, RemoteRequestException, OrderManagementException {
        RemoteDeleteOrderRequest remoteDeleteOrderRequest = new RemoteDeleteOrderRequest(order);
		remoteDeleteOrderRequest.send();
    }

    @Override
    public Instance getInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException,
            PropertyNotSpecifiedException, RemoteRequestException {
        RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
        Instance instance = remoteGetOrderRequest.send();
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws
            PropertyNotSpecifiedException,  QuotaException, UnauthorizedException, TokenCreationException,
            RemoteRequestException {

        RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationMember, federationUser, instanceType);
        Quota quota = remoteGetUserQuotaRequest.send();
        return quota;
    }

    @Override
    public Allocation getUserAllocation(Collection<Order> orders, InstanceType instanceType)
            throws RemoteRequestException, InstanceNotFoundException, RequestException, QuotaException,
            TokenCreationException, PropertyNotSpecifiedException, UnauthorizedException {

        switch (instanceType) {
            case COMPUTE:
                return (Allocation) getUserComputeAllocation(orders);
            default:
                throw new UnsupportedOperationException("Not yet implemented.");
        }
    }

    private ComputeAllocation getUserComputeAllocation(Collection<Order> computeOrders) throws
            QuotaException, RemoteRequestException, RequestException, TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException {

        int vCPU = 0, ram = 0, instances = 0;

        for (Order order : computeOrders) {
            ComputeInstance computeInstance = (ComputeInstance) this.getInstance(order);
            vCPU += computeInstance.getvCPU();
            ram += computeInstance.getMemory();
            instances++;
        }

        return new ComputeAllocation(vCPU, ram, instances);
    }
}
