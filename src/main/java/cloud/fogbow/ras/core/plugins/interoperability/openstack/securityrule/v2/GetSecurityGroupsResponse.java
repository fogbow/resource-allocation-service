package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/?expanded=list-security-groups-detail#list-security-groups
 *
 * {
 *     "security_groups": [
 *         {
 *             "id": "85cc3048-abc3-43cc-89b3-377341426ac5",
 *         }
 *     ]
 * }
 */
public class GetSecurityGroupsResponse {

    @SerializedName(OpenStackConstants.Network.SECURITY_GROUPS_KEY_JSON)
    private List<SecurityGroup> securityGroups;

    public List<SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public static GetSecurityGroupsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetSecurityGroupsResponse.class);
    }

    public class SecurityGroup {

        private String id;

        public String getId() {
            return id;
        }

    }

}
