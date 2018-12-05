package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class CloudStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(CloudStackStateMapper.class);

    private static final String CREATING_STATUS = "creating";
    private static final String STARTING_STATUS = "starting";
    private static final String READY_STATUS = "ready";
    private static final String ALLOCATED_STATUS = "allocated";
    public static final String RUNNING_STATUS = "running";
    public static final String DOWN_STATUS = "shutdowned";
    public static final String STOPPED_STATUS = "stopped";
    public static final String STOPPING_STATUS = "stopping";
    public static final String EXPUNGING_STATUS = "expunging";
    public static final String ERROR_STATUS = "error";
    public static final String PENDING_STATUS = "pending";
    public static final String FAILURE_STATUS = "failure";

    public static InstanceState map(ResourceType type, String cloudStackState) {

        cloudStackState = cloudStackState.toLowerCase();

        switch (type) {
            case COMPUTE:
                switch(cloudStackState) {
                    case RUNNING_STATUS:
                        return InstanceState.READY;
                    case DOWN_STATUS:
                    case STOPPED_STATUS:
                    case STOPPING_STATUS:
                    case EXPUNGING_STATUS:
                        return InstanceState.UNAVAILABLE;
                    case STARTING_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                                "CloudStackComputePlugin"));
                        return InstanceState.INCONSISTENT;
                }
            case VOLUME:
                switch(cloudStackState) {
                    case READY_STATUS:
                    case ALLOCATED_STATUS:
                        return InstanceState.READY;
                    case CREATING_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                    case FAILURE_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                                "CloudStackVolumePlugin"));
                        return InstanceState.INCONSISTENT;
                }
            case ATTACHMENT:
                switch(cloudStackState) {
                    case READY_STATUS:
                        return InstanceState.READY;
                    case PENDING_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                    case FAILURE_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                                "CloudStackAttachmentPlugin"));
                        return InstanceState.INCONSISTENT;
                }
            case NETWORK:
                // ToDo: find documentation to set this up appropriately
                return InstanceState.READY;
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}
