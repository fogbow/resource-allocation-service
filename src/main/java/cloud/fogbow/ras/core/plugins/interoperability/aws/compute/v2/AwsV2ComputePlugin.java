package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;

import java.util.Properties;

public class AwsV2ComputePlugin implements ComputePlugin<AwsV2User> {

    private String region;
    private Properties properties;

    public AwsV2ComputePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new FogbowException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new FogbowException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new FogbowException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return true;
    }

}
