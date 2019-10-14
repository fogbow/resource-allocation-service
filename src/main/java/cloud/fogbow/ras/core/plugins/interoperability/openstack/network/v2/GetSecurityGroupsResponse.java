package cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetSecurityGroupsResponse {

    @SerializedName(OpenStackConstants.Network.SECURITY_GROUPS_KEY_JSON)
    private List<GetSecurityGroupsResponse.SecurityGroup> securityGroups;

    public List<GetSecurityGroupsResponse.SecurityGroup> getSecurityGroups() {
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
