package org.fogbowcloud.manager.core.statisticsprovider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.manager.plugins.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;

public class LocalStatisticsProvider implements StatisticsProvider {
	
	private ComputeQuotaPlugin computeQuotaPlugin;
	private SharedOrderHolders orderHolders;
	private final LocalInstanceProvider localInstanceProvider;
	
	public LocalStatisticsProvider(ComputeQuotaPlugin computeQuotaPlugin, SharedOrderHolders 
			orderHolders, LocalInstanceProvider localInstanceProvider) {
		this.computeQuotaPlugin = computeQuotaPlugin;
		this.orderHolders = orderHolders;
		this.localInstanceProvider = localInstanceProvider;
	}
	
	@Override
	public ComputeQuota getSharedQuota() {
		return this.computeQuotaPlugin.getQuota();
	}

	@Override
	public ComputeQuota getUsedQuota() throws QuotaException {
		Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();
        
		List<Order> computeOrders = orders.stream().filter(order -> order.getType().equals(OrderType.COMPUTE))
				.collect(Collectors.toList());

		for (Order order : computeOrders) {
			try {
				ComputeInstance computeInstance = (ComputeInstance) localInstanceProvider.getInstance(order);
			} catch (RequestException | TokenCreationException | UnauthorizedException | PropertyNotSpecifiedException
					| InstanceNotFoundException e) {
				
				throw new QuotaException("Error while getting order instance", e);
			}
		}
	}

	@Override
	public ComputeQuota getInUseQuota() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private getAllReadyComputeInstances() {
		
	}
	
}
