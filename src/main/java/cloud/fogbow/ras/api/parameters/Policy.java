package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class Policy {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.POLICY, notes = ApiDocumentation.Model.POLICY_NOTE)
    private String policy;
    
    public String getPolicy() {
        return policy;
    }
}
