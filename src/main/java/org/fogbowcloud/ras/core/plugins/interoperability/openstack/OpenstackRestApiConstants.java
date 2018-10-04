package org.fogbowcloud.ras.core.plugins.interoperability.openstack;

public class OpenstackRestApiConstants {

    public static class Volume {
        public static final String VOLUME_KEY_JSON = "volume";
        public static final String STATUS_KEY_JSON = "status";
        public static final String SIZE_KEY_JSON = "size";
        public static final String NAME_KEY_JSON = "name";
        public static final String ID_KEY_JSON = "id";
    }

    public static class Attachment {
        public static final String VOLUME_ATTACHMENT_KEY_JSON = "volumeAttachment";
        public static final String VOLUME_ID_KEY_JSON = "volumeId";
        public static final String SERVER_ID_KEY_JSON = "serverId";
        public static final String ID_KEY_JSON = "id";
        public static final String DEVICE_KEY_JSON = "device";
    }

    public static class Compute {
        public static final String SERVER_KEY_JSON = "server";
        public static final String ID_KEY_JSON = "id";
        public static final String NAME_KEY_JSON = "name";
        public static final String DISK_KEY_JSON = "disk";
        public static final String MEMORY_KEY_JSON = "ram";
        public static final String VCPUS_KEY_JSON = "vcpus";
        public static final String ADDRESSES_KEY_JSON = "addresses";
        public static final String FLAVOR_KEY_JSON = "flavor";
        public static final String STATUS_KEY_JSON = "status";
        public static final String PROVIDER_KEY_JSON = "provider";
        public static final String ADDRESS_KEY_JSON = "addr";
        public static final String IMAGE_REFERENCE_KEY_JSON = "imageRef";
        public static final String FLAVOR_REFERENCE_KEY_JSON = "flavorRef";
        public static final String USER_DATA_KEY_JSON = "user_data";
        public static final String KEY_NAME_KEY_JSON = "key_name";
        public static final String NETWORKS_KEY_JSON = "networks";
        public static final String SECURITY_GROUPS_KEY_JSON = "security_groups";
        public static final String UUID_KEY_JSON = "uuid";
        public static final String KEY_PAIR_KEY_JSON = "keypair";
        public static final String PUBLIC_KEY_KEY_JSON = "public_key";
    }

    public static class Network {
        public static final String NETWORK_KEY_JSON = "network";
        public static final String NAME_KEY_JSON = "name";
        public static final String PROJECT_ID_KEY_JSON = "project_id";
        public static final String ID_KEY_JSON = "id";
        public static final String NETWORK_ID_KEY_JSON = "network_id";
        public static final String IP_VERSION_KEY_JSON = "ip_version";
        public static final String GATEWAY_IP_KEY_JSON = "gateway_ip";
        public static final String CIDR_KEY_JSON = "cidr";
        public static final String ENABLE_DHCP_KEY_JSON = "enable_dhcp";
        public static final String DNS_NAMESERVERS_KEY_JSON = "dns_nameservers";
        public static final String SECURITY_GROUP_KEY_JSON = "security_group";
        public static final String SECURITY_GROUP_RULE_KEY_JSON = "security_group_rule";
        public static final String DIRECTION_KEY_JSON = "direction";
        public static final String SECURITY_GROUP_ID_KEY_JSON = "security_group_id";
        public static final String REMOTE_IP_PREFIX_KEY_JSON = "remote_ip_prefix";
        public static final String PROTOCOL_KEY_JSON = "protocol";
        public static final String MIN_PORT_KEY_JSON = "port_range_min";
        public static final String MAX_PORT_KEY_JSON = "port_range_max";
        public static final String PROVIDER_SEGMENTATION_ID_KEY_JSON = "provider:segmentation_id";
        public static final String SUBNET_KEY_JSON = "subnet";
        public static final String SUBNETS_KEY_JSON = "subnets";
        public static final String STATUS_KEY_JSON = "status";
    }

    public static class Image {
        public static final String ID_KEY_JSON = "id";
        public static final String NAME_KEY_JSON = "name";
        public static final String SIZE_KEY_JSON = "size";
        public static final String STATUS_KEY_JSON = "status";
        public static final String MIN_RAM_KEY_JSON = "min_ram";
        public static final String MIN_DISK_KEY_JSON = "min_disk";
        public static final String VISIBILITY_KEY_JSON = "visibility";
        public static final String OWNER_KEY_JSON = "owner";
        public static final String NEXT_KEY_JSON = "next";
        public static final String IMAGES_KEY_JSON = "images";
    }

    public static class Identity {
        public static final String ID_KEY_JSON = "id";
        public static final String AUTH_KEY_JSON = "auth";
        public static final String USER_KEY_JSON = "user";
        public static final String NAME_KEY_JSON = "name";
        public static final String SCOPE_KEY_JSON = "scope";
        public static final String DOMAIN_KEY_JSON = "domain";
        public static final String TOKEN_KEY_JSON = "token";
        public static final String METHODS_KEY_JSON = "methods";
        public static final String METHODS_PASSWORD_VALUE_JSON = "password";
        public static final String PROJECT_KEY_JSON = "project";
        public static final String PASSWORD_KEY_JSON = "password";
        public static final String IDENTITY_KEY_JSON = "identity";
    }

    public static class Quota {
        public static final String LIMITS_KEY_JSON = "limits";
        public static final String ABSOLUTE_KEY_JSON = "absolute";
        public static final String MAX_TOTAL_CORES_KEY_JSON = "maxTotalCores";
        public static final String MAX_TOTAL_RAM_SIZE_KEY_JSON = "maxTotalRAMSize";
        public static final String MAX_TOTAL_INSTANCES_KEY_JSON = "maxTotalInstances";
        public static final String TOTAL_CORES_USED_KEY_JSON = "totalCoresUsed";
        public static final String TOTAL_RAM_USED_KEY_JSON = "totalRAMUsed";
        public static final String TOTAL_INSTANCES_USED_KEY_JSON = "totalInstancesUsed";
    }

    public static class PublicIp {
        public static final String ID_KEY_JSON = "id";
        public static final String STATUS_KEY_JSON = "status";
        public static final String FLOATING_IP_KEY_JSON = "floatingip";
        public static final String FLOATING_IP_ADDRESS_KEY_JSON = "floating_ip_address";
        public static final String FLOATING_NETWORK_ID_KEY_JSON = "floating_network_id";
        public static final String PORT_ID_KEY_JSON = "port_id";
        public static final String PROJECT_ID_KEY_JSON = "project_id";
        public static final String PORTS_KEY_JSON = "ports";
    }
}
