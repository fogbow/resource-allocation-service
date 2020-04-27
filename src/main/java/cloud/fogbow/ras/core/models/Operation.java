package cloud.fogbow.ras.core.models;

// TODO refactor GET_USER_QUOTA operation, so that this operation returns all quota and not only compute quota
public enum Operation {
    CREATE("create"),
    GET_ALL("getAll"),
    GET("get"),
    DELETE("delete"),
    GET_USER_ALLOCATION("getUserAllocation");

    private String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
