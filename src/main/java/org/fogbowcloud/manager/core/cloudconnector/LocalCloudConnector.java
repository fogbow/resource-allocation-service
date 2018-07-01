package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.OrderToInstanceStateMapper;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.util.connectivity.ComputeInstanceConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.SshConnectivityUtil;
import org.fogbowcloud.manager.util.connectivity.SshTunnelConnectionData;
import org.fogbowcloud.manager.util.connectivity.TunnelingServiceUtil;

import java.util.Map;

public class LocalCloudConnector implements CloudConnector {

    private final AaController aaController;
    private final AttachmentPlugin attachmentPlugin;
    private final ComputePlugin computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;
    private final ImagePlugin imagePlugin;

    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    public LocalCloudConnector(AaController aaController, CloudPluginsHolder cloudPluginsHolder) {
        this.aaController = aaController;
        this.attachmentPlugin = cloudPluginsHolder.getAttachmentPlugin();
        this.computePlugin = cloudPluginsHolder.getComputePlugin();
        this.computeQuotaPlugin = cloudPluginsHolder.getComputeQuotaPlugin();
        this.networkPlugin = cloudPluginsHolder.getNetworkPlugin();
        this.volumePlugin = cloudPluginsHolder.getVolumePlugin();
        this.imagePlugin = cloudPluginsHolder.getImagePlugin();
    }

    @Override
    public String requestInstance(Order order) throws FogbowManagerException, UnexpectedException {
        String requestInstance = null;
        Token localToken = this.aaController.getLocalToken(order.getFederationUser());
        switch (order.getType()) {
            case COMPUTE:
                ComputeOrder computeOrder = (ComputeOrder) order;
                requestInstance = this.computePlugin.requestInstance(computeOrder, localToken);
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                requestInstance = this.networkPlugin.requestInstance(networkOrder, localToken);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                requestInstance = this.volumePlugin.requestInstance(volumeOrder, localToken);
                break;
            case ATTACHMENT:
                AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                requestInstance = this.attachmentPlugin.requestInstance(attachmentOrder, localToken);
                break;
            default:
                String message = "No requestInstance plugin implemented for order " + order.getType();
                throw new UnexpectedException(message);
        }
        if (requestInstance == null) {
            String message = "Plugin returned a null value for the instanceId.";
            throw new UnexpectedException(message);
        }
        return requestInstance;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowManagerException {

        if (order.getInstanceId() != null) {
            Token localToken = this.aaController.getLocalToken(order.getFederationUser());
            switch (order.getType()) {
                case COMPUTE:
                    this.computePlugin.deleteInstance(order.getInstanceId(), localToken);
                    break;
                case VOLUME:
                    this.volumePlugin.deleteInstance(order.getInstanceId(), localToken);
                    break;
                case NETWORK:
                    this.networkPlugin.deleteInstance(order.getInstanceId(), localToken);
                    break;
                case ATTACHMENT:
                    this.attachmentPlugin.deleteInstance(order.getInstanceId(), localToken);
                    break;
                default:
                    LOGGER.error("No deleteInstance plugin implemented for order " + order.getType());
                    break;
            }
        } else {
            LOGGER.error("Trying to delete an instance with no instanceId.");
        }
    }

    @Override
    public Instance getInstance(Order order) throws FogbowManagerException, UnexpectedException {
        Instance instance;
        Token localToken = this.aaController.getLocalToken(order.getFederationUser());

        synchronized (order) {
            String instanceId = order.getInstanceId();

            if (instanceId != null) {
                instance = getResourceInstance(order, order.getType(), localToken);
            } else {
                // When there is no instance, an empty one is created with the appropriate state
                switch (order.getType()) {
                    case COMPUTE:
                        instance = new ComputeInstance(order.getId());
                        break;
                    case VOLUME:
                        instance = new VolumeInstance(order.getId());
                        break;
                    case NETWORK:
                        instance = new NetworkInstance(order.getId());
                        break;
                    case ATTACHMENT:
                        instance = new AttachmentInstance(order.getId());
                        break;
                    default:
                        String message = "Not supported order type " + order.getType();
                        throw new UnexpectedException(message);
                }
                // The state of the instance can be inferred from the state of the order
                instance.setState(OrderToInstanceStateMapper.map(order.getOrderState(), order.getType()));
            }
        }
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws
            FogbowManagerException, UnexpectedException {

        Token localToken = this.aaController.getLocalToken(federationUser);

        switch (instanceType) {
            case COMPUTE:
                return this.computeQuotaPlugin.getUserQuota(localToken);
            default:
                throw new UnexpectedException("Not yet implemented quota endpoint for " + instanceType);
        }
    }

    @Override
    public Map<String, String> getAllImages(FederationUser federationUser) throws FogbowManagerException {
        Token localToken = this.aaController.getLocalToken(federationUser);
        return this.imagePlugin.getAllImages(localToken);
    }

    @Override
    public Image getImage(String imageId, FederationUser federationUser) throws FogbowManagerException {
        Token localToken = this.aaController.getLocalToken(federationUser);
        return this.imagePlugin.getImage(imageId, localToken);
    }

    private Instance getResourceInstance(Order order, InstanceType instanceType, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        Instance instance;
        String instanceId = order.getInstanceId();

        switch (instanceType) {
            case COMPUTE:
                instance = this.computePlugin.getInstance(instanceId, localToken);
                addReverseTunnelInfo(order.getId(), (ComputeInstance) instance);
                break;
            case NETWORK:
                instance = this.networkPlugin.getInstance(instanceId, localToken);
                break;
            case VOLUME:
                instance = this.volumePlugin.getInstance(instanceId, localToken);
                break;
            case ATTACHMENT:
                instance = this.attachmentPlugin.getInstance(instanceId, localToken);
                break;
            default:
                String message = "Not supported order type " + order.getType();
                throw new UnexpectedException(message);
        }
        return instance;
    }

    private void addReverseTunnelInfo(String orderId, ComputeInstance computeInstance) {
        TunnelingServiceUtil tunnelingServiceUtil = TunnelingServiceUtil.getInstance();
        SshConnectivityUtil sshConnectivityUtil = SshConnectivityUtil.getInstance();

        ComputeInstanceConnectivityUtil computeInstanceConnectivity =
                new ComputeInstanceConnectivityUtil(tunnelingServiceUtil, sshConnectivityUtil);

        SshTunnelConnectionData sshTunnelConnectionData = computeInstanceConnectivity
                .getSshTunnelConnectionData(orderId);

        computeInstance.setSshTunnelConnectionData(sshTunnelConnectionData);
    }

}