package org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Image.*;

import java.util.List;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/image/v2/
 */
public class GetAllImagesResponse {

	@SerializedName(IMAGES_KEY_JSON)
	private List<GetImageResponse> images;
	
	@SerializedName(NEXT_KEY_JSON)
	private String next;
	
	public List<GetImageResponse> getImages() {
		return this.images;
	}
	
	public String getNext() {
		return this.next;
	}
	
	public static GetAllImagesResponse fromJson(String json) {
		return GsonHolder.getInstance().fromJson(json, GetAllImagesResponse.class);
	}

}
