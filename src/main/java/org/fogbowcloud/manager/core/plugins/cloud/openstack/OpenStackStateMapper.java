package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.ResourceType;

public class OpenStackStateMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenStackStateMapper.class);

    protected static final String ACTIVE_STATUS = "active";
    protected static final String BUILD_STATUS = "build";
    protected static final String DOWN_STATUS = "down";
    protected static final String ERROR_STATUS = "error";
    protected static final String CREATING_STATUS = "creating";
    protected static final String AVAILABLE_STATUS = "available";
    protected static final String ATTACHING_STATUS = "attaching";
    protected static final String DETACHING_STATUS = "detaching";
    protected static final String IN_USE_STATUS = "in-use";
    protected static final String MAINTENANCE_STATUS = "maintenance";
    protected static final String DELETING_STATUS = "deleting";
    protected static final String AWAITING_TRANSFER_STATUS = "awaiting-transfer";
    protected static final String ERROR_DELETING_STATUS = "error_deleting";
    protected static final String BACKING_UP_STATUS = "backing-up";
    protected static final String RESTORING_BACKUP_STATUS = "restoring-backup";
    protected static final String ERROR_BACKING_UP_STATUS = "error_backing-up";
    protected static final String ERROR_RESTORING_STATUS = "error_restoring";
    protected static final String ERROR_EXTENDING_STATUS = "error_extending";
    protected static final String DOWNLOADING_STATUS = "downloading";
    protected static final String UPLOADING_STATUS = "uploading";
    protected static final String RETYPING_STATUS = "retyping";
    protected static final String EXTENDING_STATUS = "extending";

    public static InstanceState map(ResourceType type, String openStackState) {

        openStackState = openStackState.toLowerCase();

        switch (type) {
            case COMPUTE:
                switch (openStackState) {
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    case BUILD_STATUS:
                        return InstanceState.SPAWNING;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(getDefaultLogMessage(openStackState, "OpenstackComputePlugin"));
                        return InstanceState.INCONSISTENT;
                }
            case NETWORK:
                switch (openStackState) {
                    case BUILD_STATUS:
                    case DOWN_STATUS:
                        return InstanceState.INACTIVE;
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(getDefaultLogMessage(openStackState, "OpenstackNetworkPlugin"));
                        return InstanceState.INCONSISTENT;
                }
            case VOLUME:
                switch (openStackState) {
                    case CREATING_STATUS:
                        return InstanceState.CREATING;
                    case AVAILABLE_STATUS:
                        return InstanceState.READY;
                    case DETACHING_STATUS:
                    case MAINTENANCE_STATUS:
                    case DELETING_STATUS:
                    case AWAITING_TRANSFER_STATUS:
                    case BACKING_UP_STATUS:
                    case RESTORING_BACKUP_STATUS:
                    case DOWNLOADING_STATUS:
                    case UPLOADING_STATUS:
                    case RETYPING_STATUS:
                    case EXTENDING_STATUS:
                        return InstanceState.UNAVAILABLE;
                    case ATTACHING_STATUS:
                        return InstanceState.ATTACHING;
                    case IN_USE_STATUS:
                        return InstanceState.IN_USE;
                    case ERROR_BACKING_UP_STATUS:
                    case ERROR_DELETING_STATUS:
                    case ERROR_EXTENDING_STATUS:
                    case ERROR_RESTORING_STATUS:
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(getDefaultLogMessage(openStackState, "OpenstackVolumePlugin"));
                        return InstanceState.INCONSISTENT;
                }
            case ATTACHMENT:
                switch (openStackState) {
                    default:
                        return InstanceState.READY;
                }
            default:
                LOGGER.error("Instance type not defined.");
                return InstanceState.INCONSISTENT;
        }
    }

    private static String getDefaultLogMessage(String openStackState, String pluginName) {
        return openStackState + " was not mapped to a well-defined OpenStack " +
                "instance state when " + pluginName + " were implemented.";
    }
}
