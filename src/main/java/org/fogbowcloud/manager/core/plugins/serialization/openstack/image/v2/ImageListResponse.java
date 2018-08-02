package org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Image.*;

import java.util.List;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/image/v2/
 */
public class ImageListResponse {

	@SerializedName(IMAGES_KEY_JSON)
	private List<ImageResponse> imageResponseList;
	
	@SerializedName(NEXT_KEY_JSON)
	private String next;
	
	public List<ImageResponse> getImageResponseList() {
		return this.imageResponseList;
	}
	
	public String getNext() {
		return this.next;
	}
	
	public static ImageListResponse fromJson(String json) {
		return GsonHolder.getInstance().fromJson(json, ImageListResponse.class);
	}

}
