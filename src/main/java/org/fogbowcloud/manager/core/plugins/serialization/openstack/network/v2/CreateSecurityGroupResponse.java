package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

public class CreateSecurityGroupResponse {

    @SerializedName(SECURITY_GROUP_KEY_JSON)
    private SecurityGroup securityGroup;

    public CreateSecurityGroupResponse(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public String getId() {
        return securityGroup.id;
    }

    public static CreateSecurityGroupResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateSecurityGroupResponse.class);
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class SecurityGroup {

        @SerializedName(ID_KEY_JSON)
        private String id;

        public SecurityGroup(String id) {
            this.id = id;
        }

    }

}
