package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.enums;

public enum RoutingMode {
    GLOBAL("GLOBAL"),
    REGIONAL("REGIONAL");

    private String description;

    private RoutingMode(String description){
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }
    public static RoutingMode toEnum(String description) {

        if (description == null) {
            return null;
        }

        for (RoutingMode x : RoutingMode.values()) {
            if (description.equals(x.getDescription())) {
                return x;
            }
        }
        //TODO: Throw some exception if string is invalid
        //throw new IllegalArgumentException("Invalid Description: " + description);
        return null;
    }
}
