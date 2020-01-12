package cloud.fogbow.ras.core.plugins.interoperability.aws.quota.v2;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

public class AwsV2QuotaPlugin implements QuotaPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsV2QuotaPlugin.class);
    
    public AwsV2QuotaPlugin(String confFilePath) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ResourceQuota getUserQuota(AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(Messages.Info.GETTING_QUOTA);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    }

}
