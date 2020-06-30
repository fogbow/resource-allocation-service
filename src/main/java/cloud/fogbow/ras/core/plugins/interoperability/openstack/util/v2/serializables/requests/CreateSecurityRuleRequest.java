package cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 *     {
 *         "security_group_rule": {
 *             "direction": "ingress",
 *             "port_range_min": "80",
 *             "ethertype": "IPv4",
 *             "port_range_max": "80",
 *             "protocol": "tcp",
 *             "remote_group_id": "85cc3048-abc3-43cc-89b3-377341426ac5",
 *             "security_group_id": "a7734e61-b545-452d-a3cd-0189cbd9747a"
 *         }
 *     }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateSecurityRuleRequest {
    @SerializedName(OpenStackConstants.Network.SECURITY_GROUP_RULE_KEY_JSON)
    private SecurityRule securityRule;

    public CreateSecurityRuleRequest(SecurityRule securityRule) {
        this.securityRule = securityRule;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class SecurityRule {
        @SerializedName(OpenStackConstants.Network.SECURITY_GROUP_ID_KEY_JSON)
        private String securityGroupId;
        @SerializedName(OpenStackConstants.Network.REMOTE_IP_PREFIX_KEY_JSON)
        private String remoteIpPrefix;
        @SerializedName(OpenStackConstants.Network.MIN_PORT_KEY_JSON)
        private int portRangeMin;
        @SerializedName(OpenStackConstants.Network.MAX_PORT_KEY_JSON)
        private int portRangeMax;
        @SerializedName(OpenStackConstants.Network.DIRECTION_KEY_JSON)
        private String direction;
        @SerializedName(OpenStackConstants.Network.ETHER_TYPE_KEY_JSON)
        private String etherType;
        @SerializedName(OpenStackConstants.Network.PROTOCOL_KEY_JSON)
        private String protocol;

        private SecurityRule(Builder builder) {
            this.securityGroupId = builder.securityGroupId;
            this.remoteIpPrefix =  builder.remoteIpPrefix;
            this.portRangeMin = builder.portRangeMin;
            this.portRangeMax = builder.portRangeMax;
            this.direction = builder.direction;
            this.etherType = builder.etherType;
            this.protocol = builder.protocol;
        }
    }

    public static class Builder {
        private String securityGroupId;
        private String remoteIpPrefix;
        private int portRangeMin;
        private int portRangeMax;
        private String direction;
        private String etherType;
        private String protocol;

        public Builder securityGroupId(String securityGroupId) {
            this.securityGroupId = securityGroupId;
            return this;
        }

        public Builder remoteIpPrefix(String remoteIpPrefix) {
            this.remoteIpPrefix = remoteIpPrefix;
            return this;
        }

        public Builder portRangeMin(int portRangeMin) {
            this.portRangeMin = portRangeMin;
            return this;
        }

        public Builder portRangeMax(int portRangeMax) {
            this.portRangeMax = portRangeMax;
            return this;
        }

        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder etherType(String etherType) {
            this.etherType = etherType;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public CreateSecurityRuleRequest build() {
            SecurityRule securityRule = new SecurityRule(this);
            return new CreateSecurityRuleRequest(securityRule);
        }
    }
}
