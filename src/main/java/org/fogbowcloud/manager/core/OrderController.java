package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.apache.log4j.Logger;

public class OrderController {

    private static final Logger LOGGER = Logger.getLogger(OrderController.class);

    private final SharedOrderHolders orderHolders;

    public OrderController() {
        this.orderHolders = SharedOrderHolders.getInstance();
    }

    public List<Order> getAllOrders(FederationUser federationUser, ResourceType resourceType) {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        // Filter all orders of resourceType from federationUser that are not closed (closed orders have been deleted by
        // the user and should not be seen; they will disappear from the system as soon as the closedProcessor thread
        // process them).
        List<Order> requestedOrders =
                orders.stream()
                        .filter(order -> order.getType().equals(resourceType))
                        .filter(order -> order.getFederationUser().equals(federationUser))
                        .filter(order -> !order.getOrderState().equals(OrderState.CLOSED))
                        .collect(Collectors.toList());

        return requestedOrders;
    }

    public Order getOrder(String orderId, FederationUser requester, ResourceType resourceType)
            throws FogbowManagerException {
        Order requestedOrder = this.orderHolders.getActiveOrdersMap().get(orderId);

        if (requestedOrder == null) {
            throw new InstanceNotFoundException();
        }

        if (!requestedOrder.getType().equals(resourceType)) {
            throw new InstanceNotFoundException();
        }

        FederationUser orderOwner = requestedOrder.getFederationUser();
        if (!orderOwner.getId().equals(requester.getId())) {
            throw new UnauthorizedRequestException();
        }

        return requestedOrder;
    }

    public void deleteOrder(Order order) throws InstanceNotFoundException, UnexpectedException {
        if (order == null) {
            String message = "Cannot delete a null order.";
            LOGGER.error(message);
            throw new InstanceNotFoundException();
        }

        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CLOSED)) {
                OrderStateTransitioner.transition(order, OrderState.CLOSED);
           } else {
                String message = "Order [" + order.getId() + "] is already in the closed state";
                LOGGER.error(message);
                throw new InstanceNotFoundException(message);
            }
        }
    }

    public Instance getResourceInstance(Order order) throws Exception {
        LOGGER.info("Get resource instance from order with id <" + order.getId() + "> received");
        synchronized (order) {

            CloudConnector cloudConnector =
                    CloudConnectorFactory.getInstance().getCloudConnector(order.getProvidingMember());
            return cloudConnector.getInstance(order);
        }
    }

    public Allocation getUserAllocation(String memberId, FederationUser federationUser, ResourceType resourceType)
            throws UnexpectedException {

        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> filteredOrders = orders.stream()
                .filter(order -> order.getType().equals(resourceType))
                .filter(order -> order.getOrderState().equals(OrderState.FULFILLED))
                .filter(order -> order.isProviderLocal(memberId))
                .filter(order -> order.getFederationUser().equals(federationUser))
                .collect(Collectors.toList());

        switch (resourceType) {
            case COMPUTE:
                List<ComputeOrder> computeOrders = new ArrayList<>();
                for (Order order : filteredOrders) {
                    computeOrders.add((ComputeOrder) order);
                }
                return getUserComputeAllocation(computeOrders);
            default:
                throw new UnexpectedException("Not yet implemented.");
        }
    }

    private ComputeAllocation getUserComputeAllocation(Collection<ComputeOrder> computeOrders) {
        int vCPU = 0;
        int ram = 0;
        int instances = 0;

        for (ComputeOrder order : computeOrders) {
            ComputeAllocation actualAllocation = order.getActualAllocation();
            vCPU += actualAllocation.getvCPU();
            ram += actualAllocation.getRam();
            instances += actualAllocation.getInstances();
        }

        return new ComputeAllocation(vCPU, ram, instances);
    }
}

