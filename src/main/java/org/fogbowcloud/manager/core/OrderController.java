package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.apache.log4j.Logger;

public class OrderController {

    private static final Logger LOGGER = Logger.getLogger(OrderController.class);

    private final String localMemberId;
    private final SharedOrderHolders orderHolders;

    public OrderController(String localMemberId) {
        //FIXME: is localMemberId really necessary? Maybe it was put here to support future plans. I do not know
        this.localMemberId = localMemberId;
        this.orderHolders = SharedOrderHolders.getInstance();
    }

    public List<Order> getAllOrders(FederationUser federationUser, InstanceType instanceType) throws UnauthorizedRequestException {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> requestedOrders =
                orders.stream()
                        .filter(order -> order.getType().equals(instanceType))
                        .filter(order -> order.getFederationUser().equals(federationUser))
                        .collect(Collectors.toList());

        return requestedOrders;
    }

    public Order getOrder(String orderId, FederationUser federationUser, InstanceType instanceType) {
        Order requestedOrder = this.orderHolders.getActiveOrdersMap().get(orderId);

        if (requestedOrder == null) {
            return null;
        }

        if (!requestedOrder.getType().equals(instanceType)) {
            return null;
        }

        FederationUser orderOwner = requestedOrder.getFederationUser();
        if (!orderOwner.getId().equals(federationUser.getId())) {
            return null;
        }

        return requestedOrder;
    }

    public void deleteOrder(Order order) throws FogbowManagerException {
        if (order == null) {
            String message = "Cannot delete a null order.";
            LOGGER.error(message);
            return;
        }

        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CLOSED)) {
                OrderStateTransitioner.transition(order, OrderState.CLOSED);
           } else {
                String message = "Order [" + order.getId() + "] is already in the closed state";
                LOGGER.error(message);
            }
        }
    }

    public Instance getResourceInstance(Order order) throws FogbowManagerException {
        synchronized (order) {

            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvidingMember());
            return cloudConnector.getInstance(order);
        }
    }

    public Allocation getUserAllocation(String memberId, FederationUser federationUser, InstanceType instanceType) {

        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> filteredOrders = orders.stream()
                .filter(order -> order.getType().equals(instanceType))
                .filter(order -> order.getOrderState().equals(OrderState.FULFILLED))
                .filter(order -> order.isProviderLocal(memberId))
                .filter(order -> order.getFederationUser().equals(federationUser))
                .collect(Collectors.toList());

        switch (instanceType) {
            case COMPUTE:
                List<ComputeOrder> computeOrders = new ArrayList<ComputeOrder>();
                for (Order order : filteredOrders) {
                    computeOrders.add((ComputeOrder) order);
                }
                return (Allocation) getUserComputeAllocation(computeOrders);
            default:
                throw new UnsupportedOperationException("Not yet implemented.");
        }
    }

    private ComputeAllocation getUserComputeAllocation(Collection<ComputeOrder> computeOrders) {

        int vCPU = 0, ram = 0, disk = 0, instances = 0;

        for (ComputeOrder order : computeOrders) {
            ComputeAllocation actualAllocation = order.getActualAllocation();
            vCPU += actualAllocation.getvCPU();
            ram += actualAllocation.getRam();
            instances++;
        }
        return new ComputeAllocation(vCPU, ram, instances);
    }
}

