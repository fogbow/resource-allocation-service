package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.compute.v1.GoogleCloudComputePlugin;
import org.apache.log4j.Logger;

public class GoogleCloudStateMapper {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudStateMapper.class);

    private static final String COMPUTE_PLUGIN = GoogleCloudComputePlugin.class.getSimpleName();

    // Instance states
    private static final String PROVISIONING_STATE = "provisioning";
    private static final String STAGING_STATE = "staging";
    private static final String RUNNING_STATE = "running";
    private static final String STOPPING_STATE = "stopping";
    private static final String REPAIRING_STATE = "repairing";
    private static final String TERMINATED_STATE = "terminated";
    private static final String SUSPENDING_STATE = "suspending";
    private static final String SUSPENDED_STATE = "suspended";

    //Network states
    private static final String SIMULATED_ERROR_STATE = "error";

    public static InstanceState map(ResourceType type, String state) {
        state = state.toLowerCase();

        switch (type){
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
