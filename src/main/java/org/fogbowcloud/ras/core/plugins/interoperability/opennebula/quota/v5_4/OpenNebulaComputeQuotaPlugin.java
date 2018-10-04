package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.group.Group;
import org.opennebula.client.user.User;

public class OpenNebulaComputeQuotaPlugin implements ComputeQuotaPlugin {

	private static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	private static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	private static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	private static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	private static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	private static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
	private static final String GROUPS_ID_PATH = "GROUPS/ID";
	
	private OpenNebulaClientFactory factory;
	
	@Override
	public ComputeQuota getUserQuota(Token token) throws FogbowRasException, UnexpectedException {
		Client client = this.factory.createClient(token.getTokenValue());				
		
		User user = this.factory.createUser(client, token.getTokenValue()); // FIXME token.getUser().getName()...
		String maxCpuByUser = user.xpath(QUOTA_CPU_PATH);
		String maxMemoryByUser = user.xpath(QUOTA_MEMORY_PATH);
		String maxVMsByUser = user.xpath(QUOTA_VMS_PATH);
		String cpuInUseByUser = user.xpath(QUOTA_CPU_USED_PATH);
		String memoryInUseByUser = user.xpath(QUOTA_MEMORY_USED_PATH);
		String vmsInUseByUser = user.xpath(QUOTA_VMS_USED_PATH);
		
		String groupId = user.xpath(GROUPS_ID_PATH);
		int id = Integer.parseInt(groupId);
		Group group = this.factory.createGroup(client, id);
		String maxCpuByGroup = group.xpath(QUOTA_CPU_PATH);
		String maxMemoryByGroup = group.xpath(QUOTA_MEMORY_PATH);
		String maxVMsByGroup = group.xpath(QUOTA_VMS_PATH);
		String cpuInUseByGroup = group.xpath(QUOTA_CPU_USED_PATH);
		String memoryInUseByGroup = group.xpath(QUOTA_MEMORY_USED_PATH);
		String vmsInUseByGroup = group.xpath(QUOTA_VMS_USED_PATH);
		
		ResourceQuota resourceQuota = getQuota(maxCpuByUser, cpuInUseByUser, maxCpuByGroup, cpuInUseByGroup);
		double maxCpu = resourceQuota.getMax();
		double cpuInUse = resourceQuota.getInUse();
		
		// TODO Auto-generated method stub
		return null;
	}

	private ResourceQuota getQuota(String maxUserResource, String cpuInUseByUser, String maxGroupResource, String cpuInUseByGroup) {
		if (isValidNumber(maxUserResource) && isValidNumber(maxGroupResource)) {
			
		}
		// TODO Auto-generated method stub
		return null;
	}

	private boolean isValidNumber(String maxUserResource) {
		// TODO Auto-generated method stub
		return false;
	}

	private static class ResourceQuota {
		
		double maxResource;
		double resourceInUse;
		
		public ResourceQuota(String maxResource, String resourceInUse) {
			this.maxResource = Double.parseDouble(maxResource);
			this.resourceInUse = Double.parseDouble(resourceInUse);
		}
		
		public double getInUse() {	
			return resourceInUse;
		}
		
		public double getMax() {
			return maxResource;
		}
	}
	
}
