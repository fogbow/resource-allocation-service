package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securityrule.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.SECURITY_GROUPS_KEY_JSON;

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

    @SerializedName(SECURITY_GROUPS_KEY_JSON)
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
