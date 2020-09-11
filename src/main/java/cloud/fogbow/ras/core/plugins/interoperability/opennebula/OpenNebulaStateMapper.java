package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4.OpenNebulaAttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4.OpenNebulaComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4.OpenNebulaImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.OpenNebulaNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4.OpenNebulaPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4.OpenNebulaVolumePlugin;
import org.apache.log4j.Logger;

public class OpenNebulaStateMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaStateMapper.class);

    public static final String USED_STATE = "used";
    public static final String ATTACHMENT_USED_PERSISTENT_STATE = "used_pers";
    public static final String COMPUTE_FAILURE_STATE = "failure";
    public static final String COMPUTE_INIT_STATE = "lcm_init";
    public static final String COMPUTE_BOOT_STATE = "boot";
    public static final String COMPUTE_PENDING_STATE = "pending";
    public static final String COMPUTE_RUNNING_STATE = "running";
    public static final String COMPUTE_SPAWNING_STATE = "prolog";
    public static final String COMPUTE_SUSPENDED_STATE = "suspended";
    public static final String DEFAULT_ERROR_STATE = "error";
    public static final String DEFAULT_READY_STATE = "ready";

    public static final int IMAGE_INIT_STATE = 0;
    public static final int IMAGE_READY_STATE = 1;
    public static final int IMAGE_ERROR_STATE = 5;

    public static final String ATTACHMENT_PLUGIN = OpenNebulaAttachmentPlugin.class.getSimpleName();
    public static final String COMPUTE_PLUGIN = OpenNebulaComputePlugin.class.getSimpleName();
    public static final String IMAGE_PLUGIN = OpenNebulaImagePlugin.class.getSimpleName();
    public static final String NETWORK_PLUGIN = OpenNebulaNetworkPlugin.class.getSimpleName();
    public static final String PUBLIC_IP_PLUGIN = OpenNebulaPublicIpPlugin.class.getSimpleName();
    public static final String VOLUME_PLUGIN = OpenNebulaVolumePlugin.class.getSimpleName();

    public static InstanceState map(ResourceType type, String state){
        state = state.toLowerCase();
        switch (type){
            case COMPUTE:
                switch (state) {
                	case COMPUTE_PENDING_STATE:
                    case COMPUTE_SPAWNING_STATE:
                    case COMPUTE_INIT_STATE:
                    case COMPUTE_BOOT_STATE:
                        return InstanceState.CREATING;
                	case COMPUTE_RUNNING_STATE:
                        return InstanceState.READY;
                    case COMPUTE_SUSPENDED_STATE:
                        return InstanceState.BUSY;
                    case COMPUTE_FAILURE_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, COMPUTE_PLUGIN));
                        return InstanceState.BUSY;
                }
            case VOLUME:
                switch (state) {
                    case DEFAULT_READY_STATE:
                        return InstanceState.READY;
                    case DEFAULT_ERROR_STATE:
                        return InstanceState.FAILED;
                    case USED_STATE:
                        return InstanceState.BUSY;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, VOLUME_PLUGIN));
                        return InstanceState.BUSY;
                }
            case ATTACHMENT:
                switch (state) {
                	case USED_STATE:
                		return InstanceState.READY;
                    case ATTACHMENT_USED_PERSISTENT_STATE:
                        return InstanceState.READY;
                    case DEFAULT_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, ATTACHMENT_PLUGIN));
                        return InstanceState.BUSY;
                }
            case NETWORK:
                switch (state) {
                    case DEFAULT_READY_STATE:
                        return InstanceState.READY;
                    case DEFAULT_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, NETWORK_PLUGIN));
                        return InstanceState.BUSY;
                }
            case PUBLIC_IP:
                switch (state) {
                    case DEFAULT_READY_STATE:
                        return InstanceState.READY;
                    case DEFAULT_ERROR_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, PUBLIC_IP_PLUGIN));
                        return InstanceState.BUSY;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
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
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, IMAGE_PLUGIN));
                        return InstanceState.BUSY;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}
