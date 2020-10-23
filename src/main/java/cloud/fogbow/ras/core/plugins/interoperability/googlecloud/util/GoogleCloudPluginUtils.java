package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;

public class GoogleCloudPluginUtils {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudPluginUtils.class);
    public static final String VOLUME_COMPUTE_URL_KEY = "googlecloud_compute_url";
    public static final String PROJECT_NAME_EXAMPLE = "diesel_talon";

    public static String getProjectIdFrom(GoogleCloudUser cloudUser) throws InvalidParameterException {
        //TODO - cloudUser needs to return projectId
        String projectId = PROJECT_NAME_EXAMPLE; //cloudUser.getProjectId();
        if (projectId == null) {
            LOGGER.error(Messages.Log.UNSPECIFIED_PROJECT_ID);
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }
}
