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

	protected static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	protected static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	protected static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	protected static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	protected static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	protected static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";

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

		ComputeAllocation totalAllocation = this.getTotalAllocation(user);
		ComputeAllocation usedAllocation = this.getUsedAllocation(user);

		return new ComputeQuota(totalAllocation, usedAllocation);
	}

	protected ComputeAllocation getTotalAllocation(User user) {
		int maxCpu = convertToInteger(user.xpath(QUOTA_CPU_PATH));
		int maxMemory = convertToInteger(user.xpath(QUOTA_MEMORY_PATH));
		int maxInstances = convertToInteger(user.xpath(QUOTA_VMS_PATH));

		return new ComputeAllocation(maxCpu, maxMemory, maxInstances);
	}

	protected ComputeAllocation getUsedAllocation(User user) {
		int cpuInUse = convertToInteger(user.xpath(QUOTA_CPU_USED_PATH));
		int memoryInUse = convertToInteger(user.xpath(QUOTA_MEMORY_USED_PATH));
		int instancesInUse = convertToInteger(user.xpath(QUOTA_VMS_USED_PATH));

		return new ComputeAllocation(cpuInUse, memoryInUse, instancesInUse);
	}

	protected int convertToInteger(String number) {
		int converted = 0;
		try {
			converted = (int) Math.round(Double.parseDouble(number));
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e));
		}

		return converted;
	}
}
