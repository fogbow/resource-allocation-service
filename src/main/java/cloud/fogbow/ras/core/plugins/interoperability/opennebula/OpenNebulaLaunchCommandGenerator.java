package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.util.CloudInitUserDataBuilder;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class OpenNebulaLaunchCommandGenerator implements LaunchCommandGenerator {
    private static final Logger LOGGER = Logger.getLogger(OpenNebulaLaunchCommandGenerator.class);

    private final String ONE_STARTUP_SCRIPT_FILE_PATH = "bin/one-startup-script.sh";
    private final String SCRIPT_FILE_DELIMETER = "\\A";

    public OpenNebulaLaunchCommandGenerator() { }

    @Override
    public String createLaunchCommand(ComputeOrder order) {
        List<UserData> userDataScripts = order.getUserData();
        StringBuilder userDataBuilder = new StringBuilder();
        Scanner scanner = null;

        try {
            scanner = new Scanner(new File(ONE_STARTUP_SCRIPT_FILE_PATH));
            userDataBuilder.append(scanner.useDelimiter(SCRIPT_FILE_DELIMETER).next());
        } catch (IOException e) {
            throw new FatalErrorException(e.getMessage());
        } finally {
            scanner.close();
        }

        if (userDataScripts != null) {
            for (UserData userDataScript : userDataScripts) {
                if (userDataScript != null) {
                    String normalizedExtraUserData = null;
                    String extraUserDataFileContent = userDataScript.getExtraUserDataFileContent();
                    // NOTE(pauloewerton): since ONe supports just one script for a single file type at VM creation,
                    // we're appending only the bash scripts to the default script in order to prevent errors
                    if (extraUserDataFileContent != null &&
                            userDataScript.getExtraUserDataFileType().equals(CloudInitUserDataBuilder.FileType.SHELL_SCRIPT)) {
                        normalizedExtraUserData = new String(Base64.decodeBase64(extraUserDataFileContent));
                        userDataBuilder.append("\n");
                        userDataBuilder.append(normalizedExtraUserData);
                    } else {
                        LOGGER.warn(Messages.Warn.UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL);
                    }
                }
            }
        }

        String base64String = new String(Base64.encodeBase64(userDataBuilder.toString().getBytes(StandardCharsets.UTF_8),
                false, false), StandardCharsets.UTF_8);

        return base64String;
    }
}
