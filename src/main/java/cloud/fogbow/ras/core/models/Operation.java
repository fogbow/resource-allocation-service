package cloud.fogbow.ras.core.models;

public enum Operation {
    CREATE("create"),
    GET_ALL("getAll"),
    GET("get"),
    DELETE("delete"),
    GET_USER_ALLOCATION("getUserAllocation"),
    TAKE_SNAPSHOT("takeSnapshot"),
    RELOAD("reload"),
    PAUSE("pause"),
    HIBERNATE("hibernate"),
    RESUME("resume");


    private String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
