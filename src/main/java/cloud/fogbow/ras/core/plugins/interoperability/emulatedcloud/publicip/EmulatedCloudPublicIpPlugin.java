package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.publicip;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedPublicIp;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedSecurityGroup;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedSecurityRule;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class EmulatedCloudPublicIpPlugin implements PublicIpPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudPublicIpPlugin.class);

    private Properties properties;

    public EmulatedCloudPublicIpPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        EmulatedPublicIp publicIp = generateJsonEntityToCreateInstance(publicIpOrder);

        String publicIpInstanceId = publicIp.getInstanceId();

        String publicIpJson = publicIp.toJson();

        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, publicIpInstanceId);

        try {
            this.createSecurityGroup(publicIpInstanceId);
            EmulatedCloudUtils.saveFileContent(publicIpPath, publicIpJson);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }

        return publicIpInstanceId;
    }

    private void createSecurityGroup(String publicIpInstanceId) throws IOException {
        String id = EmulatedCloudUtils.getPublicIpSecurityGroupId(publicIpInstanceId);
        EmulatedSecurityGroup securityGroup = this.buildEmulatedSecurityGroup(id);
        String path = EmulatedCloudUtils.getResourcePath(this.properties, id);
        EmulatedCloudUtils.saveFileContent(path, securityGroup.toJson());
    }

    private EmulatedSecurityGroup buildEmulatedSecurityGroup(String id) {
        List<String> securityRuleIdList = new ArrayList<>();

        return new EmulatedSecurityGroup.Builder()
                .id(id)
                .securityRules(securityRuleIdList)
                .build();
    }

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        String publicIpId = publicIpOrder.getInstanceId();
        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, publicIpId);
        EmulatedCloudUtils.deleteFile(publicIpPath);

        String securityGroupId = EmulatedCloudUtils.getPublicIpSecurityGroupId(publicIpId);
        deleteSecurityRules(securityGroupId);
        deleteSecurityGroup(securityGroupId);
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = publicIpOrder.getInstanceId();

        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, instanceId);
        String publicIpJson = null;

        try {
            publicIpJson = EmulatedCloudUtils.getFileContent(publicIpPath);
        } catch (IOException e) {
            LOGGER.error(Messages.Exception.INSTANCE_NOT_FOUND);
            throw new InstanceNotFoundException(e.getMessage());
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
            .instanceId(instanceId)
            .ip(ip)
            .provider(provider)
            .state(EmulatedCloudConstants.Plugins.STATE_READY)
            .build();

        return emulatedPublicIp;
    }

    private void deleteSecurityGroup(String securityGroupId) {
        String securityGroupPath = EmulatedCloudUtils.getResourcePath(this.properties, securityGroupId);
        EmulatedCloudUtils.deleteFile(securityGroupPath);
    }

    private void deleteSecurityRules(String securityGroupId) throws FogbowException {
        EmulatedSecurityGroup securityGroup = this.getSecurityGroup(securityGroupId);

        for (String securityRuleId : securityGroup.getSecurityRules()) {
            String path = EmulatedCloudUtils.getResourcePath(properties, securityRuleId);
            EmulatedCloudUtils.deleteFile(path);
        }
    }

    private EmulatedSecurityGroup getSecurityGroup(String securityGroupId) throws FogbowException {
        try {
            String content = EmulatedCloudUtils.getFileContentById(properties, securityGroupId);
            return EmulatedSecurityGroup.fromJson(content);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
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
