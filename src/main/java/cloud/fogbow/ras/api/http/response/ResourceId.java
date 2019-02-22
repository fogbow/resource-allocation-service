package cloud.fogbow.ras.api.http.response;

public class ResourceId {
    private String id;

    public ResourceId() {}

    public ResourceId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
