package org.fogbowcloud.ras.core.models.instances;

public class AttachmentInstance extends Instance {
    private String serverId;
    private String volumeId;
    private String device;
    private String computeName;
    private String volumeName;

    public AttachmentInstance(String id, InstanceState state, String serverId, String volumeId, String device) {
        super(id, state);
        this.serverId = serverId;
        this.volumeId = volumeId;
        this.device = device;
    }

    public AttachmentInstance(String id) {
        super(id);
    }

    public String getDevice() {
        return this.device;
    }

    public String getServerId() {
        return this.serverId;
    }

    public String getVolumeId() {
        return this.volumeId;
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
