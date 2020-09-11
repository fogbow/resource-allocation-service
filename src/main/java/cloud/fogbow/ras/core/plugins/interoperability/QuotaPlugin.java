package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;

public interface QuotaPlugin<T extends CloudUser> {

    public ResourceQuota getUserQuota(T cloudUser) throws FogbowException;
}
