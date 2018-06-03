package org.fogbowcloud.manager.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);
    private final String localMemberId;
    private SharedOrderHolders orderHolders;

    public OrderController(String localMemberId) {
        this.localMemberId = localMemberId;
        this.orderHolders = SharedOrderHolders.getInstance();
    }

    public void activateOrder(Order order)
            throws OrderManagementException {
        if (order == null) {
            String message = "Can't process new order request. Order reference is null.";
            throw new OrderManagementException(message);
        }

        addOrderInActiveOrdersMap(order);
    }

    public List<Order> getAllOrders(FederationUser federationUser, InstanceType instanceType) throws UnauthorizedException {
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
        if (!orderOwner.equals(federationUser)) {
            return null;
        }

        return requestedOrder;
    }

    public void deleteOrder(Order order) throws OrderManagementException {
    	if (order == null) {
    		String message = "Cannot delete a null order";
    		throw new OrderManagementException(message);
    	}
        synchronized (order) {

            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CLOSED)) {
                try {
                    OrderStateTransitioner.transition(order, OrderState.CLOSED);
                } catch (OrderStateTransitionException e) {
                    LOGGER.error(
                        "This should never happen. Error trying to change the status from"
                            + order.getOrderState()
                            + " to closed for order ["
                            + order.getId()
                            + "]",
                        e);
                }
            } else {
                String message = "Order [" + order.getId() + "] is already in the closed state";
                throw new OrderManagementException(message);
            }
        }
    }

    public void removeOrderFromActiveOrdersMap(Order order) {
        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();
        synchronized (order) {
            if (activeOrdersMap.containsKey(order.getId())) {
                activeOrdersMap.remove(order.getId());
            } else {
                String message =
                    "Tried to remove order %s from the active orders but it was not active";
                LOGGER.error(String.format(message, order.getId()));
            }
        }
    }

    public Instance getResourceInstance(Order order)
        throws PropertyNotSpecifiedException, TokenCreationException, RequestException, UnauthorizedException,
            InstanceNotFoundException, RemoteRequestException {
        synchronized (order) {

            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvidingMember());
            return cloudConnector.getInstance(order);
        }
    }

    public Allocation getUserAllocation(String memberId, FederationUser federationUser, InstanceType instanceType)
            throws UnauthorizedException, InstanceNotFoundException, QuotaException, PropertyNotSpecifiedException,
            RemoteRequestException, TokenCreationException, RequestException {

        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> computeOrders = orders.stream()
                .filter(order -> order.getType().equals(instanceType))
                .filter(order -> order.getOrderState().equals(OrderState.FULFILLED))
                .filter(order -> order.isProviderLocal(memberId))
                .filter(order -> order.getFederationUser().equals(federationUser))
                .collect(Collectors.toList());

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserAllocation(orders, instanceType);
    }

    private void addOrderInActiveOrdersMap(Order order) throws OrderManagementException {
        String orderId = order.getId();
        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();
        SynchronizedDoublyLinkedList openOrdersList = this.orderHolders.getOpenOrdersList();

        synchronized (order) {
            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format("Order with id %s is already in active orders map.", orderId);
                throw new OrderManagementException(message);
            }

            if (order.getRequestingMember() == null) {
                order.setRequestingMember(this.localMemberId);
            }

            if (order.getProvidingMember() == null) {
                order.setProvidingMember(this.localMemberId);
            }

            order.setOrderState(OrderState.OPEN);
            activeOrdersMap.put(orderId, order);
            openOrdersList.addItem(order);
        }
    }

}
