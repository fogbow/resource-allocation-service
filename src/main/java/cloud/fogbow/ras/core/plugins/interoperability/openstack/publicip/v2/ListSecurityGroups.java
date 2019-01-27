package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import static cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.ID_KEY_JSON;
import static cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.SECURITY_GROUPS_KEY_JSON;

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
