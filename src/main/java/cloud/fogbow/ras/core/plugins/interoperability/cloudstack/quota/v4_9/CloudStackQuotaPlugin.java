package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

public class CloudStackQuotaPlugin implements QuotaPlugin<CloudStackUser> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackQuotaPlugin.class);
    
    public CloudStackQuotaPlugin(String confFilePath) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ResourceQuota getUserQuota(CloudStackUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Info.GETTING_QUOTA);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    }

}
