package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputeQuotaPlugin;

public class OpenNebulaComputeQuotaPlugin implements ComputeQuotaPlugin {

	@Override
	public ComputeQuota getUserQuota(Token token) throws FogbowRasException, UnexpectedException {
		// TODO Auto-generated method stub
		return null;
	}

}
