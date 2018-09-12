package org.fogbowcloud.ras.core.plugins.interoperability.openstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class OpenStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(OpenStackStateMapper.class);

    public static final String ACTIVE_STATUS = "active";
    public static final String BUILD_STATUS = "build";
    public static final String DOWN_STATUS = "down";
    public static final String ERROR_STATUS = "error";
    public static final String CREATING_STATUS = "creating";
    public static final String AVAILABLE_STATUS = "available";
    public static final String ATTACHING_STATUS = "attaching";
    public static final String DETACHING_STATUS = "detaching";
    public static final String IN_USE_STATUS = "in-use";
    public static final String MAINTENANCE_STATUS = "maintenance";
    public static final String DELETING_STATUS = "deleting";
    public static final String AWAITING_TRANSFER_STATUS = "awaiting-transfer";
    public static final String ERROR_DELETING_STATUS = "error_deleting";
    public static final String BACKING_UP_STATUS = "backing-up";
    public static final String RESTORING_BACKUP_STATUS = "restoring-backup";
    public static final String ERROR_BACKING_UP_STATUS = "error_backing-up";
    public static final String ERROR_RESTORING_STATUS = "error_restoring";
    public static final String ERROR_EXTENDING_STATUS = "error_extending";
    public static final String DOWNLOADING_STATUS = "downloading";
    public static final String UPLOADING_STATUS = "uploading";
    public static final String RETYPING_STATUS = "retyping";
    public static final String EXTENDING_STATUS = "extending";
    public static final String COMPUTE_PLUGIN = "OpenstackComputePlugin";
    public static final String NETWORK_PLUGIN = "OpenstackNetworkPlugin";
    public static final String VOLUME_PLUGIN = "OpenstackVolumePlugin";

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
                    	LOGGER.error(String.format(Messages.Error.INSTANCE_STATE_NOT_MAPPED, openStackState, COMPUTE_PLUGIN));
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
                    	LOGGER.error(String.format(Messages.Error.INSTANCE_STATE_NOT_MAPPED, openStackState, NETWORK_PLUGIN));
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
                    	LOGGER.error(String.format(Messages.Error.INSTANCE_STATE_NOT_MAPPED, openStackState, VOLUME_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case ATTACHMENT:
                switch (openStackState) {
                    default:
                        return InstanceState.READY;
                }
            default:
            	LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }

}
