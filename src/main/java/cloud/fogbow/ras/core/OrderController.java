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
            ChainedList<Order> closedOrders = sharedOrderHolders.getClosedOrdersList();

            if (activeOrdersMap.containsKey(order.getId())) {
                activeOrdersMap.remove(order.getId());
            } else {
                String message = String.format(Messages.Exception.UNABLE_TO_REMOVE_INACTIVE_REQUEST, order.getId());
                throw new UnexpectedException(message);
            }
            closedOrders.removeItem(order);
            order.setInstanceId(null);
            order.setOrderState(OrderState.DEACTIVATED);
        }
    }

    public void deleteOrder(Order order) throws FogbowException {
        if (order == null)
            throw new UnexpectedException(Messages.Exception.CORRUPTED_INSTANCE);

        // This code may be executed by two different providers for the same logical order. This is the case when the
        // provider provider is remote. In this case, some parts of the code should be executed only at the provider that
        // receives the request through its REST API, while other parts should be executed only by the provider that
        // is providing resources to the order.
        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (!(orderState.equals(OrderState.CLOSED) || order.equals(OrderState.DEACTIVATED))) {
                // Sometimes an order depends on other orders (ex. an attachment depends on a volume and a compute).
                // We need to verify whether another order depends on this order, and if this is the case, throw a
                // DependencyDetectedException. Only the provider that is receiving the delete request through its
                // REST API needs to check order dependencies.
                if (order.isRequesterLocal(this.localProviderId) && hasOrderDependencies(order.getId())) {
                    throw new DependencyDetectedException(String.format(Messages.Exception.DEPENDENCY_DETECTED, order.getId(),
                            this.orderDependencies.get(order.getId())));
                }
                try {
                    // A local order that doesn't have an instance associated to it, need not be deleted in the cloud.
                    // Also, a remote order that is still open has never been sent to the remote provider, therefore,
                    // should not have the delete request forwarded.
                    if ((order.getInstanceId() != null) ||
                           (order.isProviderRemote(this.localProviderId) && (order.getOrderState() != OrderState.OPEN))) {
                       CloudConnector cloudConnector = getCloudConnector(order);
                       cloudConnector.deleteInstance(order);
                       order.setInstanceId(null);
                    }
                } catch (FogbowException e) {
                    LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, order.getId()), e);
                    throw e;
                }
                // When the provider is remote, both local and remote providers call this code and move their
                // respective orders to CLOSED.
                OrderStateTransitioner.transition(order, OrderState.CLOSED);
                // Remove any references that related dependencies of other orders with the order that has
                // just been deleted. Only the provider that is receiving the delete request through its
                // REST API needs to update order dependencies.
                if (order.isRequesterLocal(this.localProviderId)) {
                    updateOrderDependencies(order, Operation.DELETE);
                }
            } else {
                String message = String.format(Messages.Error.REQUEST_ALREADY_CLOSED, order.getId());
                throw new InstanceNotFoundException(message);
            }
        }
    }

    public Instance getResourceInstance(Order order) throws FogbowException {
        if (order == null) {
            throw new UnexpectedException(Messages.Exception.CORRUPTED_INSTANCE);
        }

        synchronized (order) {
            if ((!this.localProviderId.equals(order.getProvider())) && order.getOrderState().equals(OrderState.OPEN)) {
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

    public Allocation getUserAllocation(String providerId, SystemUser systemUser, ResourceType resourceType)
            throws UnexpectedException {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> filteredOrders = orders.stream()
                .filter(order -> order.getType().equals(resourceType))
                .filter(order -> order.getOrderState().equals(OrderState.FULFILLED))
                .filter(order -> order.isProviderLocal(providerId))
                .filter(order -> order.getSystemUser().equals(systemUser))
                .collect(Collectors.toList());

        switch (resourceType) {
            case COMPUTE:
                List<ComputeOrder> computeOrders = new ArrayList<>();
                for (Order order : filteredOrders) {
                    computeOrders.add((ComputeOrder) order);
                }
                return getUserComputeAllocation(computeOrders);
            case VOLUME:
                List<VolumeOrder> volumeOrders = new ArrayList<>();
                for (Order order : filteredOrders) {
                    volumeOrders.add((VolumeOrder) order);
                }
                return getUserVolumeAllocation(volumeOrders);
            case NETWORK:
                List<NetworkOrder> networkOrders = new ArrayList<>();
                for (Order order : filteredOrders) {
                    networkOrders.add((NetworkOrder) order);
                }
                return getUserNetworkAllocation(networkOrders);
            case PUBLIC_IP:
                List<PublicIpOrder> publicIpOrders = new ArrayList<>();
                for (Order order : filteredOrders) {
                    publicIpOrders.add((PublicIpOrder) order);
                }
                return getUserPublicIpAllocation(publicIpOrders);
            default:
                throw new UnexpectedException(Messages.Exception.RESOURCE_TYPE_NOT_IMPLEMENTED);
        }
    }

    private PublicIpAllocation getUserPublicIpAllocation(List<PublicIpOrder> publicIpOrders) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    }

    private NetworkAllocation getUserNetworkAllocation(List<NetworkOrder> networkOrders) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    }

    private VolumeAllocation getUserVolumeAllocation(List<VolumeOrder> volumeOrders) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    };

    public List<InstanceStatus> getInstancesStatus(SystemUser systemUser, ResourceType resourceType) throws InstanceNotFoundException {
		List<InstanceStatus> instanceStatusList = new ArrayList<>();
		List<Order> allOrders = getAllOrders(systemUser, resourceType);

		for (Order order : allOrders) {
		    synchronized (order) {
		        if (order.getOrderState() == OrderState.CLOSED || order.getOrderState() == OrderState.DEACTIVATED) {
		            // The order might have been closed or deactivated between the time the list of orders were
                    // fetched by the getAllOrders() call above and now.
		            continue;
                }
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

    private ComputeAllocation getUserComputeAllocation(Collection<ComputeOrder> computeOrders) {
        int vCPU = 0;
        int ram = 0;
        int instances = 0;
        int disk = 0;

        for (ComputeOrder order : computeOrders) {
            synchronized (order) {
                ComputeAllocation actualAllocation = order.getActualAllocation();
                vCPU += actualAllocation.getvCPU();
                ram += actualAllocation.getRam();
                instances += actualAllocation.getInstances();
                disk += actualAllocation.getDisk();
            }
        }

        return new ComputeAllocation(vCPU, ram, instances, disk);
    }

	private List<Order> getAllOrders(SystemUser systemUser, ResourceType resourceType) {
		Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

		// Filter all orders of resourceType from the user systemUser that are not
		// closed (closed orders have been deleted by the user and should not be seen;
		// they will disappear from the system as soon as the closedProcessor thread
		// process them) or deactivated.
		List<Order> requestedOrders = orders.stream()
				.filter(order -> order.getType().equals(resourceType))
				.filter(order -> order.getSystemUser().equals(systemUser))
                .filter(order -> !order.getOrderState().equals(OrderState.DEACTIVATED))
				.filter(order -> !order.getOrderState().equals(OrderState.CLOSED)).collect(Collectors.toList());

		return requestedOrders;
	}

	private void updateOrderDependencies(Order order, Operation operation) throws FogbowException {
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

        // NOTE(pauloewerton): in order to prevent extra plugin requests to retrieve the specified instance resources allocation,
        // we get those from the order allocation
        ComputeAllocation allocation = order.getActualAllocation();
        instance.setvCPU(allocation.getvCPU());
        instance.setMemory(allocation.getRam());
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

