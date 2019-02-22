package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CloudConnectorFactory {
    private static CloudConnectorFactory instance;
    private String localMemberId;

    private CloudConnectorFactory() {
        this.localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
    }

    public static synchronized CloudConnectorFactory getInstance() {
        if (instance == null) {
            instance = new CloudConnectorFactory();
        }
        return instance;
    }

    public CloudConnector getCloudConnector(String memberId, String cloudName) {
        CloudConnector cloudConnector;
        if (memberId.equals(this.localMemberId)) {
            cloudConnector = new LocalCloudConnector(cloudName);
        } else {
            cloudConnector = new RemoteCloudConnector(memberId, cloudName);
        }
        return cloudConnector;
    }
}
