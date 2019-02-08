package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 *     {
 *         "security_group_rules": [
 *             {
 *                 "direction": "egress",
 *                 "ethertype": "IPv6",
 *                 "id": "3c0e45ff-adaf-4124-b083-bf390e5482ff",
 *                 "port_range_max": null,
 *                 "port_range_min": null,
 *                 "protocol": null,
 *                 "remote_group_id": null,
 *                 "remote_ip_prefix": null,
 *                 "security_group_id": "85cc3048-abc3-43cc-89b3-377341426ac5",
 *                 "project_id": "e4f50856753b4dc6afee5fa6b9b6c550",
 *                 "revision_number": 1,
 *                 "created_at": "2018-03-19T19:16:56Z",
 *                 "updated_at": "2018-03-19T19:16:56Z",
 *                 "tenant_id": "e4f50856753b4dc6afee5fa6b9b6c550",
 *                 "description": ""
 *             }
 *         ]
 *     }
 * </p>
 */
public class GetSecurityRulesResponse {
    @SerializedName(OpenStackConstants.Network.SECURITY_GROUP_RULES_KEY_JSON)
    private List<SecurityRules> securityRules;

    public static GetSecurityRulesResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetSecurityRulesResponse.class);
    }

    public class SecurityRules {
        @SerializedName(OpenStackConstants.Network.ID_KEY_JSON)
        private String id;
        @SerializedName(OpenStackConstants.Network.REMOTE_IP_PREFIX_KEY_JSON)
        private String cidr;
        @SerializedName(OpenStackConstants.Network.MIN_PORT_KEY_JSON)
        private int portFrom;
        @SerializedName(OpenStackConstants.Network.MAX_PORT_KEY_JSON)
        private int portTo;
        @SerializedName(OpenStackConstants.Network.DIRECTION_KEY_JSON)
        private String direction;
        @SerializedName(OpenStackConstants.Network.ETHER_TYPE_KEY_JSON)
        private String etherType;
        @SerializedName(OpenStackConstants.Network.PROTOCOL_KEY_JSON)
        private String protocol;

        public String getId() {
            return this.id;
        }

        public String getCidr() {
            return this.cidr;
        }

        public int getPortFrom() {
            return this.portFrom;
        }

        public int getPortTo() {
            return this.portTo;
        }

        public String getDirection() {
            return this.direction;
        }

        public String getEtherType() {
            return this.etherType;
        }

        public String getProtocol() {
            return this.protocol;
        }
    }

    public List<SecurityRules> getSecurityRules() {
        return this.securityRules;
    }
}
