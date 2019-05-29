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

    public AwsV2NetworkPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public boolean isReady(String instanceState) {
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return true;
    }
}
