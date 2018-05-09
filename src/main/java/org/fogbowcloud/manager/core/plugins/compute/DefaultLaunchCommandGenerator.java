package org.fogbowcloud.manager.core.plugins.compute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.plugins.compute.util.CloudInitUserDataBuilder;
import org.fogbowcloud.manager.core.plugins.compute.util.CloudInitUserDataBuilder.FileType;

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
	private final String managerSshPublicKey;

	private final String reverseTunnelPrivateIP;
	private final String reverseTunnelSshPort;
	private final String reverseTunnelHttpPort;

	public DefaultLaunchCommandGenerator(Properties properties)
			throws PropertyNotSpecifiedException, FileNotFoundException, IOException {
		
		String managerSshPublicKeyFilePath = properties.getProperty(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
		this.managerSshPublicKey = IOUtils.toString(new FileInputStream(new File(managerSshPublicKeyFilePath)));

		this.sshCommonUser = properties.getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY,
				DefaultConfigurationConstants.SSH_COMMON_USER_KEY);
		this.reverseTunnelPrivateIP = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
		this.reverseTunnelSshPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PORT_KEY,
				this.DEFAULT_SSH_HOST_PORT);
		this.reverseTunnelHttpPort = properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);

		checkNecessaryAttributes();
	}

	private void checkNecessaryAttributes() throws PropertyNotSpecifiedException {
		if (this.reverseTunnelPrivateIP == null || this.reverseTunnelSshPort.trim().isEmpty()) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
		}
		if (this.managerSshPublicKey == null || this.managerSshPublicKey.trim().isEmpty()) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
		}
		if (this.reverseTunnelHttpPort == null || this.reverseTunnelHttpPort.trim().isEmpty()) {
			throw new PropertyNotSpecifiedException(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
		}
	}

	@Override
	public String createLaunchCommand(ComputeOrder order) {
		CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
		cloudInitUserDataBuilder.addShellScript(new FileReader(this.sshReverseTunnelScriptPath));
		cloudInitUserDataBuilder.addCloudConfig(new FileReader(new File(this.cloudConfigFilePath)));

		UserData userData = order.getUserData();

		String normalizedExtraUserData = null;
		String extraUserData = userData.getExtraUserDataFile();
		if (extraUserData != null) {
			normalizedExtraUserData = new String(Base64.decodeBase64(extraUserData));
		}
		String extraUserDataContentType = userData.getExtraUserDataFileType();

		addExtraUserData(cloudInitUserDataBuilder, normalizedExtraUserData, extraUserDataContentType);

		String mimeString = cloudInitUserDataBuilder.buildUserData();
		
		mimeString = applyReplacements(order, mimeString);
	}

	private void addExtraUserData(CloudInitUserDataBuilder cloudInitUserDataBuilder, String extraUserData,
			String extraUserDataContentType) {
		if (extraUserData != null || extraUserDataContentType != null) {
			String lineSeparator = "\n";
			String normalizedExtraUserData = extraUserData.replace(USER_DATA_LINE_BREAKER, lineSeparator);
			for (FileType fileType : CloudInitUserDataBuilder.FileType.values()) {
				String mimeType = fileType.getMimeType();
				if (mimeType.equals(extraUserDataContentType)) {
					cloudInitUserDataBuilder.addFile(fileType, new StringReader(normalizedExtraUserData));
					break;
				}
			}
		}
	}

	private String applyReplacements(ComputeOrder order, String mimeString) {
		String orderId = order.getId();

		mimeString.replace(this.TOKEN_ID, orderId);
		mimeString.replace(this.TOKEN_HOST, this.reverseTunnelPrivateIP);
		mimeString.replace(this.TOKEN_HOST_SSH_PORT, this.reverseTunnelSshPort);
		mimeString.replace(this.TOKEN_HOST_HTTP_PORT, this.reverseTunnelHttpPort);

		String userPublicKey = order.getPublicKey();
		if (userPublicKey == null) {
			userPublicKey = "";
		}

		mimeString.replace(this.TOKEN_MANAGER_SSH_PUBLIC_KEY, this.managerSshPublicKey);
		mimeString.replace(this.TOKEN_USER_SSH_PUBLIC_KEY, userPublicKey);
		mimeString.replace(this.TOKEN_SSH_USER, this.sshCommonUser);

		return mimeString;
	}

}
