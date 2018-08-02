package org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/
 */
public class VolumeResponse {

	@SerializedName(OpenstackRestApiConstants.Volume.VOLUME_KEY_JSON)
	private Volume volume;
	
	public Volume getVolume() {
		return this.volume;
	}
	
	public String getId() {
		return volume.id;
	}

	public static VolumeResponse fromJson(String json) {
		return GsonHolder.getInstance().fromJson(json, VolumeResponse.class);
	}

	public String getName() {
		return volume.name;
	}

	public Integer getSize() {
		return volume.size;
	}

	public String getStatus() {
		return volume.status;
	}

	public class Volume {

		@SerializedName(OpenstackRestApiConstants.Volume.ID_KEY_JSON)
		private String id;

		@SerializedName(OpenstackRestApiConstants.Volume.NAME_KEY_JSON)
		private String name;

		@SerializedName(OpenstackRestApiConstants.Volume.SIZE_KEY_JSON)
		private Integer size;

		@SerializedName(OpenstackRestApiConstants.Volume.STATUS_KEY_JSON)
		private String status;

	}
}
