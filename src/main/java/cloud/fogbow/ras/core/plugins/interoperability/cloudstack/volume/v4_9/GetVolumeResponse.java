package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import javax.validation.constraints.NotNull;
import java.util.List;

import static cloud.fogbow.common.constants.CloudStackConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listVolumes.html
 * <p>
 * Response example:
 * {
 * "listvolumesresponse": {
 * "volume": [{
 * "id": "dad76621-edcd-4968-a152-74d877d1961b",
 * "name": "ca43bccc-21a6-4f88-8fac-c88ea386a451",
 * "size": 1073741824,
 * "state": "Ready",
 * }]
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetVolumeResponse {
    @SerializedName(VOLUMES_KEY_JSON)
    private ListVolumesResponse listVolumesResponse;

    @NotNull
    public List<Volume> getVolumes() {
        return this.listVolumesResponse.volumes;
    }

    public static GetVolumeResponse fromJson(String json) throws HttpResponseException {
        GetVolumeResponse volumeResponse = GsonHolder.getInstance().fromJson(json, GetVolumeResponse.class);
        volumeResponse.listVolumesResponse.checkErrorExistence();
        return volumeResponse;
    }

    public class ListVolumesResponse extends CloudStackErrorResponse {
        @SerializedName(VOLUME_KEY_JSON)
        private List<Volume> volumes;
    }

    public class Volume {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(SIZE_KEY_JSON)
        private long size;
        @SerializedName(STATE_KEY_JSON)
        private String state;

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public long getSize() {
            return this.size;
        }

        public String getState() {
            return this.state;
        }
    }
}
