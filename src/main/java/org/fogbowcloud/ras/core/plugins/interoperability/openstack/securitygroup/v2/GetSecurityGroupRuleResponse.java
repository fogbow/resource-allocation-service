package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securitygroup.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 *     {
 *       "security_group_rule": {
 *           "direction": "egress",
 *           "ethertype": "IPv6",
 *           "id": "3c0e45ff-adaf-4124-b083-bf390e5482ff",
 *           "port_range_max": null,
 *           "port_range_min": null,
 *           "protocol": null,
 *           "remote_group_id": null,
 *           "remote_ip_prefix": null,
 *           "revision_number": 1,
 *           "created_at": "2018-03-19T19:16:56Z",
 *           "updated_at": "2018-03-19T19:16:56Z",
 *           "security_group_id": "85cc3048-abc3-43cc-89b3-377341426ac5",
 *           "project_id": "e4f50856753b4dc6afee5fa6b9b6c550",
 *           "tenant_id": "e4f50856753b4dc6afee5fa6b9b6c550"
 *       }
 *     }
 * </p>
 */
public class GetSecurityGroupRuleResponse {
    @SerializedName(SECURITY_GROUP_RULE_KEY_JSON)
    private SecurityGroupRule securityGroupRule;

    public static GetSecurityGroupRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetSecurityGroupRuleResponse.class);
    }

    public class SecurityGroupRule {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(REMOTE_IP_PREFIX_KEY_JSON)
        private String cidr;
        @SerializedName(MIN_PORT_KEY_JSON)
        private int portFrom;
        @SerializedName(MAX_PORT_KEY_JSON)
        private int portTo;
        @SerializedName(DIRECTION_KEY_JSON)
        private String direction;
        @SerializedName(ETHER_TYPE_KEY_JSON)
        private String etherType;
        @SerializedName(PROTOCOL_KEY_JSON)
        private String protocol;
    }

    public String getId() {
        return securityGroupRule.id;
    }

    public String getCidr() {
        return securityGroupRule.cidr;
    }

    public int getPortFrom() {
        return securityGroupRule.portFrom;
    }

    public int getPortTo() {
        return securityGroupRule.portTo;
    }

    public String getDirection() {
        return securityGroupRule.direction;
    }

    public String getEtherType() {
        return securityGroupRule.etherType;
    }

    public String getProtocol() {
        return securityGroupRule.protocol;
    }
}
