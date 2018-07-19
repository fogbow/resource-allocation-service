package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;

public class OpenStackStateMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenStackStateMapper.class);

    public static final String ACTIVE_STATUS = "active";
    public static final String BUILD_STATUS = "build";
    public static final String DOWN_STATUS = "down";
    public static final String ERROR_STATUS = "error";
    private static final String CREATING_STATUS = "creating";
    private static final String AVAILABLE_STATUS = "available";
    private static final String ATTACHING_STATUS = "attaching";
    private static final String DETACHING_STATUS = "detaching";
    private static final String IN_USE_STATUS = "in-use";
    private static final String MAINTENANCE_STATUS = "maintenance";
    private static final String DELETING_STATUS = "deleting";
    private static final String AWAITING_TRANSFER_STATUS = "awaiting-transfer";
    private static final String ERROR_DELETING_STATUS = "error_deleting";
    private static final String BACKING_UP_STATUS = "backing-up";
    private static final String RESTORING_BACKUP_STATUS = "restoring-backup";
    private static final String ERROR_BACKING_UP_STATUS = "error_backing-up";
    private static final String ERROR_RESTORING_STATUS = "error_restoring";
    private static final String ERROR_EXTENDING_STATUS = "error_extending";
    private static final String DOWNLOADING_STATUS = "downloading";
    private static final String UPLOADING_STATUS = "uploading";
    private static final String RETYPING_STATUS = "retyping";
    private static final String EXTENDING_STATUS = "extending";

    public static InstanceState map(InstanceType type, String openStackState) {

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
