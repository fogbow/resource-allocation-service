package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

public class EmulatedCloudConstants {

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
    }
}

