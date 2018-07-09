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

    public List<Order> getAllOrders(FederationUser federationUser, InstanceType instanceType) {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        // Filter all orders of instanceType from federationUser that are not closed (closed orders have been deleted by
        // the user and should not be seen; they will disappear from the system as soon as the closedProcessor thread
        // process them).
        List<Order> requestedOrders =
                orders.stream()
                        .filter(order -> order.getType().equals(instanceType))
                        .filter(order -> order.getFederationUser().equals(federationUser))
                        .filter(order -> !order.getOrderState().equals(OrderState.CLOSED))
                        .collect(Collectors.toList());

        return requestedOrders;
    }

    public Order getOrder(String orderId, FederationUser federationUser, InstanceType instanceType)
            throws FogbowManagerException {
        Order requestedOrder = this.orderHolders.getActiveOrdersMap().get(orderId);

        if (requestedOrder == null) {
            throw new InstanceNotFoundException();
        }

        if (!requestedOrder.getType().equals(instanceType)) {
            throw new InstanceNotFoundException();
        }

        FederationUser orderOwner = requestedOrder.getFederationUser();
        if (!orderOwner.getId().equals(federationUser.getId())) {
            throw new UnauthorizedRequestException();
        }

        return requestedOrder;
    }

    public void deleteOrder(Order order) throws OrderNotFoundException, UnexpectedException {
        if (order == null) {
            String message = "Cannot delete a null order.";
            LOGGER.error(message);
            throw new OrderNotFoundException();
        }

        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CLOSED)) {
                OrderStateTransitioner.transition(order, OrderState.CLOSED);
           } else {
                String message = "Order [" + order.getId() + "] is already in the closed state";
                LOGGER.error(message);
                throw new OrderNotFoundException(message);
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

    public Allocation getUserAllocation(String memberId, FederationUser federationUser, InstanceType instanceType)
            throws UnexpectedException {

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
                throw new UnexpectedException("Not yet implemented.");
        }
    }

    private ComputeAllocation getUserComputeAllocation(Collection<ComputeOrder> computeOrders) {

        int vCPU = 0, ram = 0, instances = 0;

        for (ComputeOrder order : computeOrders) {
            ComputeAllocation actualAllocation = order.getActualAllocation();
            vCPU += actualAllocation.getvCPU();
            ram += actualAllocation.getRam();
            instances++;
        }
        return new ComputeAllocation(vCPU, ram, instances);
    }
}

