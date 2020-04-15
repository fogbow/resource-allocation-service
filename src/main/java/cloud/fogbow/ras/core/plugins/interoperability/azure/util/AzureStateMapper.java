package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.AzureAttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.AzureVolumePlugin;

import cloud.fogbow.ras.core.plugins.interoperability.azure.network.AzureNetworkPlugin;
import org.apache.log4j.Logger;

public class AzureStateMapper {

    private static final Logger LOGGER = Logger.getLogger(AzureStateMapper.class);

    private static final String ATTACHMENT_PLUGIN = AzureAttachmentPlugin.class.getSimpleName();
    private static final String COMPUTE_PLUGIN = AzureComputePlugin.class.getSimpleName();
    private static final String VOLUME_PLUGIN = AzureVolumePlugin.class.getSimpleName();
    private static final String NETWORK_PLUGIN = AzureNetworkPlugin.class.getSimpleName();
    
    public static final String ATTACHED_STATE = "Attached";
    public static final String CREATING_STATE = "Creating";
    public static final String SUCCEEDED_STATE = "Succeeded";
    public static final String UNATTACHED_STATE = "Unattached";
    public static final String FAILED_STATE = "Failed";

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
                    LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, ATTACHMENT_PLUGIN));
                    return InstanceState.INCONSISTENT;
            }
            case COMPUTE:
                // cloud state values: [creating, succeeded, failed]
                switch (state) {
                    case CREATING_STATE:
                        return InstanceState.CREATING;
                    case SUCCEEDED_STATE:
                        return InstanceState.READY;
                    case FAILED_STATE:
                        return InstanceState.FAILED;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, COMPUTE_PLUGIN));
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
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, VOLUME_PLUGIN));
                }
            case NETWORK:
                // cloud state values: [succeeded]
                switch (state) {
                    case SUCCEEDED_STATE:
                        return InstanceState.READY;
                    default:
                        LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, NETWORK_PLUGIN));
                        return InstanceState.INCONSISTENT;
                }
            default:
                LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }
    }

}
