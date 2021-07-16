package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.compute;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.EmulatedCloudComputeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.models.EmulatedCompute;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class EmulatedCloudComputePlugin implements ComputePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudComputePlugin.class);

    private Properties properties;

    public EmulatedCloudComputePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        EmulatedCloudComputeManager computeManager = EmulatedCloudComputeManager.getInstance();
        EmulatedCompute compute = createCompute(computeOrder);
        String computeId = computeManager.create(compute);
        updateInstanceAllocation(computeOrder);
        return computeId;
    }

    private void updateInstanceAllocation(ComputeOrder computeOrder) {
        synchronized (computeOrder) {
            int instances = 1;
            int vCPU = computeOrder.getvCPU();
            int ram = computeOrder.getRam();
            int disk = computeOrder.getDisk();
            ComputeAllocation allocation = new ComputeAllocation(instances, vCPU, ram, disk);
            computeOrder.setActualAllocation(allocation);
        }
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        EmulatedCloudComputeManager computeManager = EmulatedCloudComputeManager.getInstance();
        Optional<EmulatedCompute> optionalEmulatedCompute = computeManager.find(instanceId);

        if (optionalEmulatedCompute.isPresent()) {
            return this.buildComputeInstance(optionalEmulatedCompute.get());
        } else {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    private ComputeInstance buildComputeInstance(EmulatedCompute compute) {
        int disk = compute.getDisk();
        int vCPU = compute.getvCPU();
        int memory = compute.getMemory();
        String id = compute.getInstanceId();
        String imageId = compute.getImageId();
        String name = compute.getName();
        String cloudName = compute.getCloudName();
        String provider = compute.getProvider();
        String publicKey = compute.getPublicKey();
        List<NetworkSummary> networks = compute.getNetworks();

        ComputeInstance computeInstance = new ComputeInstance(id, compute.getCloudState(), name,
                vCPU, memory, disk, new ArrayList<>(), imageId, publicKey, new ArrayList());

        computeInstance.setNetworks(networks);
        computeInstance.setProvider(provider);
        computeInstance.setCloudName(cloudName);

        return computeInstance;
    }

    @Override
    public boolean isReady(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        EmulatedCloudComputeManager computeManager = EmulatedCloudComputeManager.getInstance();
        computeManager.delete(instanceId);
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, CloudUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void pauseInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void hibernateInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        setCloudState(order, EmulatedCloudStateMapper.HIBERNATED_STATUS);
    }

    @Override
    public void stopInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        setCloudState(order, EmulatedCloudStateMapper.STOPPED_STATUS);
    }

    @Override
    public void resumeInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        setCloudState(order, EmulatedCloudStateMapper.ACTIVE_STATUS);
    }
    
    private void setCloudState(ComputeOrder order, String cloudState) throws InstanceNotFoundException {
        String instanceId = order.getInstanceId();
        EmulatedCloudComputeManager computeManager = EmulatedCloudComputeManager.getInstance();
        Optional<EmulatedCompute> optionalEmulatedCompute = computeManager.find(instanceId);

        if (optionalEmulatedCompute.isPresent()) {
            EmulatedCompute compute = optionalEmulatedCompute.get();
            compute.setCloudState(cloudState);
        } else {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }
    
    @Override
    public boolean isPaused(String cloudState) throws FogbowException {
        return false;
    }

    @Override
    public boolean isHibernated(String cloudState) throws FogbowException {
        return EmulatedCloudStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.HIBERNATED);
    }

    @Override
    public boolean isStopped(String cloudState) throws FogbowException {
        return EmulatedCloudStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.STOPPED);
    }
    
    private EmulatedCompute createCompute(ComputeOrder computeOrder) {
        int disk = computeOrder.getDisk();
        int vCPU = computeOrder.getvCPU();
        int memory = computeOrder.getRam();
        String imageId = computeOrder.getImageId();

        String name = EmulatedCloudUtils.getName(computeOrder.getName());
        String publicKey = computeOrder.getPublicKey();
        String id = EmulatedCloudUtils.getRandomUUID();

        List<NetworkSummary> networks = getNetworks(computeOrder);

        EmulatedCompute emulatedCompute = new EmulatedCompute.Builder()
                .disk(disk)
                .vCPU(vCPU)
                .memory(memory)
                .imageId(imageId)
                .name(name)
                .publicKey(publicKey)
                .instanceId(id)
                .cloudState(EmulatedCloudStateMapper.ACTIVE_STATUS)
                .networks(networks)
                .build();

        return emulatedCompute;
    }

    private List getNetworks(ComputeOrder computeOrder) {
        List networks = new ArrayList();

        for (String network : computeOrder.getNetworkOrderIds()){
            networks.add(new NetworkSummary(network, network));
        }

        return networks;
    }
}
