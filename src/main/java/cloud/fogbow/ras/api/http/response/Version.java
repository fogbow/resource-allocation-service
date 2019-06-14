package cloud.fogbow.ras.api.http.response;

import io.swagger.annotations.ApiModelProperty;

public class Version {
    @ApiModelProperty(example = "v.3.0.0-ras-ec6bf564-as-c803775-common-4e0d74e")
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
