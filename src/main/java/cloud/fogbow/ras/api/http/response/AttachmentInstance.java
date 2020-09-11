package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class AttachmentInstance extends OrderInstance {
    @ApiModelProperty(position = 8, example = ApiDocumentation.Model.VOLUME_ID)
    private String volumeId;
    @ApiModelProperty(position = 9, example = ApiDocumentation.Model.VOLUME_NAME)
    private String volumeName;
    @ApiModelProperty(position = 10, example = ApiDocumentation.Model.COMPUTE_ID)
    private String computeId;
    @ApiModelProperty(position = 11, example = ApiDocumentation.Model.COMPUTE_NAME)
    private String computeName;
    @ApiModelProperty(position = 12, example = ApiDocumentation.Model.DEVICE, notes = ApiDocumentation.Model.DEVICE_NOTE)
    private String device;

    public AttachmentInstance(String id, String cloudState, String computeId, String volumeId, String device) {
        super(id, cloudState);
        this.computeId = computeId;
        this.volumeId = volumeId;
        this.device = device;
    }

    public AttachmentInstance(String id) {
        super(id);
    }

    public String getDevice() {
        return this.device;
    }

    public String getComputeId() {
        return this.computeId;
    }

    public void setComputeId(String computeId) {
        this.computeId = computeId;
    }

    public String getVolumeId() {
        return this.volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getComputeName() {
        return computeName;
    }

    public void setComputeName(String computeName) {
        this.computeName = computeName;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }
}
