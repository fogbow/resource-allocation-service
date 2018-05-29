package org.fogbowcloud.manager.core.models.orders.instances;

public class VolumeAttachment extends Instance{

	private String serverId;
    private String volumeId;
    private String device;
	
    public VolumeAttachment(String id, InstanceState state, String serverId, String volumeId, String device) {
		super(id, state);
		this.serverId = serverId;
		this.volumeId = volumeId;
		this.device = device;
	}

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getVolumeId() {
        return this.volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getDevice() {
        return this.device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
    
}
