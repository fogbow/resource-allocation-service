package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securitygroup.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.ID_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 *     {
 *       "security_group_rule": {
 *           "direction": "ingress",
 *           "ethertype": "IPv4",
 *           "id": "2bc0accf-312e-429a-956e-e4407625eb62",
 *           "port_range_max": 80,
 *           "port_range_min": 80,
 *           "protocol": "tcp",
 *           "remote_group_id": "85cc3048-abc3-43cc-89b3-377341426ac5",
 *           "remote_ip_prefix": null,
 *           "security_group_id": "a7734e61-b545-452d-a3cd-0189cbd9747a",
 *           "project_id": "e4f50856753b4dc6afee5fa6b9b6c550",
 *           "revision_number": 1,
 *           "tenant_id": "e4f50856753b4dc6afee5fa6b9b6c550",
 *           "created_at": "2018-03-19T19:16:56Z",
 *           "updated_at": "2018-03-19T19:16:56Z",
 *           "description": ""
 *       }
 *     }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateSecurityGroupRuleResponse {
    public SecurityGroupRule securityGroupRule;

    public CreateSecurityGroupRuleResponse(SecurityGroupRule securityGroupRule) {
        this.securityGroupRule = securityGroupRule;
    }

    public String getId() {
        return securityGroupRule.id;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static CreateSecurityGroupRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateSecurityGroupRuleResponse.class);
    }

    public static class SecurityGroupRule {
        @SerializedName(ID_KEY_JSON)
        private String id;

        public SecurityGroupRule(String id) {
            this.id = id;
        }
    }
}
