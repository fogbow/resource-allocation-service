package org.fogbowcloud.ras.core.plugins.interoperability.openstack.attachment.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Attachment.*;


/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Response example:
 * {
 * "volumeAttachment": {
 * "device": "/dev/sdd",
 * "id": "a26887c6-c47b-4654-abb5-dfadf7d3f803",
 * "serverId": "2390fb4d-1693-45d7-b309-e29c4af16538",
 * "volumeId": "a26887c6-c47b-4654-abb5-dfadf7d3f803"
 * }
 * }
 */
public class GetAttachmentResponse {
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

    public static GetAttachmentResponse fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, GetAttachmentResponse.class);
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
