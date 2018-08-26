package org.fogbowcloud.ras.core.cloudconnector;

import org.fogbowcloud.ras.core.CloudPluginsHolder;
import org.fogbowcloud.ras.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

public class CloudConnectorFactory {
    private static CloudConnectorFactory instance;
    private String localMemberId;
    private FederationToLocalMapperPlugin mapperPlugin;
    private CloudPluginsHolder cloudPluginsHolder;

    public static synchronized CloudConnectorFactory getInstance() {
        if (instance == null) {
            instance = new CloudConnectorFactory();
        }
        return instance;
    }

    public CloudConnector getCloudConnector(String memberId) {
        CloudConnector cloudConnector;

        if (memberId.equals(this.localMemberId)) {
            cloudConnector = new LocalCloudConnector(this.mapperPlugin, this.cloudPluginsHolder);
        } else {
            cloudConnector = new RemoteCloudConnector(memberId);
        }

        return cloudConnector;
    }

    public void setMapperPlugin(FederationToLocalMapperPlugin mapperPlugin) {
        this.mapperPlugin = mapperPlugin;
    }

    public void setCloudPluginsHolder(CloudPluginsHolder cloudPluginsHolder) {
        this.cloudPluginsHolder = cloudPluginsHolder;
    }

    public void setLocalMemberId(String localMemberId) {
        this.localMemberId = localMemberId;
    }
}
