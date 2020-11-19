package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class Snapshot {
    @ApiModelProperty(position = 1, example = ApiDocumentation.Model.SNAPSHOT_NAME, notes = ApiDocumentation.Model.SNAPSHOT_NAME_NOTE)
    private String name;

    public String getName() {
        return name;
    }
}
