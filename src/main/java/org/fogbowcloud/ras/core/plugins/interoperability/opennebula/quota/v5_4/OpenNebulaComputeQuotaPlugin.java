package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.group.Group;
import org.opennebula.client.user.User;

public class OpenNebulaComputeQuotaPlugin implements ComputeQuotaPlugin<OpenNebulaToken> {

	private static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	private static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	private static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	private static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	private static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	private static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
	private static final String GROUPS_ID_PATH = "GROUPS/ID";
	private static final int DEFAULT_RESOURCE_MAX_VALUE = Integer.MAX_VALUE;
	private static final int VALUE_DEFAULT_QUOTA_OPENNEBULA = -1;
	private static final int VALUE_UNLIMITED_QUOTA_OPENNEBULA = -2;
	
	private OpenNebulaClientFactory factory;
	
	public OpenNebulaComputeQuotaPlugin() {
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public ComputeQuota getUserQuota(OpenNebulaToken localUserAttributes) throws FogbowRasException, UnexpectedException {
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());				
		
		User user = this.factory.createUser(client, localUserAttributes.getUserName());
		String maxCpuByUser = user.xpath(QUOTA_CPU_PATH);
		String maxMemoryByUser = user.xpath(QUOTA_MEMORY_PATH);
		String maxInstancesByUser = user.xpath(QUOTA_VMS_PATH);
		String cpuInUseByUser = user.xpath(QUOTA_CPU_USED_PATH);
		String memoryInUseByUser = user.xpath(QUOTA_MEMORY_USED_PATH);
		String instancesInUseByUser = user.xpath(QUOTA_VMS_USED_PATH);
		
		String groupId = user.xpath(GROUPS_ID_PATH);
		int id = Integer.parseInt(groupId);
		Group group = this.factory.createGroup(client, id);
		String maxCpuByGroup = group.xpath(QUOTA_CPU_PATH);
		String maxMemoryByGroup = group.xpath(QUOTA_MEMORY_PATH);
		String maxInstancesByGroup = group.xpath(QUOTA_VMS_PATH);
		String cpuInUseByGroup = group.xpath(QUOTA_CPU_USED_PATH);
		String memoryInUseByGroup = group.xpath(QUOTA_MEMORY_USED_PATH);
		String instancesInUseByGroup = group.xpath(QUOTA_VMS_USED_PATH);
		
		ResourceQuota resourceQuota = getQuota(maxCpuByUser, cpuInUseByUser, maxCpuByGroup, cpuInUseByGroup);
		int maxCpu = resourceQuota.getMaxResource();
		int cpuInUse = resourceQuota.getResourceInUse();
		
		resourceQuota = getQuota(maxMemoryByUser, memoryInUseByUser, maxMemoryByGroup, memoryInUseByGroup);
		int maxMemory = resourceQuota.getMaxResource();
		int memoryInUse = resourceQuota.getResourceInUse();
		
		resourceQuota = getQuota(maxInstancesByUser, instancesInUseByUser, maxInstancesByGroup, instancesInUseByGroup);
		int maxNumberInstances = resourceQuota.getMaxResource();
		int instancesInUse = resourceQuota.getResourceInUse();
		
		ComputeAllocation totalAllocation = new ComputeAllocation(maxCpu, maxMemory, maxNumberInstances);
		ComputeAllocation usedAllocation = new ComputeAllocation(cpuInUse, memoryInUse, instancesInUse);
		
		return new ComputeQuota(totalAllocation, usedAllocation);
	}

	private ResourceQuota getQuota(String maxUserResource, String resourceInUseByUser, String maxGroupResource, String resourceInUseByGroup) {
		if (isValidNumber(maxUserResource) && isValidNumber(maxGroupResource)) {
			if (isUnlimitedOrDefaultQuota(maxUserResource)){
				maxUserResource = String.valueOf(DEFAULT_RESOURCE_MAX_VALUE);
			}
			if (isUnlimitedOrDefaultQuota(maxGroupResource)){
				maxGroupResource = String.valueOf(DEFAULT_RESOURCE_MAX_VALUE);
			}
			if (isUserSmallerQuota(maxUserResource, maxGroupResource)) {
				return new ResourceQuota(maxUserResource, resourceInUseByUser);
			} else {
				return new ResourceQuota(maxGroupResource, resourceInUseByGroup);
			}
		} else if (isValidNumber(maxUserResource)) {
			if (isUnlimitedOrDefaultQuota(maxUserResource)){
				maxUserResource = String.valueOf(DEFAULT_RESOURCE_MAX_VALUE);
			}
			return new ResourceQuota(maxUserResource, resourceInUseByUser);
		} else if (isValidNumber(maxGroupResource)) {
			if (isUnlimitedOrDefaultQuota(maxGroupResource)){
				maxGroupResource = String.valueOf(DEFAULT_RESOURCE_MAX_VALUE);
			}
			return new ResourceQuota(maxGroupResource, resourceInUseByGroup);
		} else {
			String maxResource = String.valueOf(DEFAULT_RESOURCE_MAX_VALUE);
			String resourceInUse = String.valueOf(getBiggerValue(resourceInUseByUser, resourceInUseByGroup));
			return new ResourceQuota(maxResource, resourceInUse);
		}
	}

	private int getBiggerValue(String userResource, String groupResource) {
		int resourceValue = Math.max(Integer.parseInt(userResource), Integer.parseInt(groupResource));
		return resourceValue;
	}

	private boolean isUserSmallerQuota(String userResource, String groupResource) {
		int userResourceValue = Integer.parseInt(userResource);
		int groupResourceValue = Integer.parseInt(groupResource);
		if (userResourceValue < groupResourceValue) {
			return true;
		}
		return false;
	}

	private boolean isUnlimitedOrDefaultQuota(String resource) {
		int resourceValue = Integer.parseInt(resource);
		return resourceValue == VALUE_DEFAULT_QUOTA_OPENNEBULA || resourceValue == VALUE_UNLIMITED_QUOTA_OPENNEBULA;
	}

	private boolean isValidNumber(String number) {
		try {
			Integer.parseInt(number);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static class ResourceQuota {
		
		private int maxResource;
		private int resourceInUse;
		
		public ResourceQuota(String maxResource, String resourceInUse) {
			this.maxResource = Integer.parseInt(maxResource);
			this.resourceInUse = Integer.parseInt(resourceInUse);
		}
		
		public int getResourceInUse() {	
			return resourceInUse;
		}
		
		public int getMaxResource() {
			return maxResource;
		}
	}
	
}
