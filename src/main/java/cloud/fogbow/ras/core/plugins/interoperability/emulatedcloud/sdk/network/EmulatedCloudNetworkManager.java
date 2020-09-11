package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network.models.EmulatedNetwork;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule.EmulatedCloudSecurityRuleManager;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudNetworkManager implements ResourceManager<EmulatedNetwork> {
    private static EmulatedCloudNetworkManager instance;
    private Map<String, EmulatedNetwork> networks;

    private EmulatedCloudNetworkManager() {
        this.networks = new HashMap<>();
    }

    public static EmulatedCloudNetworkManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudNetworkManager();
        }
        return instance;
    }

    @Override
    public Optional<EmulatedNetwork> find(String instanceId) {
        return Optional.ofNullable(this.networks.get(instanceId));
    }

    @Override
    public List<EmulatedNetwork> list() {
        return new ArrayList<>(this.networks.values());
    }

    @Override
    public String create(EmulatedNetwork network) {
        EmulatedCloudUtils.validateEmulatedResource(network);
        this.networks.put(network.getInstanceId(), network);
        return network.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (this.networks.containsKey(instanceId)) {
            this.networks.remove(instanceId);
            String securityGroupId = EmulatedCloudUtils.getNetworkSecurityGroupId(instanceId);
            EmulatedCloudSecurityRuleManager.getInstance().deleteBySecurityGroup(securityGroupId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }
}
