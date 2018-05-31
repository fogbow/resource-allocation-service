package org.fogbowcloud.manager.core.manager.plugins.cloud.compute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.manager.plugins.cloud.compute.util.CloudInitUserDataBuilder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;

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

    private final String SSH_REVERSE_TUNNEL_SCRIPT_PATH = "bin/create-reverse-tunnel";
    private final FileReader sshReverseTunnelScript;

    private final String CLOUD_CONFIG_FILE_PATH = "bin/cloud-config.cfg";
    private final FileReader cloudConfigFile;

    private final String sshCommonUser;
    private final String managerSshPublicKey;

    private final String reverseTunnelPrivateIP;
    private final String reverseTunnelSshPort;
    private final String reverseTunnelHttpPort;

    private static final Logger LOGGER = Logger.getLogger(DefaultLaunchCommandGenerator.class);

    public DefaultLaunchCommandGenerator(Properties properties)
            throws PropertyNotSpecifiedException, IOException {
        this.cloudConfigFile = new FileReader(new File(this.CLOUD_CONFIG_FILE_PATH));

        this.sshCommonUser =
                properties.getProperty(
                        ConfigurationConstants.SSH_COMMON_USER_KEY,
                        DefaultConfigurationConstants.SSH_COMMON_USER);

        String managerSshPublicKeyFilePath =
                properties.getProperty(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);
        checkPropertyNotEmpty(
                managerSshPublicKeyFilePath, ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);

        this.managerSshPublicKey =
                IOUtils.toString(new FileInputStream(new File(managerSshPublicKeyFilePath)));
        checkPropertyNotEmpty(
                this.managerSshPublicKey, ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_PATH);

        this.sshReverseTunnelScript = new FileReader(this.SSH_REVERSE_TUNNEL_SCRIPT_PATH);

        this.reverseTunnelPrivateIP =
                properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
        checkPropertyNotEmpty(
                this.reverseTunnelPrivateIP,
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);

        this.reverseTunnelSshPort =
                properties.getProperty(
                        ConfigurationConstants.REVERSE_TUNNEL_PORT_KEY, DEFAULT_SSH_HOST_PORT);

        this.reverseTunnelHttpPort =
                properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
        checkPropertyNotEmpty(
                this.reverseTunnelHttpPort, ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
    }

    private void checkPropertyNotEmpty(String property, String propertyKey)
            throws PropertyNotSpecifiedException {
        if (property == null || property.trim().isEmpty()) {
            throw new PropertyNotSpecifiedException(propertyKey);
        }
    }

    @Override
    public String createLaunchCommand(ComputeOrder order) {
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
        cloudInitUserDataBuilder.addShellScript(this.sshReverseTunnelScript);
        cloudInitUserDataBuilder.addCloudConfig(this.cloudConfigFile);

        UserData userData = order.getUserData();

        String normalizedExtraUserData = null;
        String extraUserDataFileContent = userData.getExtraUserDataFileContent();
        if (extraUserDataFileContent != null) {
            normalizedExtraUserData = new String(Base64.decodeBase64(extraUserDataFileContent));
        }
        CloudInitUserDataBuilder.FileType extraUserDataFileType =
                userData.getExtraUserDataFileType();

        addExtraUserData(cloudInitUserDataBuilder, normalizedExtraUserData, extraUserDataFileType);

        String mimeString = cloudInitUserDataBuilder.buildUserData();

        mimeString = applyTokensReplacements(order, mimeString);

        String base64String =
                new String(
                        Base64.encodeBase64(
                                mimeString.getBytes(StandardCharsets.UTF_8), false, false),
                        StandardCharsets.UTF_8);
        return base64String;
    }

    protected void addExtraUserData(
            CloudInitUserDataBuilder cloudInitUserDataBuilder,
            String extraUserDataFileContent,
            CloudInitUserDataBuilder.FileType extraUserDataFileType) {

        if (extraUserDataFileContent != null && extraUserDataFileType != null) {
            String lineSeparator = "\n";
            String normalizedExtraUserData =
                    extraUserDataFileContent.replace(USER_DATA_LINE_BREAKER, lineSeparator);

            cloudInitUserDataBuilder.addFile(
                    extraUserDataFileType, new StringReader(normalizedExtraUserData));
        } else if (extraUserDataFileContent == null) {
            LOGGER.warn(
                    "It was not possible add the extra user data file, the extra user data file content is null");
        } else {
            LOGGER.warn(
                    "It was not possible add the extra user data file, the extra user data file type is null");
        }
    }

    protected String applyTokensReplacements(ComputeOrder order, String mimeString) {
        String orderId = order.getId();

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(TOKEN_ID, orderId);
        replacements.put(TOKEN_HOST, this.reverseTunnelPrivateIP);
        replacements.put(TOKEN_HOST_SSH_PORT, this.reverseTunnelSshPort);
        replacements.put(TOKEN_HOST_HTTP_PORT, this.reverseTunnelHttpPort);

        String userPublicKey = order.getPublicKey();
        if (userPublicKey == null) {
            userPublicKey = "";
        }

        replacements.put(TOKEN_MANAGER_SSH_PUBLIC_KEY, this.managerSshPublicKey);
        replacements.put(TOKEN_USER_SSH_PUBLIC_KEY, userPublicKey);
        replacements.put(TOKEN_SSH_USER, this.sshCommonUser);

        String messageTemplate = "Replacing %s with %s";
        for (String key : replacements.keySet()) {
            String value = replacements.get(key);
            LOGGER.debug(String.format(messageTemplate, key, value));
            mimeString = mimeString.replace(key, value);
        }
        return mimeString;
    }
}
