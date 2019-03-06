package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;

public interface ComputeQuotaPlugin<T extends CloudUser> {

    public ComputeQuota getUserQuota(T cloudUser) throws FogbowException;
}
