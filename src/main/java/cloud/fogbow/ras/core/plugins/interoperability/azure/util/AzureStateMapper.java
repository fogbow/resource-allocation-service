package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.AzureAttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.AzureVolumePlugin;

import cloud.fogbow.ras.core.plugins.interoperability.azure.network.AzureNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.AzurePublicIpPlugin;

import org.apache.log4j.Logger;

public class AzureStateMapper {

    private static final Logger LOGGER = Logger.getLogger(AzureStateMapper.class);

    private static final String ATTACHMENT_PLUGIN = AzureAttachmentPlugin.class.getSimpleName();
    private static final String COMPUTE_PLUGIN = AzureComputePlugin.class.getSimpleName();
    private static final String VOLUME_PLUGIN = AzureVolumePlugin.class.getSimpleName();
    private static final String NETWORK_PLUGIN = AzureNetworkPlugin.class.getSimpleName();
    private static final String PUBLIC_IP_PLUGIN = AzurePublicIpPlugin.class.getSimpleName();
    
    public static final String ATTACHED_STATE = "Attached";
    public static final String CREATING_STATE = "Creating";
    public static final String SUCCEEDED_STATE = "Succeeded";
    public static final String UNATTACHED_STATE = "Unattached";
    public static final String FAILED_STATE = "Failed";
    public static final String DEALLOCATING_STATE = "Deallocating";
    public static final String DEALLOCATED_STATE = "Deallocated";
    public static final String STARTING_STATE = "Starting";

    public static InstanceState map(ResourceType type, String state) {
        switch (type) {
            case ATTACHMENT:
            // cloud state values: [attached, unattached]
            switch (state) {
                case ATTACHED_STATE:
                    return InstanceState.READY;
                case UNATTACHED_STATE:
                case FAILED_STATE:
                    return InstanceState.FAILED;
                default:
                    LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, ATTACHMENT_PLUGIN));
                    return InstanceState.INCONSISTENT;
            }
            case COMPUTE:
                // cloud state values: [creating, succeeded, failed, deallocating, deallocated, starting]
                switch (state) {
                    case CREATING_STATE:
                        return InstanceState.CREATING;
                    case SUCCEEDED_STATE:
                        return InstanceState.READY;
                    case FAILED_STATE:
                        return InstanceState.FAILED;
                    case DEALLOCATING_STATE:
                        return InstanceState.STOPPING;
                    case DEALLOCATED_STATE:
                        return InstanceState.STOPPED;
                    case STARTING_STATE:
                        return InstanceState.RESUMING;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, COMPUTE_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case VOLUME:
                // cloud state values: [creating, succeeded]
                switch (state) {
                    case CREATING_STATE:
                        return InstanceState.CREATING;
                    case SUCCEEDED_STATE:
                        return InstanceState.READY;
                    case FAILED_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, VOLUME_PLUGIN));
                }
            case NETWORK:
                // cloud state values: [creating, failed, succeeded]
                switch (state) {
                    case CREATING_STATE:
                        return InstanceState.CREATING;
                    case SUCCEEDED_STATE:
                        return InstanceState.READY;
                    case FAILED_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, NETWORK_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            case PUBLIC_IP:
                // cloud state values: [creating, succeeded]
                switch (state) {
                    case CREATING_STATE:
                        return InstanceState.CREATING;
                    case SUCCEEDED_STATE:
                        return InstanceState.READY;
                    case FAILED_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Log.UNDEFINED_INSTANCE_STATE_MAPPING_S_S, state, PUBLIC_IP_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }

}
