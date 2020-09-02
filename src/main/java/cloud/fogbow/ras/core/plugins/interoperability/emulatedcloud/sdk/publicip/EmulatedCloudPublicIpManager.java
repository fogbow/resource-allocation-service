package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip.models.EmulatedPublicIp;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule.EmulatedCloudSecurityRuleManager;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudPublicIpManager implements ResourceManager<EmulatedPublicIp> {
    private static EmulatedCloudPublicIpManager instance;
    private Map<String, EmulatedPublicIp> publicIps;

    private EmulatedCloudPublicIpManager() {
        this.publicIps = new HashMap<>();
    }

    public static EmulatedCloudPublicIpManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudPublicIpManager();
        }
        return instance;
    }

    @Override
    public Optional<EmulatedPublicIp> find(String instanceId) {
        return Optional.ofNullable(this.publicIps.get(instanceId));
    }

    @Override
    public List<EmulatedPublicIp> list() {
        return new ArrayList<>(this.publicIps.values());
    }

    @Override
    public String create(EmulatedPublicIp publicIps) {
        EmulatedCloudUtils.validateEmulatedResource(publicIps);
        this.publicIps.put(publicIps.getInstanceId(), publicIps);
        return publicIps.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (this.publicIps.containsKey(instanceId)) {
            this.publicIps.remove(instanceId);
            String securityGroupId = EmulatedCloudUtils.getPublicIpSecurityGroupId(instanceId);
            EmulatedCloudSecurityRuleManager.getInstance().deleteBySecurityGroup(securityGroupId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }
}
