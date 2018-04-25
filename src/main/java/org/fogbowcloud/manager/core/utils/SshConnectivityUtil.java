package org.fogbowcloud.manager.core.utils;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.CommonConfigurationConstants;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.threads.AttendSpawningOrdersThread;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

public class SshConnectivityUtil {

	private static final Logger LOGGER = Logger.getLogger(AttendSpawningOrdersThread.class);

	private static Properties properties; // TODO create class Properties, currently using java.util.Properties...
	private SshClientPoolUtil sshClientPool = new SshClientPoolUtil();

	private boolean activeConnection;

	public SshConnectivityUtil(ComputeOrderInstance computeOrderInstance) {
		this.activeConnection = this.checkSSHConnectivity(computeOrderInstance);
	}

	public boolean isActiveConnection() {
		return activeConnection;
	}

	public void setActiveConnection(boolean activeConnection) {
		this.activeConnection = activeConnection;
	}

	private boolean checkSSHConnectivity(ComputeOrderInstance computeOrderInstance) {
		if (computeOrderInstance == null || computeOrderInstance.getTunnelingPorts() == null || computeOrderInstance
				.getTunnelingPorts().get(CommonConfigurationConstants.SSH_PUBLIC_ADDRESS_ATT) == null) {
			return false;
		}
		try {
			Command sshOutput = execOnInstance(
					computeOrderInstance.getTunnelingPorts().get(CommonConfigurationConstants.SSH_PUBLIC_ADDRESS_ATT),
					"echo HelloWorld");
			if (sshOutput.getExitStatus() == 0) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.debug("Check for SSH connectivity failed.", e);
		}
		return false;
	}

	private Command execOnInstance(String sshPublicAddress, String cmd) throws Exception {
		SSHClient sshClient = sshClientPool.getClient(sshPublicAddress, SshCommonUserUtil.getSshCommonUser(),
				getManagerSSHPrivateKeyFilePath());
		Session session = sshClient.startSession();
		Command command = session.exec(cmd);
		command.join();
		return command;
	}

	private String getManagerSSHPrivateKeyFilePath() {
		String publicKeyFilePath = properties.getProperty(ConfigurationConstants.SSH_PRIVATE_PATH_KEY);
		if (publicKeyFilePath == null || publicKeyFilePath.isEmpty()) {
			return null;
		}
		return publicKeyFilePath;
	}

}
