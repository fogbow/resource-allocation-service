package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import java.util.List;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

public class CreateSubnetRequest {

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

        @SerializedName(TENANT_ID_KEY_JSON)
        private final String tenantId;

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
            this.tenantId = builder.tenantId;
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
        private String tenantId;
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

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
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
