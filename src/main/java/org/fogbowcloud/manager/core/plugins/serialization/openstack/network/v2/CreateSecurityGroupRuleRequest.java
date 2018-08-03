package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

public class CreateSecurityGroupRuleRequest {

    @SerializedName(SECURITY_GROUP_RULE_KEY_JSON)
    private SecurityGroupRule securityGroupRule;

    public CreateSecurityGroupRuleRequest(SecurityGroupRule securityGroupRule) {
        this.securityGroupRule = securityGroupRule;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class SecurityGroupRule {

        @SerializedName(DIRECTION_KEY_JSON)
        private String direction;

        @SerializedName(SECURITY_GROUP_ID_KEY_JSON)
        private String securityGroupId;

        @SerializedName(REMOTE_IP_PREFIX_KEY_JSON)
        private String remoteIpPrefix;

        @SerializedName(PROTOCOL_KEY_JSON)
        private String protocol;

        @SerializedName(MIN_PORT_KEY_JSON)
        private int minPort;

        @SerializedName(MAX_PORT_KEY_JSON)
        private int maxPort;

        public SecurityGroupRule(Builder builder) {
            this.direction = builder.direction;
            this.securityGroupId = builder.securityGroupId;
            this.remoteIpPrefix = builder.remoteIpPrefix;
            this.protocol = builder.protocol;
            this.minPort = builder.minPort;
            this.maxPort = builder.maxPort;
        }

    }

    public static class Builder {

        private String direction;
        private String securityGroupId;
        private String remoteIpPrefix;
        private String protocol;
        private int minPort;
        private int maxPort;

        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder securityGroupId(String securityGroupId) {
            this.securityGroupId = securityGroupId;
            return this;
        }

        public Builder remoteIpPrefix(String remoteIpPrefix) {
            this.remoteIpPrefix = remoteIpPrefix;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder minPort(int minPort) {
            this.minPort = minPort;
            return this;
        }

        public Builder maxPort(int maxPort) {
            this.maxPort = maxPort;
            return this;
        }

        public CreateSecurityGroupRuleRequest build() {
            SecurityGroupRule securityGroupRule = new SecurityGroupRule(this);
            return new CreateSecurityGroupRuleRequest(securityGroupRule);

        }
    }

}
