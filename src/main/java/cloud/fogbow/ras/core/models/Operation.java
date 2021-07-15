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
    RESUME("resume"), 
    PAUSE_ALL("pauseAll"),
    RESUME_ALL("resumeAll"),
    STOP("stop");

    private String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
    
    public static Operation fromString(String value) {
        for (Operation operationValue : values()) {
            if (operationValue.getValue().equals(value)) { 
                return operationValue;
            }
        }
        throw new IllegalArgumentException();
    }
}
