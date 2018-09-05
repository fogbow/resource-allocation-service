package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.image;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

/**
 * Documentation:
 *
 * Response example:
 * {
 *     "listtemplatesresponse": {
 *         "count": 24,
 *         "template": [{
 *             "id": "7597600a-0f98-4e5e-8c96-5f62820b3b0f",
 *             "name": "CentOS 7 Minimal",
 *             "size": 26843545600,
 *         }]
 *     }
 * }
 */
public class GetAllImagesResponse {

    @SerializedName("listtemplatesresponse")
    private ListTemplatesResponse response;

    public static GetAllImagesResponse fromJson(String jsonResponse) {
        return GsonHolder.getInstance().fromJson(jsonResponse, GetAllImagesResponse.class);
    }

    public List<Image> getImages() {
        return response.images;
    }

    private class ListTemplatesResponse {

        @SerializedName("template")
        private List<Image> images;

    }

    public class Image {

        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("size")
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
