package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;

public class CloudConnectorFactory {
    private static CloudConnectorFactory instance;
    private String localProviderId;

    private CloudConnectorFactory() {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
    }

    public static synchronized CloudConnectorFactory getInstance() {
        if (instance == null) {
            instance = new CloudConnectorFactory();
        }
        return instance;
    }

    public CloudConnector getCloudConnector(String providerId, String cloudName) {
        CloudConnector cloudConnector;
        if (providerId.equals(this.localProviderId)) {
            cloudConnector = new LocalCloudConnector(cloudName);
        } else {
            cloudConnector = new RemoteCloudConnector(providerId, cloudName);
        }
        return cloudConnector;
    }
}
