package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import org.apache.log4j.Logger;

/**
 * documentation: https://cwiki.apache.org/confluence/display/CLOUDSTACK/CloudStack+objects+states
 */

public class CloudStackStateMapper {
    private static final Logger LOGGER = Logger.getLogger(CloudStackStateMapper.class);

    private static final String CREATING_STATUS = "creating";
    private static final String STARTING_STATUS = "starting";
    private static final String READY_STATUS = "ready";
    private static final String ALLOCATED_STATUS = "allocated";
    private static final String IMPLEMENTING_STATUS = "implementing";
    private static final String IMPLEMENTED_STATUS = "implemented";
    public static final String RUNNING_STATUS = "running";
    public static final String DOWN_STATUS = "shutdowned";
    public static final String SETUP_STATUS = "setup";
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
                switch (cloudStackState) {
                    case IMPLEMENTING_STATUS:
                    case ALLOCATED_STATUS:
                        return InstanceState.CREATING;
                    case SETUP_STATUS:
                    case IMPLEMENTED_STATUS:
                        return InstanceState.READY;
                    case DOWN_STATUS:
                        return InstanceState.UNAVAILABLE;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, cloudStackState,
                                "CloudStackNetworkPlugin"));
                        return InstanceState.INCONSISTENT;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}
