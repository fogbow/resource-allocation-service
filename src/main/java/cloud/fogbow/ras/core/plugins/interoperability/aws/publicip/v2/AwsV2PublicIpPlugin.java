package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;

import java.util.Properties;

public class AwsV2PublicIpPlugin implements PublicIpPlugin<AwsV2User> {

    private String region;

    public AwsV2PublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
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
