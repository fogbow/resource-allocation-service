package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;

public interface ComputeQuotaPlugin<T extends CloudToken> {

    public ComputeQuota getUserQuota(T localUserAttributes) throws FogbowException;
}
