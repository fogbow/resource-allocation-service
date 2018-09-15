package org.fogbowcloud.ras.core.plugins.interoperability.openstack.attachment.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Attachment.VOLUME_ATTACHMENT_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Attachment.VOLUME_ID_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Request example:
 * {
 * "volumeAttachment": {
 * "volumeId": "a26887c6-c47b-4654-abb5-dfadf7d3f803"
 * }
 * }
 */
public class CreateAttachmentRequest implements JsonSerializable {
    @SerializedName(VOLUME_ATTACHMENT_KEY_JSON)
    private Attachment attachment;

    private CreateAttachmentRequest(Attachment attachment) {
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

        public CreateAttachmentRequest build() {
            Attachment attachment = new Attachment(this);
            return new CreateAttachmentRequest(attachment);
        }
    }
}
