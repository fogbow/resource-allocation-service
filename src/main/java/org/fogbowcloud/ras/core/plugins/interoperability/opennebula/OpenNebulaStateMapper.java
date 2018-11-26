package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class OpenNebulaStateMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaStateMapper.class);

    private static final String ATTACMENT_USED_PERSISTENT_STATE = "used_pers";
    
    private static final String COMPUTE_FAILURE_STATE = "failure";
    private static final String COMPUTE_RUNNING_STATE = "running";
    private static final String COMPUTE_SUSPENDED_STATE = "suspended";

    private static final String DEFAULT_ERROR_STATE = "error";
    
    private static final int IMAGE_INIT_STATE = 0;
    private static final int IMAGE_READY_STATE = 1;
    private static final int IMAGE_ERROR_STATE = 5;
    
    private static final int NETWORK_INIT_STATE = 0;
	private static final int NETWORK_ACTIVE_STATE = 3;
	private static final int NETWORK_FAILURE_STATE = 11;
	
	private static final String VOLUME_READY_STATE = "ready";

    public static final String COMPUTE_PLUGIN = "OpenNebulaComputePlugin";
    public static final String IMAGE_PLUGIN = "OpenNebulaImagePlugin";
    public static final String VOLUME_PLUGIN = "OpenNebulaVolumePlugin";

    public static InstanceState map(ResourceType type, String state){
        state = state.toLowerCase();
        

        switch (type){
            case COMPUTE:
                switch (state) {
                    case COMPUTE_RUNNING_STATE:
                        return InstanceState.READY;
                    case COMPUTE_SUSPENDED_STATE:
                        return InstanceState.UNAVAILABLE;
                    case COMPUTE_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, COMPUTE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case VOLUME:
                switch (state) {
                    case VOLUME_READY_STATE:
                        return InstanceState.READY;
                    case DEFAULT_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, VOLUME_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case ATTACHMENT:
                switch (state) {
                    case ATTACMENT_USED_PERSISTENT_STATE:
                        return InstanceState.READY;
                    case DEFAULT_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, VOLUME_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
            
        }
    }

    public static InstanceState map(ResourceType type, int state){
        switch (type){
            case IMAGE:
                switch (state) {
                    case IMAGE_INIT_STATE:
                        return InstanceState.CREATING;
                    case IMAGE_READY_STATE:
                        return InstanceState.READY;
                    case IMAGE_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, IMAGE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case NETWORK:
                switch (state) {
                    case NETWORK_INIT_STATE:
                        return InstanceState.CREATING;
                    case NETWORK_ACTIVE_STATE:
                        return InstanceState.READY;
                    case NETWORK_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, IMAGE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}
