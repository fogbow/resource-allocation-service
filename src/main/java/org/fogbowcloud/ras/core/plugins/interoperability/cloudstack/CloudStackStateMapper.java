package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class CloudStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(CloudStackStateMapper.class);

    private static final String CREATING_STATUS = "creating";
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
                switch (cloudStackState) {
                    case RUNNING_STATUS:
                        return InstanceState.READY;
                    case DOWN_STATUS:
                        return InstanceState.INACTIVE;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(getDefaultLogMessage(cloudStackState, "CloudStackComputePlugin"));
                        return InstanceState.INCONSISTENT;
                }

            case VOLUME:
                switch (cloudStackState) {
                    case CREATING_STATUS:
                        return InstanceState.CREATING;

                    case READY_STATUS:
                        return InstanceState.READY;

                    default:
                        LOGGER.error(getDefaultLogMessage(cloudStackState, "CloudStackVolumePlugin"));
                        return InstanceState.INCONSISTENT;
                }
            
            case ATTACHMENT:
                switch (cloudStackState) {
                    case PENDING_STATUS:
                        return InstanceState.ATTACHING;

                    case READY_STATUS:
                        return InstanceState.READY;
                        
                    case FAILURE_STATUS:
                        return InstanceState.FAILED;

                    default:
                        LOGGER.error(getDefaultLogMessage(cloudStackState, "CloudStackVolumePlugin"));
                        return InstanceState.INCONSISTENT;
                }

            default:
                LOGGER.error("Instance type not defined.");
                return InstanceState.INCONSISTENT;
        }
    }

    private static String getDefaultLogMessage(String openStackState, String pluginName) {
        return openStackState + " was not mapped to a well-defined OpenStack "
                + "instance state when " + pluginName + " were implemented.";
    }
}
