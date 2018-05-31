package org.fogbowcloud.manager.core.statisticsprovider;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.manager.plugins.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.services.AAAController;

public class LocalStatisticsProvider implements StatisticsProvider {
	
	private ComputeQuotaPlugin computeQuotaPlugin;
	private SharedOrderHolders orderHolders;
	private final LocalInstanceProvider localInstanceProvider;
	private final String localMemberId;
	private AAAController aaaController;
	
	public LocalStatisticsProvider(
			Properties properties,
			ComputeQuotaPlugin computeQuotaPlugin, 
			SharedOrderHolders orderHolders, 
			AAAController aaaController,
			LocalInstanceProvider localInstanceProvider) {
		this.computeQuotaPlugin = computeQuotaPlugin;
		this.orderHolders = orderHolders;
		this.localInstanceProvider = localInstanceProvider;
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
		this.aaaController = aaaController;
	}
	
	@Override
	public ComputeQuota getSharedQuota() throws QuotaException {
		try {
			return this.computeQuotaPlugin.getMaxQuota(this.aaaController.getLocalToken());
		} catch (QuotaException | PropertyNotSpecifiedException | UnauthorizedException | TokenCreationException e) {
			throw new QuotaException("Error while getting shared quota", e);
		}
	}

	@Override
	public ComputeQuota getUsedQuota() throws QuotaException {
		try {
			return this.computeQuotaPlugin.getQuotaUsed(this.aaaController.getLocalToken());
		} catch (PropertyNotSpecifiedException | UnauthorizedException | TokenCreationException e) {
			throw new QuotaException("Error while getting used quota", e);
		}
	}

	@Override
	public ComputeQuota getInUseQuota(FederationUser federationUser) throws QuotaException {
		Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();
        
		List<Order> computeOrders = orders.stream()
				.filter(order -> order.getType().equals(OrderType.COMPUTE))
				.filter(order -> order.isProviderLocal(this.localMemberId))
				.filter(order -> order.getFederationUser().equals(federationUser))
				.collect(Collectors.toList());
		
		int vCPU = 0, ram = 0, instances = 0;
		
		for (Order order : computeOrders) {
			try {
				ComputeInstance computeInstance = (ComputeInstance) localInstanceProvider.getInstance(order);
				vCPU += computeInstance.getvCPU();
				ram += computeInstance.getvCPU();
				instances++;
			} catch (RequestException | TokenCreationException | UnauthorizedException | PropertyNotSpecifiedException
					| InstanceNotFoundException e) {
				throw new QuotaException("Error while getting order instance", e);
			}
		}
		
		return new ComputeQuota(vCPU, ram, instances);
	}
	
}
