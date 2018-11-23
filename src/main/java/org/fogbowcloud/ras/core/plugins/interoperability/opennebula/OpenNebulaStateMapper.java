package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class OpenNebulaStateMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaStateMapper.class);

    private static final String ATTACHMENT_FAILURE_STATE = "Failure";
    private static final String ATTACMENT_RUNNING_STATE = "Running";
    
    private static final String COMPUTE_FAILURE_STATE = "Failure";
    private static final String COMPUTE_RUNNING_STATE = "Running";
    private static final String COMPUTE_SUSPENDED_STATE = "Suspended";

    private static final int IMAGE_INIT_STATE = 0;
    private static final int IMAGE_READY_STATE = 1;
    private static final int IMAGE_ERROR_STATE = 5;
    
    private static final int NETWORK_INIT_STATE = 0;
	private static final int NETWORK_ACTIVE_STATE = 3;
	private static final int NETWORK_FAILURE_STATE = 11;
	
	private static final String VOLUME_READY_STATE = "rdy";
	private static final String VOLUME_ERROR_STATE = "err";

    public static final String COMPUTE_PLUGIN = "OpenNebulaComputePlugin";
    public static final String IMAGE_PLUGIN = "OpenNebulaImagePlugin";
    public static final String VOLUME_PLUGIN = "OpenNebulaVolumePlugin";

    public static InstanceState map(ResourceType type, String oneState){
        oneState = oneState.toLowerCase();

        switch (type){
            case COMPUTE:
                switch (oneState) {
                    case COMPUTE_RUNNING_STATE:
                        return InstanceState.READY;
                    case COMPUTE_SUSPENDED_STATE:
                        return InstanceState.UNAVAILABLE;
                    case COMPUTE_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, COMPUTE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case VOLUME:
                switch (oneState) {
                    case VOLUME_READY_STATE:
                        return InstanceState.READY;
                    case VOLUME_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, VOLUME_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case ATTACHMENT:
                switch (oneState) {
                    case ATTACMENT_RUNNING_STATE:
                        return InstanceState.READY;
                    case ATTACHMENT_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, VOLUME_PLUGIN));
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
                    case IMAGE_INIT_STATE:
                        return InstanceState.CREATING;
                    case IMAGE_READY_STATE:
                        return InstanceState.READY;
                    case IMAGE_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, oneState, IMAGE_PLUGIN));
                        return InstanceState.UNAVAILABLE;
                }
            case NETWORK:
                switch (oneState) {
                    case NETWORK_INIT_STATE:
                        return InstanceState.CREATING;
                    case NETWORK_ACTIVE_STATE:
                        return InstanceState.READY;
                    case NETWORK_FAILURE_STATE:
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
