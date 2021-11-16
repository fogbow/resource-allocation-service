package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.compute.EmulatedCloudComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.network.EmulatedCloudNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.volume.EmulatedCloudVolumePlugin;
import org.apache.log4j.Logger;

public class EmulatedCloudStateMapper {
    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudStateMapper.class);

    public static final String COMPUTE_PLUGIN = EmulatedCloudComputePlugin.class.getSimpleName();
    public static final String NETWORK_PLUGIN = EmulatedCloudNetworkPlugin.class.getSimpleName();
    public static final String VOLUME_PLUGIN = EmulatedCloudVolumePlugin.class.getSimpleName();

    // common states
    public static final String ACTIVE_STATUS = "active";
    public static final String BUILD_STATUS = "build";
    public static final String DOWN_STATUS = "down";
    public static final String ERROR_STATUS = "error";

    // vm states
    public static final String PAUSED_STATUS = "paused";
    public static final String REBOOT_STATUS = "reboot";
    public static final String HIBERNATED_STATUS = "hibernated";
    public static final String STOPPED_STATUS = "stopped";
    public static final String SHUTOFF_STATUS = "shutoff";

    // storage states
    public static final String ATTACHING_STATUS = "attaching";
    public static final String DETACHING_STATUS = "detaching";

    public static InstanceState map(ResourceType type, String emulatedCloudState) {
        switch (type) {
            case COMPUTE:
                switch (emulatedCloudState) {
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    case BUILD_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    case DOWN_STATUS:
                    case PAUSED_STATUS:
                    case REBOOT_STATUS:
                    case SHUTOFF_STATUS:
                        return InstanceState.BUSY;
                    case HIBERNATED_STATUS:
                        return InstanceState.HIBERNATED;
                    case STOPPED_STATUS:
                        return InstanceState.STOPPED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, emulatedCloudState, COMPUTE_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case VOLUME:
                switch (emulatedCloudState) {
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    case BUILD_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    case DOWN_STATUS:
                    case ATTACHING_STATUS:
                    case DETACHING_STATUS:
                        return InstanceState.BUSY;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, emulatedCloudState, VOLUME_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case NETWORK:
                switch (emulatedCloudState) {
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    case BUILD_STATUS:
                        return InstanceState.CREATING;
                    case ERROR_STATUS:
                        return InstanceState.FAILED;
                    case DOWN_STATUS:
                        return InstanceState.BUSY;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, emulatedCloudState, NETWORK_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case PUBLIC_IP:
                switch (emulatedCloudState) {
                    case ACTIVE_STATUS:
                        return InstanceState.READY;
                    default:
                        return InstanceState.BUSY;
                }
            case ATTACHMENT:
                switch (emulatedCloudState) {
                    default:
                        return InstanceState.READY;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }
}