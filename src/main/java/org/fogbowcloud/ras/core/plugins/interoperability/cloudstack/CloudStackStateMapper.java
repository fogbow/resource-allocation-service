package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class CloudStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(CloudStackStateMapper.class);

    private static final String CREATING_STATUS = "creating";
    private static final String STARTING_STATUS = "starting";
    private static final String READY_STATUS = "ready";
    public static final String RUNNING_STATUS = "running";
    public static final String DOWN_STATUS = "shutdowned";
    public static final String ERROR_STATUS = "error";
    public static final String PENDING_STATUS = "pending";
    public static final String FAILURE_STATUS = "failure";

    public static InstanceState map(ResourceType type, String cloudStackState) {

        cloudStackState = cloudStackState.toLowerCase();

        switch (type) {
            case COMPUTE:
                if (cloudStackState.equalsIgnoreCase(RUNNING_STATUS)) {
                    return InstanceState.READY;
                } else if (cloudStackState.equalsIgnoreCase(DOWN_STATUS)) {
                    return InstanceState.INACTIVE;
                } else if (cloudStackState.equalsIgnoreCase(ERROR_STATUS)) {
                    return InstanceState.FAILED;
                } else if (cloudStackState.equalsIgnoreCase(STARTING_STATUS)) {
                    return InstanceState.SPAWNING;
                } else {
                    LOGGER.error(getDefaultLogMessage(cloudStackState, "CloudStackComputePlugin"));
                    return InstanceState.INCONSISTENT;
                }
            case VOLUME:
                if (cloudStackState.equalsIgnoreCase(CREATING_STATUS)) {
                    return InstanceState.CREATING;
                } else if (cloudStackState.equalsIgnoreCase(READY_STATUS)) {
                    return InstanceState.READY;
                } else {
                    LOGGER.error(getDefaultLogMessage(cloudStackState, "CloudStackVolumePlugin"));
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
                    LOGGER.error(getDefaultLogMessage(cloudStackState, "CloudStackVolumePlugin"));
                    return InstanceState.INCONSISTENT;
                }
            default:
                LOGGER.error("Instance type not defined.");
                return InstanceState.INCONSISTENT;
        }
    }

    private static String getDefaultLogMessage(String openStackState, String pluginName) {
        return openStackState + " was not mapped to a well-defined CloudStack "
                + "instance state when " + pluginName + " were implemented.";
    }
}
