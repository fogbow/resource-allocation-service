package org.fogbowcloud.ras.core.plugins.interoperability.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DefaultLaunchCommandGenerator implements LaunchCommandGenerator {
    private static final Logger LOGGER = Logger.getLogger(DefaultLaunchCommandGenerator.class);

    protected static final String TOKEN_ID = "#TOKEN_ID#";
    protected static final String TOKEN_SSH_USER = "#TOKEN_SSH_USER#";
    protected static final String TOKEN_USER_SSH_PUBLIC_KEY = "#TOKEN_USER_SSH_PUBLIC_KEY#";
    protected static final String TOKEN_RAS_SSH_PUBLIC_KEY = "#TOKEN_RAS_SSH_PUBLIC_KEY#";
    public static final String USER_DATA_LINE_BREAKER = "[[\\n]]";
    private final String BRING_UP_NETWORK_INTERFACE_SCRIPT_PATH = "bin/bring-up-network-interface";
    private final String CLOUD_CONFIG_FILE_PATH = "bin/cloud-config.cfg";
    private final String sshCommonUser;
    private final String rasSshPublicKey;

    public DefaultLaunchCommandGenerator() throws FatalErrorException {
        try {
            this.sshCommonUser = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY,
                    DefaultConfigurationConstants.SSH_COMMON_USER);

            String rasSshPublicKeyFilePath =
                    PropertiesHolder.getInstance().getProperty(ConfigurationConstants.RAS_SSH_PUBLIC_KEY_FILE_PATH);
            checkPropertyNotEmpty(rasSshPublicKeyFilePath, ConfigurationConstants.RAS_SSH_PUBLIC_KEY_FILE_PATH);

            this.rasSshPublicKey = IOUtils.toString(new FileInputStream(new File(rasSshPublicKeyFilePath)));
            checkPropertyNotEmpty(this.rasSshPublicKey, ConfigurationConstants.RAS_SSH_PUBLIC_KEY_FILE_PATH);

        } catch (IOException e) {
            throw new FatalErrorException(e.getMessage());
        }
    }

    private void checkPropertyNotEmpty(String property, String propertyKey) throws FatalErrorException {
        if (property == null || property.trim().isEmpty()) {
            String message = "Found end property" + propertyKey;
            throw new FatalErrorException(message);
        }
    }

    @Override
    public String createLaunchCommand(ComputeOrder order) {
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
        try {
            // Here, we need to instantiate the FileReader, because, once we read this file, the stream goes to the end
            // of the file, preventing to read the file again.
            cloudInitUserDataBuilder.addCloudConfig(new FileReader(this.CLOUD_CONFIG_FILE_PATH));
            if (order.getNetworksId().size() > 1) {
                cloudInitUserDataBuilder.addShellScript(new FileReader(this.BRING_UP_NETWORK_INTERFACE_SCRIPT_PATH));
            }
        } catch (IOException e) {
            throw new FatalErrorException(e.getMessage());
        }

        UserData userData = order.getUserData();

        if (userData != null) {
            String normalizedExtraUserData = null;
            String extraUserDataFileContent = userData.getExtraUserDataFileContent();
            if (extraUserDataFileContent != null) {
                normalizedExtraUserData = new String(Base64.decodeBase64(extraUserDataFileContent));
            }

            CloudInitUserDataBuilder.FileType extraUserDataFileType = userData.getExtraUserDataFileType();
            addExtraUserData(cloudInitUserDataBuilder, normalizedExtraUserData, extraUserDataFileType);
        }

        String mimeString = cloudInitUserDataBuilder.buildUserData();

        mimeString = applyTokensReplacements(order, mimeString);

        String base64String = new String(Base64.encodeBase64(mimeString.getBytes(StandardCharsets.UTF_8),
                false, false), StandardCharsets.UTF_8);
        return base64String;
    }

    protected void addExtraUserData(CloudInitUserDataBuilder cloudInitUserDataBuilder, String extraUserDataFileContent,
                                    CloudInitUserDataBuilder.FileType extraUserDataFileType) {

        if (extraUserDataFileContent != null && extraUserDataFileType != null) {
            String lineSeparator = "\n";
            String normalizedExtraUserData = extraUserDataFileContent.replace(USER_DATA_LINE_BREAKER, lineSeparator);

            cloudInitUserDataBuilder.addFile(extraUserDataFileType, new StringReader(normalizedExtraUserData));
        } else if (extraUserDataFileContent == null) {
            LOGGER.warn("It was not possible to add the extra user data file, whose content is null");
        } else {
            LOGGER.warn("It was not possible to add the extra user data file, the extra user data file type is null");
        }
    }

    protected String applyTokensReplacements(ComputeOrder order, String mimeString) {
        String orderId = order.getId();

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(TOKEN_ID, orderId);

        String userPublicKey = order.getPublicKey();
        if (userPublicKey == null) {
            userPublicKey = "";
        }

        replacements.put(TOKEN_RAS_SSH_PUBLIC_KEY, this.rasSshPublicKey);
        replacements.put(TOKEN_USER_SSH_PUBLIC_KEY, userPublicKey);
        replacements.put(TOKEN_SSH_USER, this.sshCommonUser);

        String messageTemplate = "Replacing %s with %s";
        for (String key : replacements.keySet()) {
            String value = replacements.get(key);
            mimeString = mimeString.replace(key, value);
        }
        return mimeString;
    }
}
