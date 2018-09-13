package org.fogbowcloud.ras.core.plugins.interoperability.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;

public class DefaultLaunchCommandGenerator implements LaunchCommandGenerator {
    private static final Logger LOGGER = Logger.getLogger(DefaultLaunchCommandGenerator.class);

    protected static final String TOKEN_ID = "#TOKEN_ID#";
    protected static final String TOKEN_SSH_USER = "#TOKEN_SSH_USER#";
    protected static final String TOKEN_USER_SSH_PUBLIC_KEY = "#TOKEN_USER_SSH_PUBLIC_KEY#";
    public static final String USER_DATA_LINE_BREAKER = "[[\\n]]";
    private final String BRING_UP_NETWORK_INTERFACE_SCRIPT_PATH = "bin/bring-up-network-interface";
    private final String CLOUD_CONFIG_FILE_PATH = "bin/cloud-config.cfg";
    private final String sshCommonUser;

    public DefaultLaunchCommandGenerator() {
        this.sshCommonUser = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY,
                DefaultConfigurationConstants.SSH_COMMON_USER);
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
            LOGGER.warn(Messages.Warn.NOT_POSSIBLE_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL);
        } else {
            LOGGER.warn(Messages.Warn.NOT_POSSIBLE_ADD_EXTRA_USER_DATA_FILE_TYPE_NULL);
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

        replacements.put(TOKEN_USER_SSH_PUBLIC_KEY, userPublicKey);
        replacements.put(TOKEN_SSH_USER, this.sshCommonUser);

        for (String key : replacements.keySet()) {
            String value = replacements.get(key);
            mimeString = mimeString.replace(key, value);
        }
        return mimeString;
    }
}
