package org.fogbowcloud.manager.core.plugins.serialization.openstack.attachment.v2;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2.OpenstackApiConstants;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 */
public class AttachmentResponse {

	@SerializedName(OpenstackApiConstants.Attachment.VOLUME_ATTACHMENT_KEY_JSON)
	private AttachmentParameters attachmentParameters;

	public AttachmentResponse() {}

	public AttachmentResponse(AttachmentParameters attachmentParameters) {
		this.attachmentParameters = attachmentParameters;
	}

	public class AttachmentParameters {

		@SerializedName(OpenstackApiConstants.Attachment.ID_KEY_JSON)
		private final String id;

		@SerializedName(OpenstackApiConstants.Attachment.VOLUME_ID_KEY_JSON)
		private final String volumeId;

		@SerializedName(OpenstackApiConstants.Attachment.SERVER_ID_KEY_JSON)
		private final String serverId;

		@SerializedName(OpenstackApiConstants.Attachment.DEVICE_KEY_JSON)
		private final String device;

		public AttachmentParameters(Builder builder) {
			super();
			this.id = builder.id;
			this.volumeId = builder.volumeId;
			this.serverId = builder.serverId;
			this.device = builder.device;
		}

		public String getId() {
			return id;
		}

		public String getVolumeId() {
			return volumeId;
		}

		public String getServerId() {
			return serverId;
		}

		public String getDevice() {
			return device;
		}

	}

	public class Builder {

		private String id;
		private String volumeId;
		private String serverId;
		private String device;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder volumeId(String volumeId) {
			this.volumeId = volumeId;
			return this;
		}

		public Builder serverId(String serverId) {
			this.serverId = serverId;
			return this;
		}

		public Builder device(String device) {
			this.device = device;
			return this;
		}

		public AttachmentResponse build() {
			AttachmentParameters attachmentParameters = new AttachmentParameters(this);
			return new AttachmentResponse(attachmentParameters);
		}

	}

	public AttachmentParameters getAttachmentParameters() {
		return attachmentParameters;
	}

	public AttachmentResponse fromJson(String jsonStr) {
		return GsonHolder.getInstance().fromJson(jsonStr, AttachmentResponse.class);
	}

}
