package cloud.fogbow.ras.core.plugins.interoperability.aws;

import org.apache.log4j.Logger;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;

public class AwsV2StateMapper {

	private static final Logger LOGGER = Logger.getLogger(AwsV2StateMapper.class);
	
	private static final String ATTACHMENT_PLUGIN = "AwsV2AttachmentPlugin";
	private static final String VOLUME_PLUGIN = "AwsV2VolumePlugin";
	
	public static final String ATTACHED_STATE = "attached";
	public static final String DEFAULT_ERROR_STATE = "error";
	public static final String VOLUME_AVAILABLE_STATE = "available";
	public static final String VOLUME_IN_USE_STATE = "in-use";

	
	public static InstanceState map(ResourceType type, String state) {
		state = state.toLowerCase();
		switch (type) {
		case ATTACHMENT:
			// cloud state values: [attaching, attached, detaching, detached, busy]
			switch (state) {
			case ATTACHED_STATE:
				return InstanceState.READY;
			case DEFAULT_ERROR_STATE:
				return InstanceState.FAILED;
			default:
				LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, ATTACHMENT_PLUGIN));
				return InstanceState.BUSY;
			}
		case VOLUME:
			// cloud state values: [creating, available, in-use, deleting, deleted, error]
			switch (state) {
			case VOLUME_AVAILABLE_STATE:
				return InstanceState.READY;
			case VOLUME_IN_USE_STATE:
				return InstanceState.READY;
			case DEFAULT_ERROR_STATE:
				return InstanceState.FAILED;
			default:
				LOGGER.error(String.format(Messages.Error.UNDEFINED_INSTANCE_STATE_MAPPING, state, VOLUME_PLUGIN));
				return InstanceState.BUSY;
			}
		default:
			LOGGER.error(Messages.Error.INSTANCE_TYPE_NOT_DEFINED);
			return InstanceState.INCONSISTENT;
		}
	}
}