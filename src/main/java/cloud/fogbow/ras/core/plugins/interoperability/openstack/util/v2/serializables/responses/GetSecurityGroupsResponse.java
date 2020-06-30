package cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import static cloud.fogbow.common.constants.OpenStackConstants.Network.ID_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Network.SECURITY_GROUPS_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/?expanded=list-security-groups-detail#list-security-groups
 *
 * {
 *     "security_groups": [
 *         {
 *             "id": "85cc3048-abc3-43cc-89b3-377341426ac5"
 *         }
 *     ]
 * }
 */
public class GetSecurityGroupsResponse {

    @SerializedName(SECURITY_GROUPS_KEY_JSON)
    private List<SecurityGroup> securityGroups;

    public List<SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<SecurityGroup> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public static GetSecurityGroupsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetSecurityGroupsResponse.class);
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
