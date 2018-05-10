package org.fogbowcloud.manager.core.plugins.compute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.plugins.compute.util.CloudInitUserDataBuilder;

public class DefaultLaunchCommandGenerator implements LaunchCommandGenerator {

	protected static final String TOKEN_ID = "#TOKEN_ID#";
	protected static final String TOKEN_HOST = "#TOKEN_HOST#";
	protected static final String TOKEN_HOST_HTTP_PORT = "#TOKEN_HOST_HTTP_PORT#";
	protected static final String TOKEN_HOST_SSH_PORT = "#TOKEN_HOST_SSH_PORT#";
	protected static final String TOKEN_SSH_USER = "#TOKEN_SSH_USER#";
	protected static final String TOKEN_USER_SSH_PUBLIC_KEY = "#TOKEN_USER_SSH_PUBLIC_KEY#";
	protected static final String TOKEN_MANAGER_SSH_PUBLIC_KEY = "#TOKEN_MANAGER_SSH_PUBLIC_KEY#";

	protected static final String DEFAULT_SSH_HOST_PORT = "22";

	public static final String USER_DATA_LINE_BREAKER = "[[\\n]]";

	private final String SSH_REVERSE_TUNNEL_SCRIPT_PATH = "bin/fogbow-create-reverse-tunnel";
	private final FileReader sshReverseTunnelScript;

	private final String CLOUD_CONFIG_FILE_PATH = "bin/fogbow-cloud-config.cfg";
	private final FileReader cloudConfigFile;

	private final String sshCommonUser;
	private final String managerSshPublicKey;

	private final String reverseTunnelPrivateIP;
	private final String reverseTunnelSshPort;
	private final String reverseTunnelHttpPort;

	private static final Logger LOGGER = Logger.getLogger(DefaultLaunchCommandGenerator.class);

	public DefaultLaunchCommandGenerator(Properties properties) throws PropertyNotSpecifiedException, IOException {
		this.cloudConfigFile = new FileReader(new File(this.CLOUD_CONFIG_FILE_PATH));

		this.sshCommonUser = properties.getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY,
				DefaultConfigurationConstants.SSH_COMMON_USER);

		String managerSshPublicKeyFilePath = properties.getProperty(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
		if (managerSshPublicKeyFilePath == null) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
		}
		this.managerSshPublicKey = IOUtils.toString(new FileInputStream(new File(managerSshPublicKeyFilePath)));

		this.sshReverseTunnelScript = new FileReader(this.SSH_REVERSE_TUNNEL_SCRIPT_PATH);

		this.reverseTunnelPrivateIP = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);

		this.reverseTunnelSshPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PORT_KEY,
				DefaultLaunchCommandGenerator.DEFAULT_SSH_HOST_PORT);

		this.reverseTunnelHttpPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);

		checkNecessaryAttributes();
	}

	private void checkNecessaryAttributes() throws PropertyNotSpecifiedException {
		if (this.reverseTunnelPrivateIP == null || this.reverseTunnelPrivateIP.trim().isEmpty()) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
		}
		if (this.managerSshPublicKey.trim().isEmpty()) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
		}
		if (this.reverseTunnelHttpPort == null || this.reverseTunnelHttpPort.trim().isEmpty()) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
		}
	}

	@Override
	public String createLaunchCommand(ComputeOrder order) {
		CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
		cloudInitUserDataBuilder.addShellScript(this.sshReverseTunnelScript);
		cloudInitUserDataBuilder.addCloudConfig(this.cloudConfigFile);

		UserData userData = order.getUserData();

		String normalizedExtraUserData = null;
		String extraUserData = userData.getExtraUserDataFileContent();
		if (extraUserData != null) {
			normalizedExtraUserData = new String(Base64.decodeBase64(extraUserData));
		}
		CloudInitUserDataBuilder.FileType extraUserDataContentType = userData.getExtraUserDataFileType();

		addExtraUserData(cloudInitUserDataBuilder, normalizedExtraUserData, extraUserDataContentType);

		String mimeString = cloudInitUserDataBuilder.buildUserData();

		mimeString = applyTokensReplacements(order, mimeString);

		String base64String = new String(Base64.encodeBase64(mimeString.getBytes(StandardCharsets.UTF_8), false, false),
				StandardCharsets.UTF_8);
		return base64String;
	}

	protected void addExtraUserData(CloudInitUserDataBuilder cloudInitUserDataBuilder, String extraUserData,
			CloudInitUserDataBuilder.FileType extraUserDataContentType) {

		if (extraUserData != null && extraUserDataContentType != null) {
			String lineSeparator = "\n";
			String normalizedExtraUserData = extraUserData.replace(DefaultLaunchCommandGenerator.USER_DATA_LINE_BREAKER,
					lineSeparator);

			cloudInitUserDataBuilder.addFile(extraUserDataContentType, new StringReader(normalizedExtraUserData));
		} else {
			LOGGER.info(
					"Was not possible add the extra user data file, the extra user data file or file type are null");
		}
	}

	protected String applyTokensReplacements(ComputeOrder order, String mimeString) {
		String orderId = order.getId();

		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_ID, orderId);
		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_HOST, this.reverseTunnelPrivateIP);
		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_HOST_SSH_PORT, this.reverseTunnelSshPort);
		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_HOST_HTTP_PORT, this.reverseTunnelHttpPort);

		String userPublicKey = order.getPublicKey();
		if (userPublicKey == null) {
			userPublicKey = "";
		}

		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_MANAGER_SSH_PUBLIC_KEY,
				this.managerSshPublicKey);
		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_USER_SSH_PUBLIC_KEY, userPublicKey);
		mimeString = mimeString.replace(DefaultLaunchCommandGenerator.TOKEN_SSH_USER, this.sshCommonUser);

		return mimeString;
	}

}
