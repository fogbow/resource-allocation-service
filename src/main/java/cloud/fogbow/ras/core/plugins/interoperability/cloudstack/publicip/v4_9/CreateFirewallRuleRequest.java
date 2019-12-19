package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.*;
import static cloud.fogbow.common.constants.CloudStackConstants.SecurityGroup.CREATE_FIREWALL_RULE_COMMAND;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/createFirewallRule.html
 *
 * Request Example: {url_cloudstack}?command=createFirewallRule&response=json&protocol={protocol} /
 * &startport={startPort}&endport={endPort}&ipaddressid={ipAddressId}&cidrlist={cird}
 */
public class CreateFirewallRuleRequest extends CloudStackRequest {

    protected CreateFirewallRuleRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);

        addParameter(PROTOCOL_KEY_JSON, builder.protocol);
        addParameter(STARTPORT_KEY_JSON, builder.startPort);
        addParameter(ENDPORT_KEY_JSON, builder.endPort);
        addParameter(IP_ADDRESS_ID_KEY_JSON, builder.ipAddressId);
        addParameter(CIDR_LIST_KEY_JSON, builder.cidrList);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return CREATE_FIREWALL_RULE_COMMAND;
    }

    public static class Builder {
        private String cloudStackUrl;
        private String protocol;
        private String startPort;
        private String endPort;
        private String ipAddressId;
        private String cidrList;

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder startPort(String startPort) {
            this.startPort = startPort;
            return this;
        }

        public Builder endPort(String endPort) {
            this.endPort = endPort;
            return this;
        }

        public Builder ipAddressId(String ipAddressId) {
            this.ipAddressId = ipAddressId;
            return this;
        }

        public Builder cidrList(String cidrlist) {
            this.cidrList = cidrlist;
            return this;
        }

        public CreateFirewallRuleRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new CreateFirewallRuleRequest(this);
        }
    }
}
