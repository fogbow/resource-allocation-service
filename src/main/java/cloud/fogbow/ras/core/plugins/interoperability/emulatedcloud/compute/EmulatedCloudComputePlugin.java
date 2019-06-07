package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.compute;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedCompute;
import javafx.util.Pair;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmulatedCloudComputePlugin implements ComputePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudComputePlugin.class);

    private Properties properties;

    public EmulatedCloudComputePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        EmulatedCompute compute = createCompute(computeOrder);

        String computeId = compute.getInstanceId();
        String computePath = EmulatedCloudUtils.getResourcePath(this.properties, computeId);

        try {
            EmulatedCloudUtils.saveFileContent(computePath, compute.toJson());
        } catch (IOException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        return computeId;
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        String computeId = computeOrder.getInstanceId();

        EmulatedCompute compute;
        try {
            String computePath = EmulatedCloudUtils.getResourcePath(this.properties, computeId);
            String jsonContent = EmulatedCloudUtils.getFileContent(computePath);
            compute = EmulatedCompute.fromJson(jsonContent);

        } catch (IOException e) {

            LOGGER.error(Messages.Exception.INSTANCE_NOT_FOUND);
            throw new InstanceNotFoundException(e.getMessage());
        }


        int disk = compute.getDisk();
        int vcpu = compute.getVcpu();
        int memory = compute.getMemory();
        String id = compute.getInstanceId();
        String imageId = compute.getImageId();
        String name = compute.getName();
        String cloudName = compute.getCloudName();
        String provider = compute.getProvider();
        String publicKey = compute.getPublicKey();


        List<NetworkSummary> networks = getNetworkSummaries(compute);

        ComputeInstance computeInstance = new ComputeInstance(id, EmulatedCloudConstants.Plugins.STATE_RUNNING, name,
                vcpu, memory, disk, new ArrayList<>(), imageId, publicKey, new ArrayList());
        
        computeInstance.setNetworks(networks);
        computeInstance.setProvider(provider);
        computeInstance.setCloudName(cloudName);

        return computeInstance;
    }

    private List<NetworkSummary> getNetworkSummaries(EmulatedCompute compute) {
        List<NetworkSummary> summaries = new ArrayList<>();

        for (Pair<String, String> pair : compute.getNetworks()) {
            String id = pair.getKey();
            String name = pair.getValue();
            summaries.add(new NetworkSummary(id, name));
        }

        return summaries;
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        String computeId = computeOrder.getId();
        String computePath = EmulatedCloudUtils.getResourcePath(this.properties, computeId);

        EmulatedCloudUtils.deleteFile(computePath);
    }


    protected EmulatedCompute createCompute(ComputeOrder computeOrder) {
        int disk = computeOrder.getDisk();
        int vcpu = computeOrder.getvCPU();
        int memory = computeOrder.getMemory();
        String imageId = computeOrder.getImageId();

        String name = EmulatedCloudUtils.getName(computeOrder.getName());
        String publicKey = computeOrder.getPublicKey();
        String id = EmulatedCloudUtils.getRandomUUID();

        List<Pair<String, String> > networks = getNetworks(computeOrder);

        EmulatedCompute emulatedCompute = new EmulatedCompute.Builder()
                .disk(disk)
                .vcpu(vcpu)
                .memory(memory)
                .imageId(imageId)
                .name(name)
                .publicKey(publicKey)
                .instanceId(id)
                .cloudState(EmulatedCloudConstants.Plugins.STATE_ACTIVE)
                .build();

        emulatedCompute.setNetworks(networks);

        return emulatedCompute;
    }

    private List getNetworks(ComputeOrder computeOrder) {
        List networks = new ArrayList();

        for (String network : computeOrder.getNetworkOrderIds()){
            networks.add(new Pair<String, String>(network, network));
        }

        return networks;
    }

    private String getName(ComputeOrder computeOrder){
        String name = computeOrder.getName();
        return (name == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + EmulatedCloudUtils.getRandomUUID() : name);
    }
}
