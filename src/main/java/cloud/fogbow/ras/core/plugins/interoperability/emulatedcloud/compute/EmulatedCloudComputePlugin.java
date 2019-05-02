package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.compute;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class EmulatedCloudComputePlugin implements ComputePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudComputePlugin.class);

    private static final String COMPUTE_CLOUD_NAME = "cloudName";
    private static final String COMPUTE_DISK = "disk";
    private static final String COMPUTE_IMAGE_ID = "imageId";
    private static final String COMPUTE_MEMORY = "memory";
    private static final String COMPUTE_NAME = "name";
    private static final String COMPUTE_PROVIDER = "provider";
    private static final String COMPUTE_PUBLIC_KEY = "publickKey";
    private static final String COMPUTE_VCPU = "vcpu";
    private static final String COMPUTE_NETWORKS = "networks";

    private Properties properties;

    private static final String COMPUTE_ID = "id";

    public EmulatedCloudComputePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        HashMap compute = createCompute(computeOrder);

        String computeId = (String) compute.get(COMPUTE_ID);
        String computePath = EmulatedCloudUtils.getResourcePath(this.properties, computeId);

        try {
            EmulatedCloudUtils.saveHashMapAsJson(computePath, compute);
        } catch (IOException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        return computeId;
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
        String computeId = computeOrder.getInstanceId();
        HashMap<String, String> compute;
        HashMap<String, Double> computeWithIntMembers;

        try {
            String computePath = EmulatedCloudUtils.getResourcePath(this.properties, computeId);

            HashMap computeFromStorage = EmulatedCloudUtils.readJsonAsHashMap(computePath);
            compute = computeFromStorage;
            computeWithIntMembers = computeFromStorage;

        } catch (IOException e) {

            LOGGER.error(Messages.Exception.INSTANCE_NOT_FOUND);
            throw new InstanceNotFoundException(e.getMessage());
        }


        int disk = computeWithIntMembers.get(COMPUTE_DISK).intValue();
        int vcpu = computeWithIntMembers.get(COMPUTE_VCPU).intValue();
        int memory = computeWithIntMembers.get(COMPUTE_MEMORY).intValue();
        String id = compute.get(COMPUTE_ID);
        String imageId = compute.get(COMPUTE_IMAGE_ID);
        String name = compute.get(COMPUTE_NAME);
        String cloudName = compute.get(COMPUTE_CLOUD_NAME);
        String provider = compute.get(COMPUTE_PROVIDER);
        String publicKey = compute.get(COMPUTE_PUBLIC_KEY);


        // This doesn't smell good but works.
        HashMap computeWithNetworks = compute;
        HashMap networks;

        LinkedTreeMap linkedTreeMap = (LinkedTreeMap) computeWithNetworks.get(COMPUTE_NETWORKS);
        Gson gson = GsonHolder.getInstance();
        networks = gson.fromJson(gson.toJson(linkedTreeMap), HashMap.class);


        ComputeInstance computeInstance = new ComputeInstance(id, "running", name,
                vcpu, memory, disk, new ArrayList<>(), imageId, publicKey, new ArrayList());
        
        computeInstance.setNetworks(networks);
        computeInstance.setProvider(provider);
        computeInstance.setCloudName(cloudName);

        return computeInstance;
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


    protected HashMap createCompute(ComputeOrder computeOrder) {
        int disk = computeOrder.getDisk();
        int vcpu = computeOrder.getvCPU();
        int memory = computeOrder.getMemory();
        String imageId = computeOrder.getImageId();

        String name = EmulatedCloudUtils.getName(computeOrder.getName());
        String cloudName = computeOrder.getCloudName();
        String provider = computeOrder.getProvider();
        String publicKey = computeOrder.getPublicKey();
        String id = EmulatedCloudUtils.getRandomUUID();

        HashMap networks = getNetworks(computeOrder);

        HashMap compute = new HashMap();

        compute.put(COMPUTE_ID, id);
        compute.put(COMPUTE_DISK, disk);
        compute.put(COMPUTE_VCPU, vcpu);
        compute.put(COMPUTE_MEMORY, memory);
        compute.put(COMPUTE_IMAGE_ID, imageId);
        compute.put(COMPUTE_NAME, name);
        compute.put(COMPUTE_CLOUD_NAME, cloudName);
        compute.put(COMPUTE_PROVIDER, provider);
        compute.put(COMPUTE_PUBLIC_KEY, publicKey);
        compute.put(COMPUTE_NETWORKS, networks);

        return compute;
    }

    private HashMap getNetworks(ComputeOrder computeOrder) {
        HashMap networks = new HashMap();

        for (String network : computeOrder.getNetworkOrderIds()){
            networks.put(network, "custom");
        }

        return networks;
    }

    private String getName(ComputeOrder computeOrder){
        String name = computeOrder.getName();
        return (name == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + EmulatedCloudUtils.getRandomUUID() : name);
    }
}
