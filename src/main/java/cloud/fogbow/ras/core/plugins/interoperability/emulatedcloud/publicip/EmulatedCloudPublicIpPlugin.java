package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.publicip;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedPublicIp;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class EmulatedCloudPublicIpPlugin implements PublicIpPlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudPublicIpPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        EmulatedPublicIp publicIp = generateJsonEntityToCreateInstance(publicIpOrder);

        String publicIpInstanceId = publicIp.getId();

        String publicIpJson = publicIp.toJson();

        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, publicIpInstanceId);

        try {
            EmulatedCloudUtils.saveFileContent(publicIpPath, publicIpJson);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }

        return publicIpInstanceId;
    }

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        String publicIpId = publicIpOrder.getInstanceId();
        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, publicIpId);

        EmulatedCloudUtils.deleteFile(publicIpPath);
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = publicIpOrder.getInstanceId();

        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, instanceId);
        String publicIpJson = null;

        try {
            publicIpJson = EmulatedCloudUtils.getFileContent(publicIpPath);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }

        EmulatedPublicIp publicIp = EmulatedPublicIp.fromJson(publicIpJson);

        String cloudState = publicIp.getCloudState();
        String ip = publicIp.getIp();

        return new PublicIpInstance(instanceId, cloudState, ip);
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    private EmulatedPublicIp generateJsonEntityToCreateInstance(PublicIpOrder publicIpOrder) {
        String computeId = publicIpOrder.getComputeId();
        String provider = publicIpOrder.getProvider();
        String cloudName = publicIpOrder.getCloudName();
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String ip = this.generaterandomIp();

        EmulatedPublicIp emulatedPublicIp = new EmulatedPublicIp.Builder()
            .cloudState(EmulatedCloudConstants.Plugins.STATE_ACTIVE)
            .computeId(computeId)
            .cloudName(cloudName)
            .id(instanceId)
            .ip(ip)
            .provider(provider)
            .state(EmulatedCloudConstants.Plugins.STATE_READY)
            .build();

        return emulatedPublicIp;
    }

    private String generaterandomIp() {
        Random rand = new Random();
        String[] octets = new String[4];

        for (int i = 0; i < 4; ++i) {
            int newOctet = rand.nextInt(250) + 3;
            octets[i] = String.valueOf(newOctet);
        }

        return String.join(".", octets);
    }
}
