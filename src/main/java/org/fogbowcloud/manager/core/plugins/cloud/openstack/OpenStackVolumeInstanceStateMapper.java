package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.plugins.cloud.InstanceStateMapper;

public class OpenStackVolumeInstanceStateMapper implements InstanceStateMapper {
    private static final String VALUE_CREATING_STATUS = "creating";
    private static final String VALUE_AVAILABLE_STATUS = "available";
    private static final String VALUE_ATTACHING_STATUS = "attaching";
    private static final String VALUE_DETACHING_STATUS = "detaching";
    private static final String VALUE_IN_USE_STATUS = "in-use";
    private static final String VALUE_MAINTENANCE_STATUS = "maintenance";
    private static final String VALUE_DELETING_STATUS = "deleting";
    private static final String VALUE_AWAITING_TRANSFER_STATUS = "awaiting-transfer";
    private static final String VALUE_ERROR_STATUS = "error";
    private static final String VALUE_ERROR_DELETING_STATUS = "error_deleting";
    private static final String VALUE_BACKING_UP_STATUS = "backing-up";
    private static final String VALUE_RESTORING_BACKUP_STATUS = "restoring-backup";
    private static final String VALUE_ERROR_BACKING_UP_STATUS = "error_backing-up";
    private static final String VALUE_ERROR_RESTORING_STATUS = "error_restoring";
    private static final String VALUE_ERROR_EXTENDING_STATUS = "error_extending";
    private static final String VALUE_DOWNLOADING_STATUS = "downloading";
    private static final String VALUE_UPLOADING_STATUS = "uploading";
    private static final String VALUE_RETYPING_STATUS = "retyping";
    private static final String VALUE_EXTENDING_STATUS = "extending";

    // TODO check openstack documentation. https://developer.openstack.org/api-ref/block-storage/v2/
    // TODO: Better map openstack states. Should be better create InstanceState.BUSY?
    @Override
    public InstanceState getInstanceState(String statusOpenstack) {
        InstanceState state = null;
        switch (statusOpenstack.toLowerCase()) {
            case VALUE_CREATING_STATUS:
                return InstanceState.INACTIVE;
            case VALUE_AVAILABLE_STATUS:
                return InstanceState.READY;
            case VALUE_ATTACHING_STATUS:
                return InstanceState.READY;
            case VALUE_DETACHING_STATUS:
                return InstanceState.READY;
            case VALUE_IN_USE_STATUS:
                return InstanceState.READY;
            case VALUE_MAINTENANCE_STATUS:
                return InstanceState.READY;
            case VALUE_DELETING_STATUS:
                return InstanceState.READY;
            case VALUE_AWAITING_TRANSFER_STATUS:
                return InstanceState.READY;
            case VALUE_ERROR_STATUS:
                return InstanceState.FAILED;
            case VALUE_ERROR_DELETING_STATUS:
                return InstanceState.FAILED;
            case VALUE_BACKING_UP_STATUS:
                return InstanceState.READY;
            case VALUE_RESTORING_BACKUP_STATUS:
                return InstanceState.READY;
            case VALUE_ERROR_RESTORING_STATUS:
                return InstanceState.FAILED;
            case VALUE_ERROR_BACKING_UP_STATUS:
                return InstanceState.FAILED;
            case VALUE_ERROR_EXTENDING_STATUS:
                return InstanceState.FAILED;
            case VALUE_DOWNLOADING_STATUS:
                return InstanceState.READY;
            case VALUE_UPLOADING_STATUS:
                return InstanceState.READY;
            case VALUE_RETYPING_STATUS:
                return InstanceState.READY;
            case VALUE_EXTENDING_STATUS:
                return InstanceState.READY;
            default:
                return InstanceState.INACTIVE;
        }
    }

}

