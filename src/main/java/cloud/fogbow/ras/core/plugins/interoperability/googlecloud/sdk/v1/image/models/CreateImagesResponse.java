package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.image.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;

public class CreateImagesResponse {
	
	@SerializedName(GoogleCloudConstants.Image.ITEMS_KEY_JSON)
    private List<CreateImageResponse> images;
	@SerializedName(GoogleCloudConstants.Image.NEXT_TOKEN_KEY_JSON)
	private String nextToken;
	
	public static CreateImagesResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateImagesResponse.class);
    }
	
	public List<CreateImageResponse> getImages() {
        return this.images;
    }

	public String getNextToken() {
		return nextToken;
	}
}
