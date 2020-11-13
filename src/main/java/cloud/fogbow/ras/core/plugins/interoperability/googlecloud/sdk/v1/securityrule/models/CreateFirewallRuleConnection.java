package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import com.google.gson.annotations.SerializedName;

public class CreateFirewallRuleConnection {

    @SerializedName(GoogleCloudConstants.Network.Firewall.IP_PROTOCOL_KEY_JSON)
    private String ipProtocol;
    @SerializedName(GoogleCloudConstants.Network.Firewall.PORT_KEY_JSON)
    private String[] ports;

    public CreateFirewallRuleConnection(Connection connection) {
        this.ipProtocol = connection.ipProtocol;
        this.ports = connection.ports;
    }

    protected static class ConnectionBuilder {
        private String ipProtocol;
        private String[] ports;

        protected ConnectionBuilder ipProtocol(String ipProtocol) {
            this.ipProtocol = ipProtocol;
            return this;
        }

        protected ConnectionBuilder ports(String[] ports) {
            this.ports = ports;
            return this;
        }

        protected CreateFirewallRuleConnection build() {
            Connection connection = new Connection(this);
            return new CreateFirewallRuleConnection(connection);
        }
    }

    protected static class Connection {
        private String ipProtocol;
        private String[] ports;

        private Connection(ConnectionBuilder builder){
            this.ipProtocol = builder.ipProtocol;
            this.ports = builder.ports;
        }
    }
}
