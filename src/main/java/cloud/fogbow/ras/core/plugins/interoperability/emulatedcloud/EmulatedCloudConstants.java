package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

public class EmulatedCloudConstants {

    public static final String NETWORK_ALLOCATION_MODE_DYNAMIC = "dynamic";
    public static final String NO_VALUE_STRING = "";

    public static class Conf {
        public static final String IMAGE_NAMES_KEY = "image_names";

        public static final String QUOTA_INSTANCES_KEY = "quota_instances";
        public static final String QUOTA_RAM_KEY = "quota_ram";
        public static final String QUOTA_VCPU_KEY = "quota_vCPU";

        public static final String QUOTA_VOLUMES_KEY = "quota_volumes";
        public static final String QUOTA_STORAGE_KEY = "quota_storage";

        public static final String QUOTA_NETWORKS_KEY = "quota_networks";
        public static final String QUOTA_PUBLIC_IP_KEY = "quota_public_ips";
    }

    public static class Plugins {
        public static class PublicIp {
            public static final String ID_KEY_JSON = "id";
        }

        public static class SecurityRule {
            public static final String INGRESS_DIRECTION = "ingress";
            public static final String IPV4_ETHER_TYPE = "IPv4";
        }
    }

    public class Exception {
        public static final String UNABLE_TO_CREATE_RESOURCE_INVALID_INSTANCE_ID_S = "Unable to create resource. Invalid instance id %s";
        public static final String INVALID_INSTANCE_ID = "Invalid instance id %s";
        public static final String EMULATED_RESOURCE_UNDEFINED = "Emulated resource is undefined.";
        public static final String RESOURCE_NOT_FOUND = "Resource not found.";
        public static final String UNABLE_TO_ATTACH_COMPUTE_NOT_FOUND = "Unable to attach. Compute not found.";
        public static final String UNABLE_TO_ATTACH_VOLUME_NOT_FOUND = "Unable to attach. Volume not found.";
        public static final String NO_IMAGE_NAMES_SPECIFIED = "No image names specified in the cloud.conf file";
        public static final String THE_REQUIRED_PROPERTY_S_WAS_NOT_SPECIFIED = "The required property %s was not specified.";
        public static final String THE_PROPERTY_S_MUST_BE_AN_INTEGER = "The property %s must be an integer.";
    }
}

