package cloud.fogbow.ras.core.plugins.interoperability.openstack.util;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.OpenStackNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2.OpenStackVolumePlugin;
import org.apache.log4j.Logger;

public class OpenStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(OpenStackStateMapper.class);

    public static final String COMPUTE_PLUGIN = OpenStackComputePlugin.class.getSimpleName();
    public static final String VOLUME_PLUGIN = OpenStackVolumePlugin.class.getSimpleName();
    public static final String NETWORK_PLUGIN = OpenStackNetworkPlugin.class.getSimpleName();

    public static final String ACTIVE_STATUS = "active";
    public static final String ATTACHING_STATUS = "attaching";
    public static final String AVAILABLE_STATUS = "available";
    public static final String AWAITING_TRANSFER_STATUS = "awaiting-transfer";
    public static final String BACKING_UP_STATUS = "backing-up";
    public static final String BUILD_STATUS = "build";
    public static final String CREATING_STATUS = "creating";
    public static final String DELETED_STATUS = "deleted";
    public static final String DELETING_STATUS = "deleting";
    public static final String DETACHING_STATUS = "detaching";
    public static final String DOWNLOADING_STATUS = "downloading";
    public static final String DOWN_STATUS = "down";
    public static final String ERROR_BACKING_UP_STATUS = "error_backing-up";
    public static final String ERROR_DELETING_STATUS = "error_deleting";
    public static final String ERROR_EXTENDING_STATUS = "error_extending";
    public static final String ERROR_RESTORING_STATUS = "error_restoring";
    public static final String ERROR_STATUS = "error";
    public static final String EXTENDING_STATUS = "extending";
    public static final String HARD_REBOOT_STATUS = "hard_reboot";
    public static final String IN_USE_STATUS = "in-use";
    public static final String MAINTENANCE_STATUS = "maintenance";
    public static final String MIGRATING_STATUS = "migrating";
    public static final String PASSWORD_STATUS = "password";
    public static final String PAUSED_STATUS = "paused";
    public static final String REBOOT_STATUS = "reboot";
    public static final String REBUILD_STATUS = "rebuild";
    public static final String RESCUE_STATUS = "rescue";
    public static final String RESIZE_STATUS = "resize";
    public static final String RESTORING_BACKUP_STATUS = "restoring-backup";
    public static final String RETYPING_STATUS = "retyping";
    public static final String REVERT_RESIZE_STATUS = "revert_resize";
    public static final String SHELVED_STATUS = "shelved";
    public static final String SHELVED_OFFLOADED_STATUS = "shelved_offloaded";
    public static final String SHUTOFF_STATUS = "shutoff";
    public static final String SOFT_DELETED_STATUS = "soft_deleted";
    public static final String SUSPENDED_STATUS = "suspended";
    public static final String UNKNOWN_STATUS = "unknown";
    public static final String UPLOADING_STATUS = "uploading";
    public static final String VERIFY_RESIZE_STATUS = "verify_resize";

    public static InstanceState map(ResourceType type, String openStackState) {

        openStackState = openStackState.toLowerCase();

        switch (type) {
            case COMPUTE:
                switch (openStackState) {
                    case ACTIVE_STATUS:
                    case PASSWORD_STATUS:
                    case RESCUE_STATUS:
                        return InstanceState.READY;
                    case BUILD_STATUS:
                    case REBUILD_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    case PAUSED_STATUS:
                        return InstanceState.PAUSED;
                    case SUSPENDED_STATUS:
                        return InstanceState.HIBERNATED;
                    case SHELVED_STATUS:
                    case SHELVED_OFFLOADED_STATUS:
                        return InstanceState.STOPPED;
                    case DELETED_STATUS:
                    case HARD_REBOOT_STATUS:
                    case MIGRATING_STATUS:
                    case REBOOT_STATUS:
                    case RESIZE_STATUS:
                    case REVERT_RESIZE_STATUS:
                    case SHUTOFF_STATUS:
                    case SOFT_DELETED_STATUS:
                    case VERIFY_RESIZE_STATUS:
                        return InstanceState.BUSY;
                    case UNKNOWN_STATUS:
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, openStackState, COMPUTE_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case NETWORK:
                switch (openStackState) {
                    case BUILD_STATUS:
                        return InstanceState.CREATING;
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    case DOWN_STATUS:
                        return InstanceState.BUSY;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, openStackState, NETWORK_PLUGIN));
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
                    case ATTACHING_STATUS:
                    case IN_USE_STATUS:
                        return InstanceState.BUSY;
                    case ERROR_BACKING_UP_STATUS:
                    case ERROR_DELETING_STATUS:
                    case ERROR_EXTENDING_STATUS:
                    case ERROR_RESTORING_STATUS:
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, openStackState, VOLUME_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case ATTACHMENT:
                switch (openStackState) {
                    default:
                        return InstanceState.READY;
                }
            case PUBLIC_IP:
                switch (openStackState) {
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    default:
                        return InstanceState.BUSY;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }

}
