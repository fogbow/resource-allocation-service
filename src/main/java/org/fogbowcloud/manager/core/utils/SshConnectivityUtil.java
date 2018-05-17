package org.fogbowcloud.manager.core.utils;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.threads.SpawningProcessor;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

public class SshConnectivityUtil {

	private static final int SUCCESSFUL_COMMAND_STATUS = 0;

	private static final String MESSAGE_ECHO_SEND = "echo HelloWorld";

	private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

	private static Properties properties;

	private SshClientPoolUtil sshClientPool = new SshClientPoolUtil();

	private static SshConnectivityUtil instance;
	
	private SshConnectivityUtil() {}

	public static SshConnectivityUtil getInstance() {
		if (instance == null) {
			instance = new SshConnectivityUtil();
		}
		return instance;
	}

	public boolean checkSSHConnectivity(ComputeOrderInstance computeOrderInstance) {
		if (computeOrderInstance == null || computeOrderInstance.getSshPublicAddress() == null) {
			return false;
		}
		try {
			Command sshOutput = execOnInstance(computeOrderInstance.getSshPublicAddress(), MESSAGE_ECHO_SEND);
			if (sshOutput.getExitStatus() == SUCCESSFUL_COMMAND_STATUS) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.debug("Check for SSH connectivity failed.", e);
		}
		return false;
	}

	private Command execOnInstance(String sshPublicAddress, String cmd) throws Exception {
		SSHClient sshClient = sshClientPool.getClient(sshPublicAddress, SshCommonUserUtil.getSshCommonUser(),
				getManagerSSHPrivateKey());
		Session session = sshClient.startSession();
		Command command = session.exec(cmd);
		command.join();
		return command;
	}

	private String getManagerSSHPrivateKey() {
		String privateKey = properties.getProperty(ConfigurationConstants.MANAGER_SSH_PRIVATE_KEY_PATH);
		if (privateKey == null || privateKey.isEmpty()) {
			return null;
		}
		return privateKey;
	}

}
