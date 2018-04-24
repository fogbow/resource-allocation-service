package org.fogbowcloud.manager.core.models.orders.instances;

import java.util.Map;

public class ComputeOrderInstance extends OrderInstance {

    private String hostName;
    private int vCPU;
    /**
     *  Memory attribute, must be set in MB.
     */
    private int memory;
    private InstanceState state;
    private String localIpAddress;
    private String sshPublicAddress;
    private String sshUserName;
    private String sshExtraPorts;
    private Map<String, String> tunnelingPorts;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getvCPU() {
        return vCPU;
    }

    public void setvCPU(int vCPU) {
        this.vCPU = vCPU;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public void setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
    }

    public String getSshPublicAddress() {
        return sshPublicAddress;
    }

    public void setSshPublicAddress(String sshPublicAddress) {
        this.sshPublicAddress = sshPublicAddress;
    }

    public String getSshUserName() {
        return sshUserName;
    }

    public void setSshUserName(String sshUserName) {
        this.sshUserName = sshUserName;
    }

    public String getSshExtraPorts() {
        return sshExtraPorts;
    }

    public void setSshExtraPorts(String sshExtraPorts) {
        this.sshExtraPorts = sshExtraPorts;
    }

	public Map<String, String> getTunnelingPorts() {
		return tunnelingPorts;
	}

	public void setTunnelingPorts(Map<String, String> tunnelingPorts) {
		this.tunnelingPorts = tunnelingPorts;
	}
	
	public InstanceState getState() {
		return state;
	}

	public void setState(InstanceState state) {
		this.state = state;
	}
	
}
