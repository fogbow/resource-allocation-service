package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

public class OpenStackQuotaPlugin implements QuotaPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackQuotaPlugin.class);
    
    public OpenStackQuotaPlugin(String confFilePath) {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public ResourceQuota getUserQuota(OpenStackV3User cloudUser) throws FogbowException {
        LOGGER.info(Messages.Info.GETTING_QUOTA);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    }

}
