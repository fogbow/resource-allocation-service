package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;

import java.util.Properties;

public class AwsV2NetworkPlugin implements NetworkPlugin<AwsV2User> {

    private String region;
    private Properties properties;

    public AwsV2NetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    public String requestInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        return null;
    }

    public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        return null;
    }

    public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {

    }

    public boolean isReady(String instanceState) {
        return true;
    }

    public boolean hasFailed(String instanceState) {
        return true;
    }
}
