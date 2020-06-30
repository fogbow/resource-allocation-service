package cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Compute.NAME_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Compute.REMOVE_SECURITY_GROUP_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/#add-security-group-to-a-server-addsecuritygroup-action
 * <p>
 * Request Example:
 * {
 *   "removeSecurityGroup": {
 *     "name": "test"
 *   }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class RemoveSecurityGroupFromServerRequest {
    @SerializedName(REMOVE_SECURITY_GROUP_KEY_JSON)
    private final RemoveSecurityGroup securityGroup;

    private RemoveSecurityGroupFromServerRequest(RemoveSecurityGroup removeSecurityGroup) {
        this.securityGroup = removeSecurityGroup;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class RemoveSecurityGroup {
        @SerializedName(NAME_KEY_JSON)
        private String name;

        private RemoveSecurityGroup(Builder builder) {
            this.name = builder.name;
        }
    }

    public static class Builder {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public RemoveSecurityGroupFromServerRequest build() {
            RemoveSecurityGroup securityGroup = new RemoveSecurityGroup(this);
            return new RemoveSecurityGroupFromServerRequest(securityGroup);
        }
    }
}
