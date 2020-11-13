package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.image.models;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;

public class CreateImageResponse {
	@SerializedName(GoogleCloudConstants.Image.DISK_SIZE_KEY_JSON)
	private String diskSize;
	@SerializedName(GoogleCloudConstants.Image.STATUS_KEY_JSON)
	private String status;
	@SerializedName(GoogleCloudConstants.Image.ID_KEY_JSON)
	private String id;
	@SerializedName(GoogleCloudConstants.Image.NAME_KEY_JSON)
	private String name;

	public static CreateImageResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateImageResponse.class);
    }

	public String getDiskSize() {
		return diskSize;
	}

	public void setDiskSize(String diskSize) {
		this.diskSize = diskSize;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
