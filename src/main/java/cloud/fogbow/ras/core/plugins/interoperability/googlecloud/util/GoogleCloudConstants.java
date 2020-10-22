package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;

public class GoogleCloudConstants {
    public static final String PATH_PROJECT = "/projects";
    public static final String LINE_SEPARATOR = "/";
    public static final String GLOBAL_IP_ENDPOINT = "/global/addresses";

    public static class Network{
        public static final String NETWORK_KEY_JSON = "network";
        public static final String NAME_KEY_JSON = "name";
        public static final String ID_KEY_JSON = "id";
        public static final String AUTO_CREATE_SUBNETS_KEY_JSON = "autoCreateSubnetworks";
        public static final String CIDR_KEY_JSON = "cidr";
        public static final String ROUTING_MODE_KEY_JSON = "routingMode";
        public static final String ROUTING_CONFIG_KEY_JSON = "routingConfig";
    }

    public static final class SecurityRule {
        public static final EtherType ETHER_TYPE = EtherType.IPv4;
    }
}
