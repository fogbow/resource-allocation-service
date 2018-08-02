package org.fogbowcloud.manager.core.plugins.serialization.openstack.attachment.v2;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.JsonSerializable;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2.OpenstackApiConstants;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 */
public class CreateRequest implements JsonSerializable {

	@SerializedName(OpenstackApiConstants.Attachment.VOLUME_ATTACHMENT_KEY_JSON)
	private AttachmentParameters attachmentParameters;
	
	public CreateRequest() {}
	
	private CreateRequest(AttachmentParameters attachmentParameters) {
		this.attachmentParameters = attachmentParameters;
	}
	
	public class AttachmentParameters {
		
		@SerializedName(OpenstackApiConstants.Attachment.VOLUME_ID_KEY_JSON)
		private final String volumeId;

		public AttachmentParameters(Builder builder) {
			super();
			this.volumeId = builder.volumeId;
		}
		
	}
	
	public class Builder {
		
		private String volumeId;
		
        public Builder volumeId(String volumeId) {
            this.volumeId = volumeId;
            return this;
        }
        
        public CreateRequest build() {
            AttachmentParameters attachmentParameters = new AttachmentParameters(this);
			return new CreateRequest(attachmentParameters);
        }        
		
	}
	
	@Override
	public String toJson() {
		return GsonHolder.getInstance().toJson(this);
	}
	
}
