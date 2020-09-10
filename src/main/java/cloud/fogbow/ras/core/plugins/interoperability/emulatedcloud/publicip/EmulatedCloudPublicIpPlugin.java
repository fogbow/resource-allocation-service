package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.publicip;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip.models.EmulatedPublicIp;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip.EmulatedCloudPublicIpManager;
import org.apache.log4j.Logger;

import java.util.*;

public class EmulatedCloudPublicIpPlugin implements PublicIpPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudPublicIpPlugin.class);

    private Properties properties;

    public EmulatedCloudPublicIpPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        EmulatedPublicIp publicIp = createEmulatedPublicIp(publicIpOrder);
        EmulatedCloudPublicIpManager publicIpManager = EmulatedCloudPublicIpManager.getInstance();
        String instanceId = publicIpManager.create(publicIp);
        return instanceId;
    }

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = publicIpOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        EmulatedCloudPublicIpManager publicIpManager = EmulatedCloudPublicIpManager.getInstance();
        publicIpManager.delete(instanceId);
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = publicIpOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        EmulatedCloudPublicIpManager publicIpManager = EmulatedCloudPublicIpManager.getInstance();

        Optional<EmulatedPublicIp> emulatedPublicIp = publicIpManager.find(instanceId);

        if (emulatedPublicIp.isPresent()) {
            return buildPublicIpInstance(emulatedPublicIp.get());
        } else {
            throw new InstanceNotFoundException();
        }
    }

    private PublicIpInstance buildPublicIpInstance(EmulatedPublicIp publicIp) {
        String instanceId = publicIp.getInstanceId();
        String cloudState = publicIp.getCloudState();
        String ip = publicIp.getIp();
        return new PublicIpInstance(instanceId, cloudState, ip);
    }

    @Override
    public boolean isReady(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.FAILED);
    }

    private EmulatedPublicIp createEmulatedPublicIp(PublicIpOrder publicIpOrder) {
        String computeId = publicIpOrder.getComputeId();
        String provider = publicIpOrder.getProvider();
        String cloudName = publicIpOrder.getCloudName();
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String ip = EmulatedCloudUtils.generateRandomIP();
        String cloudState = EmulatedCloudStateMapper.ACTIVE_STATUS;

        EmulatedPublicIp emulatedPublicIp = new EmulatedPublicIp.Builder()
            .cloudState(cloudState)
            .computeId(computeId)
            .cloudName(cloudName)
            .instanceId(instanceId)
            .ip(ip)
            .provider(provider)
            .build();

        return emulatedPublicIp;
    }
}
