package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRestApiConstants.Volume.*;

public class GetVolumeResponse {

    @SerializedName(VOLUMES_KEY_JSON)
    private ListVolumeResponse volumesResponse;

    public class ListVolumeResponse {
        @SerializedName(VOLUME_KEY_JSON)
        private List<Volume> volumes;
    }

    private Volume volume;

    public class Volume {
        @SerializedName(DISK_KEY_JSON)
        private int diskSize;

        public int getDiskSize() {
            return diskSize;
        }
    }

    public List<Volume> getVolumes() {
        return volumesResponse.volumes;
    }

    public static GetVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetVolumeResponse.class);
    }
}
