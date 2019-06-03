package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class ResourceId {
    @ApiModelProperty(example = ApiDocumentation.Model.INSTANCE_ID)
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
