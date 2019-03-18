package cloud.fogbow.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.group.Group;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;

public class OpenNebulaComputeQuotaPlugin implements ComputeQuotaPlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputeQuotaPlugin.class);
	
	private static final String GROUPS_ID_PATH = "GROUPS/ID";
	private static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	private static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	private static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	private static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	private static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	private static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
	
	private static final int DEFAULT_RESOURCE_MAX_VALUE = Integer.MAX_VALUE;
	
	private String endpoint;

	public OpenNebulaComputeQuotaPlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
	}
	
	@Override
	public ComputeQuota getUserQuota(CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		UserPool userPool = OpenNebulaClientUtil.getUserPool(client);
		User user = OpenNebulaClientUtil.getUser(userPool, cloudUser.getId());
		String maxCpuByUser = user.xpath(QUOTA_CPU_PATH);
		String maxMemoryByUser = user.xpath(QUOTA_MEMORY_PATH);
		String maxInstancesByUser = user.xpath(QUOTA_VMS_PATH);
		String cpuInUseByUser = user.xpath(QUOTA_CPU_USED_PATH);
		String memoryInUseByUser = user.xpath(QUOTA_MEMORY_USED_PATH);
		String instancesInUseByUser = user.xpath(QUOTA_VMS_USED_PATH);

		String groupId = user.xpath(GROUPS_ID_PATH);
		int id;
		try {
			id = Integer.parseInt(groupId);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e));
			throw new UnexpectedException();
		}

		Group group = OpenNebulaClientUtil.getGroup(client, id);
		String maxCpuByGroup = group.xpath(QUOTA_CPU_PATH);
		String maxMemoryByGroup = group.xpath(QUOTA_MEMORY_PATH);
		String maxInstancesByGroup = group.xpath(QUOTA_VMS_PATH);
		String cpuInUseByGroup = group.xpath(QUOTA_CPU_USED_PATH);
		String memoryInUseByGroup = group.xpath(QUOTA_MEMORY_USED_PATH);
		String instancesInUseByGroup = group.xpath(QUOTA_VMS_USED_PATH);

		ResourceQuota cpuQuota = getQuota(maxCpuByUser, cpuInUseByUser, maxCpuByGroup, cpuInUseByGroup);
		ResourceQuota memoryQuota = getQuota(maxMemoryByUser, memoryInUseByUser, maxMemoryByGroup, memoryInUseByGroup);
		ResourceQuota vmsQuota = getQuota(maxInstancesByUser, instancesInUseByUser, maxInstancesByGroup,
				instancesInUseByGroup);

		ComputeAllocation totalAllocation = new ComputeAllocation(cpuQuota.getMaxResource(),
				memoryQuota.getMaxResource(), vmsQuota.getMaxResource());

		ComputeAllocation usedAllocation = new ComputeAllocation(cpuQuota.getResourceInUse(),
				memoryQuota.getResourceInUse(), vmsQuota.getResourceInUse());

		ComputeQuota computeQuota = new ComputeQuota(totalAllocation, usedAllocation);
		return computeQuota;
	}

	private ResourceQuota getQuota(String maxUserResource, String resourceInUseByUser, String maxGroupResource, String resourceInUseByGroup) {
		if (isValidNumber(maxUserResource) && isValidNumber(maxGroupResource)) {
			if (isUserSmallerQuota(maxUserResource, maxGroupResource)) {
				return getResouceQuota(maxUserResource, resourceInUseByUser);
			} else {
				return getResouceQuota(maxGroupResource, resourceInUseByGroup);
			}
		} 
		String maxResource = String.valueOf(DEFAULT_RESOURCE_MAX_VALUE);
		String resourceInUse = String.valueOf(getBiggerValue(resourceInUseByUser, resourceInUseByGroup));
		return getResouceQuota(maxResource, resourceInUse);
	}

	private ResourceQuota getResouceQuota(String maxResource, String resourceInUse) {
		int maxResourceValue = parseToInteger(maxResource);
		int resourceInUseValue = parseToInteger(resourceInUse);
		return new ResourceQuota(maxResourceValue, resourceInUseValue);
	}

	private int getBiggerValue(String userResource, String groupResource) {
		int userValue = parseToInteger(userResource);
		int groupValue = parseToInteger(groupResource);
		int resourceValue = Math.max(userValue, groupValue);
		return resourceValue;
	}
	
	private int parseToInteger(String number) {
		if (isValidNumber(number)) {
			return Integer.parseInt(number);
		}
		return 0;
	}
	
	private boolean isUserSmallerQuota(String userResource, String groupResource) {
		int userResourceValue = parseToInteger(userResource);
		int groupResourceValue = parseToInteger(groupResource);
		if (userResourceValue < groupResourceValue) {
			return true;
		}
		return false;
	}

	private boolean isValidNumber(String number) {
		try {
			Integer.parseInt(number);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static class ResourceQuota {
		
		private Integer maxResource;
		private Integer resourceInUse;
		
		public ResourceQuota(int maxResource, int resourceInUse) {
			this.maxResource = maxResource;
			this.resourceInUse = resourceInUse;
		}
		
		public Integer getResourceInUse() {	
			return resourceInUse;
		}
		
		public Integer getMaxResource() {
			return maxResource;
		}
	}
	
}
