package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class VolumeInstance extends OrderInstance {
    @ApiModelProperty(position = 7, example = ApiDocumentation.Model.VOLUME_NAME)
    private String name;
    @ApiModelProperty(position = 8, required = true, example = "1", notes = ApiDocumentation.Model.VOLUME_SIZE_NOTE)
    private int size;

    public VolumeInstance(String id, String cloudState, String name, int size) {
        super(id, cloudState);
        this.name = name;
        this.size = size;
    }

    public VolumeInstance(String id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
