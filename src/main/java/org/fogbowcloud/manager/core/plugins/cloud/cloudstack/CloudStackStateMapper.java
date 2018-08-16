package org.fogbowcloud.manager.core.plugins.cloud.cloudstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;

public class CloudStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(CloudStackStateMapper.class);

    public static final String RUNNING_STATUS = "Running";
    public static final String DOWN_STATUS = "Shutdowned";
    public static final String ERROR_STATUS = "Error";

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
