package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import javax.validation.constraints.NotNull;
import java.util.List;

import static cloud.fogbow.common.constants.CloudStackConstants.Image.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listTemplates.html
 *
 * <p>
 * Response example:
 * {
 * "listtemplatesresponse": {
 * "count": 24,
 * "template": [{
 * "id": "7597600a-0f98-4e5e-8c96-5f62820b3b0f",
 * "name": "CentOS 7 Minimal",
 * "size": 26843545600,
 * }]
 * }
 * }
 */
public class GetAllImagesResponse {

    @SerializedName(LIST_TEMPLATES_KEY_JSON)
    private ListTemplatesResponse response;

    @NotNull
    public static GetAllImagesResponse fromJson(String jsonResponse) throws HttpResponseException {
        GetAllImagesResponse getAllImagesResponse =
                GsonHolder.getInstance().fromJson(jsonResponse, GetAllImagesResponse.class);
        getAllImagesResponse.response.checkErrorExistence();
        return getAllImagesResponse;
    }

    @NotNull
    public List<Image> getImages() {
        return response.images;
    }

    private class ListTemplatesResponse extends CloudStackErrorResponse {

        @SerializedName(TEMPLATE_KEY_JSON)
        private List<Image> images;

    }

    public class Image {

        @SerializedName(ID_KEY_JSON)
        private String id;

        @SerializedName(NAME_KEY_JSON)
        private String name;

        @SerializedName(SIZE_KEY_JSON)
        private long size;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

    }

}
