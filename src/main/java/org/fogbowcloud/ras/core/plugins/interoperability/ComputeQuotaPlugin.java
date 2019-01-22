package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface ComputeQuotaPlugin<T extends Token> {

    public ComputeQuota getUserQuota(T localUserAttributes) throws FogbowRasException, UnexpectedException;
}
