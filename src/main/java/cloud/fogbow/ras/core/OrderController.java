package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.*;
import cloud.fogbow.ras.api.http.response.quotas.allocation.Allocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class OrderController {
    private static final Logger LOGGER = Logger.getLogger(OrderController.class);

    private final SharedOrderHolders orderHolders;

    public OrderController() {
        this.orderHolders = SharedOrderHolders.getInstance();
    }

    public Order getOrder(String orderId) throws InstanceNotFoundException {
        Order requestedOrder = this.orderHolders.getActiveOrdersMap().get(orderId);
        if (requestedOrder == null) {
            throw new InstanceNotFoundException();
        }
        return requestedOrder;
    }

    public void deleteOrder(String orderId) throws FogbowException {
        if (orderId == null) 
        	throw new InvalidParameterException(Messages.Exception.INSTANCE_ID_NOT_INFORMED);
        
        Order order = getOrder(orderId);
        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CLOSED)) {
                CloudConnector cloudConnector = getCloudConnector(order);
                try {
                    cloudConnector.deleteInstance(order);
                } catch (InstanceNotFoundException e) {
                    LOGGER.info(String.format(Messages.Info.DELETING_ORDER_INSTANCE_NOT_FOUND, orderId), e);
                }
                OrderStateTransitioner.transition(order, OrderState.CLOSED);
            } else {
                String message = String.format(Messages.Error.REQUEST_ALREADY_CLOSED, order.getId());
                LOGGER.error(message);
                throw new InstanceNotFoundException(message);
            }
        }
    }

    public Instance getResourceInstance(String orderId) throws FogbowException, UnexpectedException {
        if (orderId == null) 
        	throw new InvalidParameterException(Messages.Exception.INSTANCE_ID_NOT_INFORMED);
        
        Order order = getOrder(orderId);
        synchronized (order) {
            CloudConnector cloudConnector = getCloudConnector(order);
            Instance instance = cloudConnector.getInstance(order);
            order.setCachedInstanceState(instance.getState());
            return instance;
        }
    }

    public Allocation getUserAllocation(String memberId, SystemUser systemUser, ResourceType resourceType)
            throws UnexpectedException {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> filteredOrders = orders.stream()
                .filter(order -> order.getType().equals(resourceType))
                .filter(order -> order.getOrderState().equals(OrderState.FULFILLED))
                .filter(order -> order.isProviderLocal(memberId))
                .filter(order -> order.getSystemUser().equals(systemUser))
                .collect(Collectors.toList());

        switch (resourceType) {
            case COMPUTE:
                List<ComputeOrder> computeOrders = new ArrayList<>();
                for (Order order : filteredOrders) {
                    computeOrders.add((ComputeOrder) order);
                }
                return getUserComputeAllocation(computeOrders);
            default:
                throw new UnexpectedException(Messages.Exception.RESOURCE_TYPE_NOT_IMPLEMENTED);
        }
    }

	public List<InstanceStatus> getInstancesStatus(SystemUser systemUser, ResourceType resourceType) {
		List<InstanceStatus> instanceStatusList = new ArrayList<>();
		List<Order> allOrders = getAllOrders(systemUser, resourceType);

		for (Order order : allOrders) {
			String name = null;

			switch (resourceType) {
			case COMPUTE:
				name = ((ComputeOrder) order).getName();
				break;
			case VOLUME:
				name = ((VolumeOrder) order).getName();
				break;
			case NETWORK:
				name = ((NetworkOrder) order).getName();
				break;
			default:
				break;
			}

			// The state of the instance can be inferred from the state of the order
			InstanceStatus instanceStatus = new InstanceStatus(
					order.getId(), 
					name, 
					order.getProvider(),
					order.getCloudName(), 
					order.getCachedInstanceState());
			
			instanceStatusList.add(instanceStatus);
		}

		return instanceStatusList;
	}

    protected CloudConnector getCloudConnector(Order order) {
        CloudConnector provider = null;
        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        
        if (order.isProviderLocal(localMemberId)) {
            provider = CloudConnectorFactory.getInstance().getCloudConnector(localMemberId, order.getCloudName());
        } else {
            if (order.getOrderState().equals(OrderState.OPEN) ||
                    order.getOrderState().equals(OrderState.FAILED_ON_REQUEST)) {
            
            	// This is an order for a remote provider that has never been received by that provider.
            	// Thus, there is no need to send a delete message via a RemoteCloudConnector, and it is 
            	// only necessary to call deleteInstance in the local member.
                provider = CloudConnectorFactory.getInstance().getCloudConnector(localMemberId, order.getCloudName());
            
            } else {
                provider = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(), order.getCloudName());
            }
        }
        return provider;
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

	private List<Order> getAllOrders(SystemUser systemUser, ResourceType resourceType) {
		Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

		// Filter all orders of resourceType from the user systemUser that are not
		// closed (closed orders have been deleted by the user and should not be seen;
		// they will disappear from the system as soon as the closedProcessor thread
		// process them).
		List<Order> requestedOrders = orders.stream()
				.filter(order -> order.getType().equals(resourceType))
				.filter(order -> order.getSystemUser().equals(systemUser))
				.filter(order -> !order.getOrderState().equals(OrderState.CLOSED)).collect(Collectors.toList());

		return requestedOrders;
	}
}

