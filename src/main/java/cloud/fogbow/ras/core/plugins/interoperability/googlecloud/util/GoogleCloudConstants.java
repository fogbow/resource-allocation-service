package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

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