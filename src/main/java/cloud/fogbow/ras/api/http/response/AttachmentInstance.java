package cloud.fogbow.ras.api.http.response;

public class AttachmentInstance extends Instance {
    private String computeId;
    private String volumeId;
    private String device;
    private String computeName;
    private String volumeName;

    public AttachmentInstance(String id, InstanceState state, String computeId, String volumeId, String device) {
        super(id, state);
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
