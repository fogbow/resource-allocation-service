package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class OpenNebulaStateMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaStateMapper.class);

    private static final String ONE_VM_FAILURE_STATE = "Failure";
    private static final String ONE_VM_RUNNING_STATE = "Running";
    private static final String ONE_VM_SUSPENDED_STATE = "Suspended";

    private static final int ONE_IMAGE_INIT_STATE = 0;
    private static final int ONE_IMAGE_READY_STATE = 1;
    private static final int ONE_IMAGE_ERROR_STATE = 5;
    
    private static final int ONE_NETWORK_INIT_STATE = 0;
	private static final int ONE_NETWORK_ACTIVE_STATE = 3;
	private static final int ONE_NETWORK_FAILURE_STATE = 11;

    public static final String COMPUTE_PLUGIN = "OpenNebulaComputePlugin";
    public static final String IMAGE_PLUGIN = "OpenNebulaImagePlugin";

    public static InstanceState map(ResourceType type, String oneState){
        oneState = oneState.toLowerCase();

        switch (type){
            case COMPUTE:
                switch (oneState) {
                    case ONE_VM_RUNNING_STATE:
                        return InstanceState.READY;
                    case ONE_VM_SUSPENDED_STATE:
                        return InstanceState.UNAVAILABLE;
                    case ONE_VM_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, COMPUTE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }

    public static InstanceState map(ResourceType type, int oneState){
        switch (type){
            case IMAGE:
                switch (oneState) {
                    case ONE_IMAGE_INIT_STATE:
                        return InstanceState.CREATING;
                    case ONE_IMAGE_READY_STATE:
                        return InstanceState.READY;
                    case ONE_IMAGE_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, IMAGE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case NETWORK:
                switch (oneState) {
                    case ONE_NETWORK_INIT_STATE:
                        return InstanceState.CREATING;
                    case ONE_NETWORK_ACTIVE_STATE:
                        return InstanceState.READY;
                    case ONE_NETWORK_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, IMAGE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}
