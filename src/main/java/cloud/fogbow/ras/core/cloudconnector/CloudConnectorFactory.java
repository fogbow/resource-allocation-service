package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CloudConnectorFactory {
    private static CloudConnectorFactory instance;
    private String localMemberId;
    private Map<String, LocalCloudConnector> cachedLocalCloudConnectors;

    private CloudConnectorFactory() {
        this.localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY);
        this.cachedLocalCloudConnectors = new ConcurrentHashMap<String, LocalCloudConnector>();
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
            if (cachedLocalCloudConnectors.containsKey(cloudName)) {
                cloudConnector = this.cachedLocalCloudConnectors.get(cloudName);
            } else {
                cloudConnector = new LocalCloudConnector(cloudName);
                this.cachedLocalCloudConnectors.put(cloudName, (LocalCloudConnector) cloudConnector);
            }
        } else {
            cloudConnector = new RemoteCloudConnector(memberId, cloudName);
        }

        return cloudConnector;
    }
}
