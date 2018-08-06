package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 *
 * Request Example:
 * {
 *   "security_group":{
 *     "name":"net1",
 *     "tenant_id":"fake-tenant"
 *   }
 * }
 *
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateSecurityGroupRequest {

    @SerializedName(SECURITY_GROUP_KEY_JSON)
    private final SecurityGroup securityGroup;

    private CreateSecurityGroupRequest(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class SecurityGroup {

        @SerializedName(NAME_KEY_JSON)
        private String name;

        @SerializedName(TENANT_ID_KEY_JSON)
        private String tenantId;

        private SecurityGroup(Builder builder) {
            this.name = builder.name;
            this.tenantId = builder.tenantId;
        }

    }

    public static class Builder {

        private String name;
        private String tenantId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CreateSecurityGroupRequest build() {
            SecurityGroup securityGroup = new SecurityGroup(this);
            return new CreateSecurityGroupRequest(securityGroup);
        }

    }

}
