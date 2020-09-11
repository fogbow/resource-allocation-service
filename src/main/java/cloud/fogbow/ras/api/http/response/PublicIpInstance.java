package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class PublicIpInstance extends OrderInstance {
    @ApiModelProperty(position = 8, example = "10.10.0.2")
    private String ip;
    @ApiModelProperty(position = 9, example = ApiDocumentation.Model.COMPUTE_ID)
    private String computeId;
    @ApiModelProperty(position = 10, example = ApiDocumentation.Model.COMPUTE_NAME)
    private String computeName;

    public PublicIpInstance(String id) {
        super(id);
    }

    public PublicIpInstance(String id, String cloudState, String ip) {
        super(id, cloudState);
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public String getComputeName() {
        return computeName;
    }

    public void setComputeName(String computeName) {
        this.computeName = computeName;
    }

    public String getComputeId() {
        return computeId;
    }

    public void setComputeId(String computeId) {
        this.computeId = computeId;
    }
}
