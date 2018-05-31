package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.manager.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;

public class LocalQuotaProvider implements QuotaProvider {
	
	private ComputeQuotaPlugin computeQuotaPlugin;
	private AAAController aaaController;
	
	public LocalQuotaProvider(
			ComputeQuotaPlugin computeQuotaPlugin, 
			AAAController aaaController) {
		this.computeQuotaPlugin = computeQuotaPlugin;
		this.aaaController = aaaController;
	}
	
	@Override
	public ComputeQuota getComputeQuota(FederationUser federationUser) throws QuotaException {
		try {
			Token localToken = this.aaaController.getLocalToken(federationUser);
			return this.computeQuotaPlugin.getComputeQuota(localToken);
		} catch (QuotaException | PropertyNotSpecifiedException | UnauthorizedException | TokenCreationException e) {
			throw new QuotaException("Error while getting shared quota", e);
		}
	}

}
