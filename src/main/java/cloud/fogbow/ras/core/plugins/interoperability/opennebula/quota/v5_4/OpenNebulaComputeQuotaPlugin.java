package cloud.fogbow.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
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
	
	private static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	private static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	private static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	private static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	private static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	private static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
	
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
		int maxCpu = convertToInteger(user.xpath(QUOTA_CPU_PATH));
		int maxMemory = convertToInteger(user.xpath(QUOTA_MEMORY_PATH));
		int maxInstances = convertToInteger(user.xpath(QUOTA_VMS_PATH));
		int cpuInUse = convertToInteger(user.xpath(QUOTA_CPU_USED_PATH));
		int memoryInUse = convertToInteger(user.xpath(QUOTA_MEMORY_USED_PATH));
		int instancesInUse = convertToInteger(user.xpath(QUOTA_VMS_USED_PATH));

		ComputeAllocation totalAllocation = new ComputeAllocation(maxCpu, maxMemory, maxInstances);
		ComputeAllocation usedAllocation = new ComputeAllocation(cpuInUse, memoryInUse, instancesInUse);

		ComputeQuota computeQuota = new ComputeQuota(totalAllocation, usedAllocation);
		return computeQuota;
	}
	
	private int convertToInteger(String number) {
		if (isValidNumber(number)) {
			double value = Double.parseDouble(number);
			return (int) Math.round(value);
		}
		return 0;
	}

	private boolean isValidNumber(String number) {
		try {
			Double.parseDouble(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e));
			return false;
		}
		return true;
	}
	
}
