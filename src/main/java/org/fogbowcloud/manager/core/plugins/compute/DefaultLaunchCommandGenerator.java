package org.fogbowcloud.manager.core.plugins.compute;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;

public class DefaultLaunchCommandGenerator implements LaunchCommandGenerator {

	private final String TOKEN_ID = "#TOKEN_ID#";
	private final String TOKEN_HOST = "#TOKEN_HOST#";
	private final String TOKEN_HOST_HTTP_PORT = "#TOKEN_HOST_HTTP_PORT#";
	private final String TOKEN_HOST_SSH_PORT = "#TOKEN_HOST_SSH_PORT#";
	private final String TOKEN_SSH_USER = "#TOKEN_SSH_USER#";
	private final String TOKEN_USER_SSH_PUBLIC_KEY = "#TOKEN_USER_SSH_PUBLIC_KEY#";
	private final String TOKEN_MANAGER_SSH_PUBLIC_KEY = "#TOKEN_MANAGER_SSH_PUBLIC_KEY#";

	private final String DEFAULT_SSH_HOST_PORT = "22";

	public static final String USER_DATA_LINE_BREAKER = "[[\\n]]";

	private final String sshReverseTunnelScriptPath = "bin/fogbow-create-reverse-tunnel";
	private final String cloudConfigFilePath = "bin/fogbow-cloud-config.cfg";

	private final String sshCommonUser;
	private final String managerSshPublicKeyPath;

	private final String reverseTunnelPrivateIP;
	private final String reverseTunnelPort;
	private final String reverseTunnelHttpPort;

	public DefaultLaunchCommandGenerator(Properties properties) {
		this.managerSshPublicKeyPath = properties.getProperty(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
		this.sshCommonUser = properties.getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY,
				DefaultConfigurationConstants.SSH_COMMON_USER_KEY);
		this.reverseTunnelPrivateIP = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
		this.reverseTunnelPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PORT_KEY,
				this.DEFAULT_SSH_HOST_PORT);
		this.reverseTunnelHttpPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
	}

	@Override
	public String createLaunchCommand(ComputeOrder order) {
		String tokenId = order.getId();
		String sshPrivateHostIP = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
		String sshRemoteHostPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PORT_KEY);
		String sshRemoteHostHttpPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
		String managerPublicKeyFilePath = getManagerSSHPublicKeyFilePath(properties);
		String userPublicKey = order.getPublicKey();
		String sshCommonUser = getSSHCommonUser(properties);
	}

}
