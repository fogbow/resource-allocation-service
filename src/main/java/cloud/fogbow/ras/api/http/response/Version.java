package cloud.fogbow.ras.api.http.response;

public class Version {
    private String version;

    public Version() {}

    public Version(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
