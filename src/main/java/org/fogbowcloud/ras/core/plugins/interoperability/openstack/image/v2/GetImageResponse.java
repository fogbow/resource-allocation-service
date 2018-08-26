package org.fogbowcloud.ras.core.plugins.interoperability.openstack.image.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Image.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/image/v2/
 * <p>
 * Response example:
 * {
 * "status": "active",
 * "name": "cirros-0.3.2-x86_64-disk",
 * "container_format": "bare",
 * "disk_format": "qcow2",
 * "visibility": "public",
 * "min_disk": 0,
 * "id": "1bea47ed-f6a9-463b-b423-14b9cca9ad27",
 * "owner": "5ef70662f8b34079a6eddb8da9d75fe8",
 * "size": 13167616,
 * "min_ram": 0
 * }
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