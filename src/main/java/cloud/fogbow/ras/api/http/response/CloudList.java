package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class CloudList {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.CLOUD_LIST)
    private List<String> clouds;

    private CloudList() {}

    public CloudList(List<String> clouds) {
        this.clouds = clouds;
    }

    public List<String> getClouds() {
        return clouds;
    }

    public void setClouds(List<String> clouds) {
        this.clouds = clouds;
    }
}
