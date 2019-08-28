package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;

public class OpenStackCloudUtils {

    private static final Logger LOGGER = Logger.getLogger(OpenStackCloudUtils.class);

    public static String getProjectIdFrom(OpenStackV3User cloudUser) throws InvalidParameterException {
        String projectId = cloudUser.getProjectId();
        if (projectId == null) {
            LOGGER.error(Messages.Error.UNSPECIFIED_PROJECT_ID);
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }
}
