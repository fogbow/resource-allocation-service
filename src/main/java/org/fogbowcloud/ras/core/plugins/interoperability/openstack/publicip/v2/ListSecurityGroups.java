package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.SECURITY_GROUPS_KEY_JSON;

public class ListSecurityGroups {

    @SerializedName(SECURITY_GROUPS_KEY_JSON)
    private List<SecurityGroup> securityGroups;

    public List<SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<SecurityGroup> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public static ListSecurityGroups fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, ListSecurityGroups.class);
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class SecurityGroup {

        @SerializedName(ID_KEY_JSON)
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

}
