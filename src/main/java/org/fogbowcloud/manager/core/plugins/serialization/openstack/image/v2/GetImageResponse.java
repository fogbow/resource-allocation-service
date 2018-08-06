package org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Image.*;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/image/v2/
 */
public class GetImageResponse {
	
	@SerializedName(ID_KEY_JSON)
	private String id;
	
	@SerializedName(STATUS_KEY_JSON)
	private String status;
	
	@SerializedName(NAME_KEY_JSON)
	private String name;
	
	@SerializedName(SIZE_KEY_JSON)
	private Long size;
	
	@SerializedName(MIN_DISK_KEY_JSON)
	private Long minDisk;
	
	@SerializedName(MIN_RAM_KEY_JSON)
	private Long minRam;
	
	@SerializedName(VISIBILITY_KEY_JSON)
	private String visibility;
			
	@SerializedName(OWNER_KEY_JSON)
	private String owner;		
	
	public String getId() {
		return this.id;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Long getSize() {
		return this.size;
	}
	
	public Long getMinDisk() {
		return this.minDisk;
	}
	
	public Long getMinRam() {
		return this.minRam;
	}
	
	public String getVisibility() {
		return this.visibility;
	}
	
	public String getOwner() { 
		return this.owner;
	}
	
	public static GetImageResponse fromJson(String json) {
		return GsonHolder.getInstance().fromJson(json, GetImageResponse.class);
	}
}