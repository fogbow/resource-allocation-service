package org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 * Request Example:
 * {
 * "subnet":{
 * "name":"subnet-name-01",
 * "project_id":"fake-project",
 * "network_id":"d32019d3-bc6e-4319-9c1d-6722fc136a22",
 * "ip_version":4,
 * "gateway_ip":"192.0.0.1",
 * "cidr":"192.168.199.0/24",
 * "enable_dhcp":true,
 * "dns_nameservers":[
 * <p>
 * ]
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateSubnetRequest {
    @SerializedName(SUBNET_KEY_JSON)
    private final Subnet subnet;

    public CreateSubnetRequest(Subnet subnet) {
        this.subnet = subnet;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Subnet {
        @SerializedName(NAME_KEY_JSON)
        private final String name;
        @SerializedName(PROJECT_ID_KEY_JSON)
        private final String projectId;
        @SerializedName(NETWORK_ID_KEY_JSON)
        private final String networkId;
        @SerializedName(IP_VERSION_KEY_JSON)
        private final int ipVersion;
        @SerializedName(GATEWAY_IP_KEY_JSON)
        private final String gatewayIp;
        @SerializedName(CIDR_KEY_JSON)
        private final String networkAddress;
        @SerializedName(ENABLE_DHCP_KEY_JSON)
        private final boolean dhcpEnabled;
        @SerializedName(DNS_NAMESERVERS_KEY_JSON)
        private final List<String> dnsNameServers;

        private Subnet(Builder builder) {
            this.name = builder.name;
            this.projectId = builder.projectId;
            this.networkId = builder.networkId;
            this.ipVersion = builder.ipVersion;
            this.gatewayIp = builder.gatewayIp;
            this.networkAddress = builder.networkAddress;
            this.dhcpEnabled = builder.dhcpEnabled;
            this.dnsNameServers = builder.dnsNameServers;
        }
    }

    public static class Builder {
        private String name;
        private String projectId;
        private String networkId;
        private int ipVersion;
        private String gatewayIp;
        private String networkAddress;
        private boolean dhcpEnabled;
        private List<String> dnsNameServers;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder ipVersion(int ipVersion) {
            this.ipVersion = ipVersion;
            return this;
        }

        public Builder gatewayIp(String gatewayIp) {
            this.gatewayIp = gatewayIp;
            return this;
        }

        public Builder networkAddress(String networkAddress) {
            this.networkAddress = networkAddress;
            return this;
        }

        public Builder dhcpEnabled(boolean dhcpEnabled) {
            this.dhcpEnabled = dhcpEnabled;
            return this;
        }

        public Builder dnsNameServers(List<String> dnsNameServers) {
            this.dnsNameServers = dnsNameServers;
            return this;
        }

        public CreateSubnetRequest build() {
            Subnet subnet = new Subnet(this);
            return new CreateSubnetRequest(subnet);
        }
    }
}
