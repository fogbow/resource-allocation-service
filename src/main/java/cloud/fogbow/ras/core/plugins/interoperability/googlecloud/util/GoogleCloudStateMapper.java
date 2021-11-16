package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.compute.v1.GoogleCloudComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.volume.v1.GoogleCloudVolumePlugin;
import org.apache.log4j.Logger;

public class GoogleCloudStateMapper {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudStateMapper.class);

    private static final String COMPUTE_PLUGIN = GoogleCloudComputePlugin.class.getSimpleName();
    private static final String VOLUME_PLUGIN = GoogleCloudVolumePlugin.class.getSimpleName();

    // Instance states
    public static final String PROVISIONING_STATE = "provisioning";
    public static final String STAGING_STATE = "staging";
    public static final String RUNNING_STATE = "running";
    public static final String STOPPING_STATE = "stopping";
    public static final String REPAIRING_STATE = "repairing";
    public static final String TERMINATED_STATE = "terminated";
    public static final String SUSPENDING_STATE = "suspending";
    public static final String SUSPENDED_STATE = "suspended";

    // Disk states
    public static final String CREATING_STATE = "creating";
    public static final String FAILED_STATE = "failed";
    public static final String READY_STATE = "ready";
    public static final String DELETING_STATE = "deleting";
    public static final String RESTORING_STATE = "restoring";

    //Network states
    public static final String SIMULATED_ERROR_STATE = "error";

    public static InstanceState map(ResourceType type, String state) {
        state = state.toLowerCase();

        switch (type){
            case ATTACHMENT:
                switch (state) {
                    default:
                        return InstanceState.READY;
                }
            case VOLUME:
                switch (state) {
                    case READY_STATE:
                        return InstanceState.READY;
                    case CREATING_STATE:
                        return InstanceState.CREATING;
                    case FAILED_STATE:
                        return InstanceState.FAILED;
                    case DELETING_STATE:
                    case RESTORING_STATE:
                        return InstanceState.BUSY;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, VOLUME_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case PUBLIC_IP:
                switch (state) {
                    // TODO: Implements states
                    default:
                        break;
                }
            case COMPUTE:
                switch (state) {
                    case PROVISIONING_STATE:
                    case STAGING_STATE:
                        return InstanceState.CREATING;
                    case RUNNING_STATE:
                        return InstanceState.READY;
                    case TERMINATED_STATE:
                        return InstanceState.STOPPED;
                    case SUSPENDED_STATE:
                    case STOPPING_STATE:
                    case SUSPENDING_STATE:
                        return InstanceState.BUSY;
                    case REPAIRING_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, COMPUTE_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case NETWORK:
                switch (state) {
                    case RUNNING_STATE:
                        return InstanceState.READY;
                    case SIMULATED_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, COMPUTE_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }

    }
}
