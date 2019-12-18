package cloud.fogbow.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

public class OpenNebulaQuotaPlugin implements QuotaPlugin<OpenNebulaUser> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaQuotaPlugin.class);
    
    public OpenNebulaQuotaPlugin(String confFilePath) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ResourceQuota getUserQuota(OpenNebulaUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Info.GETTING_QUOTA);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("This feature has not been implemented yet.");
    }

}
