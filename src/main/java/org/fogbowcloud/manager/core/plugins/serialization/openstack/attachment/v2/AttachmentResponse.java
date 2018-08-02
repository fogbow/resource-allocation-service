package org.fogbowcloud.manager.core.plugins.serialization.openstack.attachment.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Attachment.*;


/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 */
public class AttachmentResponse {

	@SerializedName(VOLUME_ATTACHMENT_KEY_JSON)
	private Attachment attachment;

	public String getId() {
		return attachment.id;
	}

	public String getVolumeId() {
		return attachment.volumeId;
	}

	public String getServerId() {
		return attachment.serverId;
	}

	public String getDevice() {
		return attachment.device;
	}

	public Attachment getAttachment() {
		return attachment;
	}

	public static AttachmentResponse fromJson(String jsonStr) {
		return GsonHolder.getInstance().fromJson(jsonStr, AttachmentResponse.class);
	}

	public class Attachment {

		@SerializedName(ID_KEY_JSON)
		private String id;

		@SerializedName(VOLUME_ID_KEY_JSON)
		private String volumeId;

		@SerializedName(SERVER_ID_KEY_JSON)
		private String serverId;

		@SerializedName(DEVICE_KEY_JSON)
		private String device;

	}

}
