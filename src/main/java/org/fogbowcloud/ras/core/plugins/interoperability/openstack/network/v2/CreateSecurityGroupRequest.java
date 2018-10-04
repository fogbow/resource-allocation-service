package org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 * Request Example:
 * {
 * "security_group":{
 * "name":"net1",
 * "project_id":"fake-project"
 * }
 * }
 * <p>
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
        @SerializedName(PROJECT_ID_KEY_JSON)
        private String projectId;

        private SecurityGroup(Builder builder) {
            this.name = builder.name;
            this.projectId = builder.projectId;
        }
    }

    public static class Builder {
        private String name;
        private String projectId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public CreateSecurityGroupRequest build() {
            SecurityGroup securityGroup = new SecurityGroup(this);
            return new CreateSecurityGroupRequest(securityGroup);
        }
    }
}
