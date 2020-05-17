package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.*;
import cloud.fogbow.ras.api.http.response.quotas.allocation.*;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.*;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderController {
    private static final Logger LOGGER = Logger.getLogger(OrderController.class);

    private final SharedOrderHolders orderHolders;
    private Map<String, List<String>> orderDependencies;
    private String localProviderId;

    public OrderController() {
        this.orderHolders = SharedOrderHolders.getInstance();
        this.orderDependencies = new ConcurrentHashMap<>();
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
    }

    public Order getOrder(String orderId) throws InstanceNotFoundException {
        Order requestedOrder = this.orderHolders.getActiveOrdersMap().get(orderId);
        if (requestedOrder == null) {
            throw new InstanceNotFoundException(String.format(Messages.Exception.NOT_FOUND_ORDER_ID_S, orderId));
        }
        return requestedOrder;
    }

    public String activateOrder(Order order) throws FogbowException {
        LOGGER.info(Messages.Info.ACTIVATING_NEW_REQUEST);

        if (order == null) {
            throw new UnexpectedException(Messages.Exception.UNABLE_TO_PROCESS_EMPTY_REQUEST);
        }

        // One might think that the synchronized block is not really needed, since the order should not exist
        // until now, and no other thread could be manipulating it at this time; although highly unlikely, it
        // is possible that ids randomly assigned clash, or that a malicious user submit requests with random
        // generated ids that may clash with the id assigned to this order. The cost of being extra safe is low.
        synchronized (order) {
            SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
            Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
            ChainedList<Order> openOrdersList = sharedOrderHolders.getOpenOrdersList();

            String orderId = order.getId();
            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format(Messages.Exception.REQUEST_ID_ALREADY_ACTIVATED, orderId);
                throw new UnexpectedException(message);
            }

            order.setOrderState(OrderState.OPEN);
            activeOrdersMap.put(orderId, order);
            openOrdersList.addItem(order);
            // Sometimes an order depends on other orders (ex. an attachment depends on a volume and a compute).
            // We need to keep this information, so to disallow the deletion of an order on which another order
            // depends (ex. we should not allow the deletion of a volume, for which there is an active attachment),
            // but the information needs only to be kept at the provider that received the create request through its
            // REST API.
            if (order.isRequesterLocal(this.localProviderId)) {
                this.updateOrderDependencies(order, Operation.CREATE);
            }
            return order.getId();
        }
    }

    public void deactivateOrder(Order order) throws UnexpectedException {
        synchronized (order) {
            SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
            Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
            ChainedList<Order> checkingDeletionOrders = sharedOrderHolders.getCheckingDeletionOrdersList();

            if (activeOrdersMap.containsKey(order.getId())) {
                activeOrdersMap.remove(order.getId());
            } else {
                String message = String.format(Messages.Exception.UNABLE_TO_REMOVE_INACTIVE_REQUEST, order.getId());
                throw new UnexpectedException(message);
            }
            checkingDeletionOrders.removeItem(order);
            order.setInstanceId(null);
            order.setOrderState(OrderState.CLOSED);
            if (order.isRequesterRemote(this.localProviderId)) {
                try {
                    LOGGER.info("Notifying remote requester that the order is CLOSED");
                    OrderStateTransitioner.notifyRequester(order, OrderState.CLOSED);
                } catch (Exception e) {
                    // ToDO: Add orders that failed to be notified to a list of orders missing notification.
                    //  A new thread (MissedNotificationProcessor) will periodically retry these notifications.
                    // This list does not need to be in stable storage. Upon recovery (see the constructor of
                    // SharedOrdersHolder), all active orders (those not deactivated) whose requesters are remote
                    // should be added in the list of orders missing notification and be notified again (just in case).
                    // ClosedProcessor should inspect this list before deactivating an order whose requester is
                    // remote. Only orders that are not present in the list should be deactivated. Those that are
                    // present in the list should be kept in the CLOSED state, until they are successfully notified.
                    // Eventual garbage that remains due to a remote requester that never recovers (and cannot be
                    // notified) will be dealt with by the admin tool to be developed.
                    String message = String.format(Messages.Warn.UNABLE_TO_NOTIFY_REQUESTING_PROVIDER, order.getRequester(), order.getId());
                    LOGGER.warn(message, e);
                }
            }
        }
    }

    public void deleteOrder(Order order) throws FogbowException {
        if (order == null)
            throw new UnexpectedException(Messages.Exception.CORRUPTED_INSTANCE);

        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (orderState.equals(OrderState.CHECKING_DELETION) ||
                    order.getOrderState().equals(OrderState.ASSIGNED_FOR_DELETION)) {
                String message = String.format(Messages.Error.DELETE_OPERATION_ALREADY_ONGOING, order.getId());
                throw new OnGoingOperationException(message);
            }
            if (order.isRequesterLocal(this.localProviderId) && hasOrderDependencies(order.getId())) {
                    throw new DependencyDetectedException(String.format(Messages.Exception.DEPENDENCY_DETECTED,
                            order.getId(), this.orderDependencies.get(order.getId())));
            }
            if (order.getOrderState().equals(OrderState.SELECTED)) {
                // Orders in state SELECTED cannot be deleted. This state is used to implement an
                // "at-most-once" semantic for the requestInstance() call. See OpenProcessor.
                throw new OnGoingOperationException(cloud.fogbow.common.constants.Messages.Exception.CANNOT_DELETE_INSTANCE_WHILE_IT_IS_BEING_CREATED);
            }
            if (order.isProviderLocal(this.localProviderId)) {
                OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);
            } else {
                CloudConnector cloudConnector = getCloudConnector(order);
                cloudConnector.deleteInstance(order);
                order.setInstanceId(null);
            }
        }
    }

    public Instance getResourceInstance(Order order) throws FogbowException {
        if (order == null) {
            throw new UnexpectedException(Messages.Exception.CORRUPTED_INSTANCE);
        }

        synchronized (order) {
            if ((!this.localProviderId.equals(order.getProvider())) && (order.getOrderState().equals(OrderState.OPEN)
                            || order.getOrderState().equals(OrderState.SELECTED))) {
                // This is an order for a remote provider that has never been received by that provider.
                throw new RequestStillBeingDispatchedException(String.format(cloud.fogbow.common.constants.Messages.Exception.REQUEST_S_STILL_OPEN, order.getId()));
            }
            CloudConnector cloudConnector = getCloudConnector(order);
            Instance instance = cloudConnector.getInstance(order);
            if (order.isProviderLocal(this.localProviderId)) {
                return updateInstanceUsingOrderData(instance, order);
            } else {
                return instance;
            }
        }
    }

    public Allocation getUserAllocation(String providerId, String cloudName, SystemUser systemUser, ResourceType resourceType)
            throws UnexpectedException {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> filteredOrders = orders.stream()
                .filter(order -> order.getType().equals(resourceType))
                .filter(order -> order.getOrderState().equals(OrderState.FULFILLED))
                .filter(order -> order.isProviderLocal(providerId))
                .filter(order -> order.getSystemUser().equals(systemUser))
                .filter(order -> order.getCloudName().equals(cloudName))
                .collect(Collectors.toList());

        switch (resourceType) {
            case COMPUTE:
                List<ComputeOrder> computeOrders = castOrders(filteredOrders);
                return getUserComputeAllocation(computeOrders);
            case VOLUME:
                List<VolumeOrder> volumeOrders = castOrders(filteredOrders);
                return getUserVolumeAllocation(volumeOrders);
            case NETWORK:
                List<NetworkOrder> networkOrders = castOrders(filteredOrders);
                return getUserNetworkAllocation(networkOrders);
            case PUBLIC_IP:
                List<PublicIpOrder> publicIpOrders = castOrders(filteredOrders);
                return getUserPublicIpAllocation(publicIpOrders);
            default:
                throw new UnexpectedException(Messages.Exception.RESOURCE_TYPE_NOT_IMPLEMENTED);
        }
    }

    @VisibleForTesting
    <T extends Order> List<T> castOrders(List<Order> orders) {
        List<T> result = new ArrayList<>();
        for (Order order : orders) {
            result.add((T) order);
        }
        return result;
    }

    @VisibleForTesting
    PublicIpAllocation getUserPublicIpAllocation(List<PublicIpOrder> publicIpOrders) {
        int publicIps = publicIpOrders.size();
        return new PublicIpAllocation(publicIps);
    }

    @VisibleForTesting
    NetworkAllocation getUserNetworkAllocation(List<NetworkOrder> networkOrders) {
        int networks = networkOrders.size();
        return new NetworkAllocation(networks);
    }

    @VisibleForTesting
    VolumeAllocation getUserVolumeAllocation(List<VolumeOrder> volumeOrders) {
        int volumes = volumeOrders.size();
        int storage = 0;

        for (VolumeOrder order : volumeOrders) {
            synchronized (order) {
                VolumeAllocation actualAllocation = order.getActualAllocation();
                storage += actualAllocation.getStorage();
            }
        }
        return new VolumeAllocation(volumes, storage);
    };

    public List<InstanceStatus> getInstancesStatus(SystemUser systemUser, ResourceType resourceType) throws UnexpectedException {
		List<InstanceStatus> instanceStatusList = new ArrayList<>();
		List<Order> allOrders = getAllOrders(systemUser, resourceType);

		for (Order order : allOrders) {
		    synchronized (order) {
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

                // The state of the instance can be inferred from the state of the order. This is not the cloud-dependent
                // state of the instance (that can be consulted by issuing a GET request on a particular instance), but a
                // more generic (and cloud-independent) indication of the instance's state.
                InstanceStatus instanceStatus = new InstanceStatus(
                        order.getId(),
                        name,
                        order.getProvider(),
                        order.getCloudName(),
                        InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState()));

                instanceStatusList.add(instanceStatus);
            }
		}

		return instanceStatusList;
	}

    protected CloudConnector getCloudConnector(Order order) {
        return CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(), order.getCloudName());
    }

    @VisibleForTesting
    ComputeAllocation getUserComputeAllocation(Collection<ComputeOrder> computeOrders) {
        int instances = computeOrders.size();
        int vCPU = 0;
        int ram = 0;
        int disk = 0;

        for (ComputeOrder order : computeOrders) {
            synchronized (order) {
                ComputeAllocation actualAllocation = order.getActualAllocation();
                vCPU += actualAllocation.getvCPU();
                ram += actualAllocation.getRam();
                disk += actualAllocation.getDisk();
            }
        }
        return new ComputeAllocation(instances, vCPU, ram, disk);
    }

	private List<Order> getAllOrders(SystemUser systemUser, ResourceType resourceType) {
		Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

		// Filter all orders of resourceType from the user systemUser.
		List<Order> requestedOrders = orders.stream()
				.filter(order -> order.getType().equals(resourceType))
				.filter(order -> order.getSystemUser().equals(systemUser)).collect(Collectors.toList());

		return requestedOrders;
	}

	public void updateOrderDependencies(Order order, Operation operation) throws UnexpectedException {
        synchronized (order) {
            List<String> dependentOrderIds = new LinkedList<>();

            switch (order.getType()) {
                case ATTACHMENT:
                    AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                    dependentOrderIds.add(attachmentOrder.getComputeOrderId());
                    dependentOrderIds.add(attachmentOrder.getVolumeOrderId());
                    break;
                case COMPUTE:
                    ComputeOrder computeOrder = (ComputeOrder) order;
                    dependentOrderIds.addAll(computeOrder.getNetworkOrderIds());
                    break;
                case PUBLIC_IP:
                    PublicIpOrder publicIpOrder = (PublicIpOrder) order;
                    dependentOrderIds.add(publicIpOrder.getComputeOrderId());
                    break;
                default:
                    // Dependencies apply only to attachment, compute and public IP orders for now.
                    return;
            }

            switch (operation) {
                case CREATE:
                    addOrderDependency(order.getId(), dependentOrderIds);
                    break;
                case DELETE:
                    deleteOrderDependency(order.getId(), dependentOrderIds);
                    break;
                default:
                    throw new UnexpectedException(String.format(Messages.Exception.UNEXPECTED_OPERATION_S, operation));
            }
        }
    }

    private Instance updateInstanceUsingOrderData(Instance instance, Order order) {
        switch (order.getType()) {
            case COMPUTE:
                updateComputeInstanceUsingOrderData(((ComputeInstance) instance), ((ComputeOrder) order));
                break;
            case ATTACHMENT:
                updateAttachmentInstanceUsingOrderData(((AttachmentInstance) instance), ((AttachmentOrder) order));
                break;
            case PUBLIC_IP:
                updatePublicIpInstanceUsingOrderData(((PublicIpOrder) order), ((PublicIpInstance) instance));
                break;
            case NETWORK:
            case VOLUME:
                break;
        }
        // Setting instance common fields that come from the order object
        instance.setProvider(order.getProvider());
        instance.setCloudName(order.getCloudName());
        // The user believes that the order id is actually the instance id.
        // So we need to overwrite the instance id received from the cloud
        // with the order id, before returning the instance to the user.
        instance.setId(order.getId());
        return instance;
    }

    private void updateComputeInstanceUsingOrderData(ComputeInstance instance, ComputeOrder order) {
        String publicKey = order.getPublicKey();
        instance.setImageId(order.getImageId());
        List<UserData> userData = order.getUserData();
        List<NetworkSummary> instanceNetworks = instance.getNetworks();
        if (instanceNetworks == null) {
            instanceNetworks = new ArrayList<>();
        }

        // Remember that the instance ids seen by the user are really order ids, thus, when an order embeds other
        // orders, the instance that is returned needs to display order ids for these embedded orders, and not the
        // corresponding instance ids.
        List<NetworkSummary> mappedNetworks = addPrivateNetworksToMap(instanceNetworks, order);
        instance.setNetworks(mappedNetworks);
        instance.setPublicKey(publicKey);
        instance.setUserData(userData);

        // NOTE(pauloewerton): in order to prevent extra plugin requests to retrieve the specified instance
        // resources allocation, we get those from the order allocation
        ComputeAllocation allocation = order.getActualAllocation();
        instance.setvCPU(allocation.getvCPU());
        instance.setRam(allocation.getRam());
        instance.setDisk(allocation.getDisk());
    }

    private void updateAttachmentInstanceUsingOrderData(AttachmentInstance instance, AttachmentOrder order) {
        // Remember that the instance ids seen by the user are really order ids, thus, when an order embeds other
        // orders, the instance that is returned needs to display order ids for these embedded orders, and not the
        // corresponding instance ids.
        String volumeOrderId = order.getVolumeOrderId();
        String computeOrderId = order.getComputeOrderId();
        ComputeOrder computeOrder = (ComputeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(computeOrderId);
        VolumeOrder volumeOrder = (VolumeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(volumeOrderId);
        instance.setComputeName(computeOrder.getName());
        instance.setComputeId(computeOrder.getId());
        instance.setVolumeName(volumeOrder.getName());
        instance.setVolumeId(volumeOrder.getId());
    }

    private void updatePublicIpInstanceUsingOrderData(PublicIpOrder order, PublicIpInstance instance) {
        // Remember that the instance ids seen by the user are really order ids, thus, when an order embeds other
        // orders, the instance that is returned needs to display order ids for these embedded orders, and not the
        // corresponding instance ids.
        String computeOrderId = order.getComputeOrderId();
        ComputeOrder computeOrder = (ComputeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(computeOrderId);
        String computeInstanceName = computeOrder.getName();
        String computeInstanceId = computeOrder.getId();
        instance.setComputeName(computeInstanceName);
        instance.setComputeId(computeInstanceId);
    }

    private List<NetworkSummary> addPrivateNetworksToMap(List<NetworkSummary> networks, ComputeOrder order) {
        List<NetworkSummary> mappedNetworks = new ArrayList<>();
        Iterator<NetworkSummary> iterator = networks.iterator();
        while (iterator.hasNext()) {
            mappedNetworks.add(iterator.next());
        }
        List<NetworkOrder> networkOrders = order.getNetworkOrders();
        for (NetworkOrder networkOrder : networkOrders) {
            mappedNetworks.add(new NetworkSummary(networkOrder.getId(), networkOrder.getName()));
        }
        return mappedNetworks;
    }

    private void deleteOrderDependency(String orderId, List<String> dependentOrderIds) {
        Order order = SharedOrderHolders.getInstance().getActiveOrdersMap().get(orderId);
        synchronized (order) {
            for (String dependentOrderId : dependentOrderIds) {
                if (this.orderDependencies.containsKey(dependentOrderId)) {
                    List<String> currentList = this.orderDependencies.get(dependentOrderId);
                    if (currentList.remove(orderId)) {
                        this.orderDependencies.put(dependentOrderId, currentList);
                    } else {
                        LOGGER.error(String.format(Messages.Error.COULD_NOT_FIND_DEPENDENCY_S_S, dependentOrderId, orderId));
                    }
                } else {
                    LOGGER.error(String.format(Messages.Error.COULD_NOT_FIND_DEPENDENCY_S_S, dependentOrderId, orderId));
                }
            }
        }
    }

    private void addOrderDependency(String orderId, List<String> dependentOrderIds) {
        Order order = SharedOrderHolders.getInstance().getActiveOrdersMap().get(orderId);
        synchronized (order) {
            for (String dependentOrderId : dependentOrderIds) {
                if (this.orderDependencies.containsKey(dependentOrderId)) {
                    List<String> currentList = this.orderDependencies.get(dependentOrderId);
                    currentList.add(orderId);
                    this.orderDependencies.put(dependentOrderId, currentList);
                } else {
                    List<String> currentList = new LinkedList<>();
                    currentList.add(orderId);
                    this.orderDependencies.put(dependentOrderId, currentList);
                }
            }
        }
    }

    protected boolean hasOrderDependencies(String orderId) {
        Order order = SharedOrderHolders.getInstance().getActiveOrdersMap().get(orderId);
        synchronized (order) {
            if (this.orderDependencies.containsKey(orderId) && !this.orderDependencies.get(orderId).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}

