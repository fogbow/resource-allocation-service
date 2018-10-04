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
                if (cloudStackState.equalsIgnoreCase(RUNNING_STATUS)) {
                    return InstanceState.READY;
                } else if (cloudStackState.equalsIgnoreCase(DOWN_STATUS) || cloudStackState.equalsIgnoreCase(STOPPED_STATUS) ||
                           cloudStackState.equalsIgnoreCase(STOPPING_STATUS) || cloudStackState.equalsIgnoreCase(STARTING_STATUS) ||
                           cloudStackState.equalsIgnoreCase(EXPUNGING_STATUS)) {
                    return InstanceState.INACTIVE;
                } else if (cloudStackState.equalsIgnoreCase(ERROR_STATUS)) {
                    return InstanceState.FAILED;
                } else if (cloudStackState.equalsIgnoreCase(STARTING_STATUS)) {
                    return InstanceState.SPAWNING;
                } else {
                    LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                            "CloudStackComputePlugin"));
                    return InstanceState.INCONSISTENT;
                }
            case VOLUME:
                if (cloudStackState.equalsIgnoreCase(CREATING_STATUS)) {
                    return InstanceState.CREATING;
                } else if (cloudStackState.equalsIgnoreCase(READY_STATUS)) {
                    return InstanceState.READY;
                } else {
                    LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                            "CloudStackVolumePlugin"));
                    return InstanceState.INCONSISTENT;
                }
            case ATTACHMENT:
                if (cloudStackState.equalsIgnoreCase(PENDING_STATUS)) {
                    return InstanceState.ATTACHING;
                } else if (cloudStackState.equalsIgnoreCase(READY_STATUS)) {
                    return InstanceState.READY;
                } else if (cloudStackState.equalsIgnoreCase(FAILURE_STATUS)) {
                    return InstanceState.FAILED;
                } else {
                    LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                            "CloudStackAttachmentPlugin"));
                    return InstanceState.INCONSISTENT;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}
