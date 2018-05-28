package org.fogbowcloud.manager.core.models.orders.instances;

public class AttachmentInstance extends Instance{

	private String source;
    private String target;
    private String deviceId;
	
    public AttachmentInstance(String id, InstanceState state, String source, String target, String deviceId) {
		super(id, state);
		this.source = source;
		this.target = target;
		this.deviceId = deviceId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
    
}
