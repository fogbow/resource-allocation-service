package org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Volume.PROJECT_ID_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/?expanded=list-all-volume-types-for-v2-detail
 * <p>
 * Request example:
 * {
 *   "project_id": "fogbow-atm"
 * }
 */
public class CreateAllTypesRequest implements JsonSerializable {
    @SerializedName(PROJECT_ID_KEY_JSON)
    private String projectId;

    private CreateAllTypesRequest(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Builder {
        private String projectId;

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public CreateAllTypesRequest build() {
            return new CreateAllTypesRequest(this.projectId);
        }
    }
}
