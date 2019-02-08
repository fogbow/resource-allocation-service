package cloud.fogbow.ras.core.models;

public enum Operation {
    CREATE("create"),
    GET_ALL("getAll"),
    GET("get"),
    DELETE("delete"),
    GET_USER_QUOTA("getUserQuota"),
    GET_USER_ALLOCATION("getUserAllocation"),
    GET_ALL_IMAGES("getAllImages"),
    GET_IMAGE("getImage"),
    GET_CLOUD_NAMES("getCloudNames"),
    GENERIC_REQUEST("genericRequest");

    private String value;

    private Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
