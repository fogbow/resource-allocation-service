package cloud.fogbow.ras.core.plugins.interoperability.aws.quota.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;

import java.util.Properties;

public class AwsV2ComputeQuotaPlugin implements ComputeQuotaPlugin<AwsV2User> {

    private String region;

    public AwsV2ComputeQuotaPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public ComputeQuota getUserQuota(AwsV2User cloudUser) throws FogbowException {
        throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }
}
