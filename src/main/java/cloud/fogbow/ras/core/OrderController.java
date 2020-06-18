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
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.CloseOrderAtRemoteProviderRequest;
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

    public OrderController() throws InternalServerErrorException {
        this.orderHolders = SharedOrderHolders.getInstance();
        this.orderDependencies = new ConcurrentHashMap<>();
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        // We need to repopulate ordersDependencies after a restart of the service
        updateAllOrdersDependencies();
    }

    public Order getOrder(String orderId) throws InstanceNotFoundException {
        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();
        Order requestedOrder = activeOrdersMap.get(orderId);
        if (requestedOrder == null) {
            throw new InstanceNotFoundException(String.format(Messages.Exception.NOT_FOUND_ORDER_ID_S, orderId));
        }
        return requestedOrder;
    }

    public String activateOrder(Order order) throws FogbowException {
        LOGGER.info(Messages.Log.ACTIVATING_NEW_REQUEST);
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        ChainedList<Order> openOrdersList = sharedOrderHolders.getOpenOrdersList();

        synchronized (activeOrdersMap) {
            String orderId = order.getId();
            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format(Messages.Exception.REQUEST_ID_ALREADY_ACTIVATED_S, orderId);
                throw new InternalServerErrorException(message);
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

    public void closeOrder(Order order) throws InternalServerErrorException {
        synchronized (order) {
            if (order.isRequesterRemote(this.localProviderId)) {
                try {
                    this.notifyRequesterToCloseOrder(order);
                } catch (Exception e) {
                    LOGGER.warn(String.format(Messages.Log.UNABLE_TO_NOTIFY_REQUESTING_PROVIDER_S_S, order.getRequester(),
                            order.getId()), e);
                    return;
                }
            }
            // Will only get here if successfully signaled remote requester (if needed). If the signalling fails, it
            // keeps retrying. If it succeeds, but the provider fails before updating the local order to CLOSED and
            // save it in stable storage, then, upon recovery, it will try to signal again. The remote requester will
            // simply drop this redundant signal (see handleRemoteEvent() in RemoteFacade class).
            SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
            Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
            ChainedList<Order> checkingDeletionOrders = sharedOrderHolders.getCheckingDeletionOrdersList();
            ChainedList<Order> remoteProviderOrders = sharedOrderHolders.getRemoteProviderOrdersList();

            synchronized (activeOrdersMap) {
                if (activeOrdersMap.containsKey(order.getId())) {
                    activeOrdersMap.remove(order.getId());
                } else {
                    String message = String.format(Messages.Exception.UNABLE_TO_REMOVE_INACTIVE_REQUEST_S, order.getId());
                    throw new InternalServerErrorException(message);
                }
            }

            if (order.isProviderLocal(this.localProviderId)) {
                checkingDeletionOrders.removeItem(order);
            } else {
                remoteProviderOrders.removeItem(order);
            }
            order.setOrderState(OrderState.CLOSED);
        }
    }

    public void deleteOrder(Order order) throws FogbowException {
        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (orderState.equals(OrderState.CHECKING_DELETION) ||
                    order.getOrderState().equals(OrderState.ASSIGNED_FOR_DELETION)) {
                throw new UnacceptableOperationException(Messages.Exception.DELETE_OPERATION_ALREADY_ONGOING);
            }
            if (order.isRequesterLocal(this.localProviderId) && hasOrderDependencies(order.getId())) {
                throw new UnacceptableOperationException(String.format(Messages.Exception.DEPENDENCY_DETECTED_S_S,
                        order.getId(), this.orderDependencies.get(order.getId())));
            }
            if (order.getOrderState().equals(OrderState.SELECTED)) {
                // This only happens if the provider has failed between selecting the order and saving the new state.
                // It means that there might be some "garbage" left in the cloud. The Fogbow node admin should
                // take the required actions to remove such garbage. The log below can help in identifying this
                // kind of problem.
                LOGGER.warn(String.format(Messages.Log.REMOVING_ORDER_IN_SELECT_STATE_S, order.toString()));
            }
            if (order.isProviderRemote(this.localProviderId)) {
                try {
                    // Here we know that the CloudConnector is remote, but the use of CloudConnectFactory facilitates testing.
                    RemoteCloudConnector remoteCloudConnector = (RemoteCloudConnector)
                            CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(), order.getCloudName());
                    remoteCloudConnector.deleteInstance(order);
                    // This is just to make sure the remote provider order will be moved to the remoteProviderOrders
                    // list (if it is not already there), since PENDING orders belong to this list.
                    OrderStateTransitioner.transitionToRemoteList(order, OrderState.ASSIGNED_FOR_DELETION);
                } catch (Exception e) {
                    // Here we do not know whether the deleteOrder() has been executed or not at the remote site.
                    // We return to the user as if deletion is on its way and try to figure out what is going on in
                    // the RemoteOrdersStateSynchronization processor.
                    LOGGER.error(Messages.Exception.UNABLE_TO_RETRIEVE_RESPONSE_FROM_PROVIDER_S);
                    throw e;
                }
            } else {
                OrderStateTransitioner.transition(order, OrderState.ASSIGNED_FOR_DELETION);
            }
        }
    }

    public Instance getResourceInstance(Order order) throws FogbowException {
        synchronized (order) {
            CloudConnector cloudConnector = getCloudConnector(order);
            if (order.isProviderLocal(this.localProviderId)) {
                Instance instance = cloudConnector.getInstance(order);
                return updateInstanceUsingOrderData(instance, order);
            } else if (order.getOrderState().equals(OrderState.OPEN) || order.getOrderState().equals(OrderState.SELECTED)) {
                // This is an order for a remote provider that has never been received by that provider.
                // We create an empty Instance and update the Instance fields with the values held in the order.
                InstanceState instanceState = InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState());
                OrderInstance emptyInstance = EmptyOrderInstanceGenerator.createEmptyInstance(order);
                emptyInstance.setState(instanceState);
                return updateInstanceUsingOrderData(emptyInstance, order);
            } else {
                return cloudConnector.getInstance(order);
            }

        }
    }

    public Allocation getUserAllocation(String providerId, String cloudName, SystemUser systemUser, ResourceType resourceType)
            throws InternalServerErrorException {

        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();

        Collection<Order> orders = activeOrdersMap.values();

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
                throw new InternalServerErrorException(Messages.Exception.RESOURCE_TYPE_NOT_IMPLEMENTED);
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
    }

    ;

    public List<InstanceStatus> getInstancesStatus(SystemUser systemUser, ResourceType resourceType) throws InternalServerErrorException {
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
                        InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState(), false, false, false));

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
        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();

        Collection<Order> orders = activeOrdersMap.values();

        // Filter all orders of resourceType from the user systemUser.
        List<Order> requestedOrders = orders.stream()
                .filter(order -> order.getType().equals(resourceType))
                .filter(order -> order.getSystemUser().equals(systemUser)).collect(Collectors.toList());

        return requestedOrders;
    }

    public void updateOrderDependencies(Order order, Operation operation) throws InternalServerErrorException {
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
                    throw new InternalServerErrorException(String.format(Messages.Exception.UNEXPECTED_OPERATION_S, operation));
            }
        }
    }

    @VisibleForTesting
    void updateAllOrdersDependencies() throws InternalServerErrorException {
        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();

        Collection<Order> allOrders = activeOrdersMap.values();

        for (Order order : allOrders) {
            // No need to synchronize as this is only executed at startup time, and the processor threads
            // have not yet been started.
            if (order.isRequesterLocal(this.localProviderId)) {
                this.updateOrderDependencies(order, Operation.CREATE);
            }
        }
    }

    @VisibleForTesting
    Instance updateInstanceUsingOrderData(Instance instance, Order order) {
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

        // In order to avoid extra plugin requests to retrieve the specified instance
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
                        LOGGER.error(String.format(Messages.Log.COULD_NOT_FIND_DEPENDENCY_S_S, dependentOrderId, orderId));
                    }
                } else {
                    LOGGER.error(String.format(Messages.Log.COULD_NOT_FIND_DEPENDENCY_S_S, dependentOrderId, orderId));
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

    protected void notifyRequesterToCloseOrder(Order order) throws FogbowException {
        try {
            CloseOrderAtRemoteProviderRequest closeOrderAtRemoteProviderRequest = new CloseOrderAtRemoteProviderRequest(order);
            closeOrderAtRemoteProviderRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

}

