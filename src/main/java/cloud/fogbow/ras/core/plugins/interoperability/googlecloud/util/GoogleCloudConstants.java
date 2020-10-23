package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;

public class GoogleCloudConstants {
  
    public static final String AUTHORIZATION_KEY = "Authorization";
    public static final String BEARER_S = "Bearer %s";
    public static final String COMPUTE_V1_API_ENDPOINT = "/v1";
    public static final String ENDPOINT_SEPARATOR = "/";
    public static final String VOLUME_ENDPOINT = "/volume";
    public static final String COMPUTE_ENDPOINT = "/compute";
    public static final String ZONES_KEY_ENDPOINT = "/zones";
    public static final String INSTANCES_KEY_ENDPOINT = "/instances";
    public static final String ATTACH_DISK_KEY_ENDPOINT = "/attachDisk";
    public static final String DETACH_DISK_KEY_ENDPOINT = "/DetachDisk";
    public static final String PREDEFINED_ZONE = "";
    public static final String DEVICE_NAME_QUERY_PARAM = "?deviceName=";
    public static final String DEFAULT_ZONE = "southamerica-east1-b";
    public static final String DISKS_KEY_ENDPOINT = "/disks";
    public static final String PROJECTS_KEY_ENDPOINT = "/projects";
    public static final String PATH_PROJECT = "/projects";
    public static final String PATH_ZONE = "/zones";
    public static final String LINE_SEPARATOR = "/";
    public static final String V1_API_ENDPOINT = "/v1";
    public static final String GLOBAL_IP_ENDPOINT = "/global/addresses";
    public static final String GLOBAL_IMAGES_ENDPOINT = "/global/images";
    public static final String GLOBAL_NETWORKS_ENDPOINT = "/global/networks";
    public static final String COMPUTE_ENGINE_ENDPOINT = "/compute";
    public static final String COMPUTE_ENGINE_V1_ENDPOINT = "/v1";
    public static final String FLAVOR_ENDPOINT = "/machineTypes";
    public static final String COMPUTE_ENDPOINT = "/instances";
    public static final String ZONE_KEY_CONFIG = "zone";
    public static final String STATUS_KEY_JSON = "status";
    public static final String GLOBAL_IP_NETWORK = "/global/networks";
    public static final String REGION_ENDPOINT = "/regions";
    public static final String SUBNET_ENDPOINT = "/subnetworks";

    public static class Volume {
        public static final String VOLUME_KEY_JSON = "volume";
        public static final String STATUS_KEY_JSON = "status";
        public static final String SIZE_KEY_JSON = "sizeGb";
        public static final String NAME_KEY_JSON = "name";
        public static final String ID_KEY_JSON = "id";
        public static final String TYPE_KEY_JSON = "type";
        public static final String VALID_DISK_SIZE_KEY_JSON = "valid_disk_size";
        public static final String DESCRIPTION_KEY_JSON = "description";
    }

    public static class Attachment {
        public static final String INSTANCE_NAME_KEY_JSON = "serverId";
        public static final String ATTACH_DISK_KEY_JSON = "attachDisk";
        public static final String DEVICE_NAME_KEY_JSON = "deviceName";
        public static final String VOLUME_SOURCE_KEY_JSON = "source";
        public static final String ATTACH_NAME_KEY_JSON = "name";
        public static final String ATTACH_ID_KEY_JSON = "id";

    }
}

public class GoogleCloudConstants {
    public static final String PATH_PROJECT = "/projects";
    public static final String PATH_ZONE = "/zones";
    public static final String LINE_SEPARATOR = "/";
    public static final String V1_API_ENDPOINT = "/v1";
    public static final String GLOBAL_IP_ENDPOINT = "/global/addresses";
    public static final String GLOBAL_IMAGES_ENDPOINT = "/global/images";
    public static final String GLOBAL_NETWORKS_ENDPOINT = "/global/networks";
    public static final String COMPUTE_ENGINE_ENDPOINT = "/compute";
    public static final String COMPUTE_ENGINE_V1_ENDPOINT = "/v1";
    public static final String FLAVOR_ENDPOINT = "/machineTypes";
    public static final String COMPUTE_ENDPOINT = "/instances";
    public static final String ZONE_KEY_CONFIG = "zone";
    public static final String STATUS_KEY_JSON = "status";
    public static final String GLOBAL_IP_NETWORK = "/global/networks";
    public static final String REGION_ENDPOINT = "/regions";
    public static final String SUBNET_ENDPOINT = "/subnetworks";

    public static class Network{
        public static final String NETWORK_KEY_JSON = "network";
        public static final String NAME_KEY_JSON = "name";
        public static final String ID_KEY_JSON = "id";
        public static final String AUTO_CREATE_SUBNETS_KEY_JSON = "autoCreateSubnetworks";
        public static final String CIDR_KEY_JSON = "ipCidrRange";
        public static final String ROUTING_MODE_KEY_JSON = "routingMode";
        public static final String ROUTING_CONFIG_KEY_JSON = "routingConfig";
        public static final String DEFAULT_NETWORK_KEY = "default";
        public static final String TARGET_LINK_KEY_JSON = "targetLink";
    }

    public static final class SecurityRule {
        public static final EtherType ETHER_TYPE = EtherType.IPv4;
    }

    public static final class Compute {
        public static final String TARGET_ID_KEY_JSON = "targetId";
        public static final String ID_KEY_JSON = "id";
        public static final String NAME_KEY_JSON = "name";
        public static final String FLAVOR_KEY_JSON = "machineType";
        public static final String DISKS_KEY_JSON = "disks";
        public static final String NETWORKS_KEY_JSON = "networkInterfaces";
        public static final String METADATA_KEY_JSON = "metadata";
        public static final String PUBLIC_SSH_KEY_JSON = "ssh-keys";
        public static final String USER_DATA_KEY_JSON = "startup-script";
        public static final String CUSTOM_FLAVOR_KEY = "custom";
        public static final String FAULT_MSG_KEY_JSON = "error.message";
        public static final String ADDRESS_KEY_JSON = "networkIP";

        public static final class Disk {
            public static final String INITIAL_PARAMS_KEY_JSON = "initializeParams";
            public static final String BOOT_KEY_JSON = "boot";
            public static final boolean BOOT_DEFAULT_VALUE = true;

            public static final class InitializeParams {
                public static final String IMAGE_KEY_JSON = "sourceImage";
                public static final String DISK_SIZE_KEY_JSON = "diskSizeGb";
            }
        }

        public static final class Metadata {
            public static final String ITEMS_KEY_JSON = "items";
            public static final String KEY_ITEM_KEY_JSON = "key";
            public static final String VALUE_ITEM_KEY_JSON = "value";
        }
    }
}
