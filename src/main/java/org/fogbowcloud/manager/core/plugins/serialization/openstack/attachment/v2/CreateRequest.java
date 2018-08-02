package org.fogbowcloud.manager.core.plugins.serialization.openstack.attachment.v2;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.JsonSerializable;

import com.google.gson.annotations.SerializedName;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Attachment.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 */
public class CreateRequest implements JsonSerializable {

	@SerializedName(VOLUME_ATTACHMENT_KEY_JSON)
	private Attachment attachment;
	
	private CreateRequest(Attachment attachment) {
		this.attachment = attachment;
	}

	@Override
	public String toJson() {
		return GsonHolder.getInstance().toJson(this);
	}

	public static class Attachment {

		@SerializedName(VOLUME_ID_KEY_JSON)
		private final String volumeId;

		public Attachment(Builder builder) {
			this.volumeId = builder.volumeId;
		}

	}

	public static class Builder {

		private String volumeId;

		public Builder volumeId(String volumeId) {
			this.volumeId = volumeId;
			return this;
		}

		public CreateRequest build() {
			Attachment attachment = new Attachment(this);
			return new CreateRequest(attachment);
		}

	}

}
