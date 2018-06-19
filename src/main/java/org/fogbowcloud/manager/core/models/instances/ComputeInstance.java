package org.fogbowcloud.manager.core.models.instances;

import org.fogbowcloud.manager.core.models.SshTunnelConnectionData;

// TODO: We need to discuss about these attributes
public class ComputeInstance extends Instance {

    private String hostName;
    private int vCPU;
    /** Memory attribute, must be set in MB. */
    private int ram;

    private String localIpAddress;

    private SshTunnelConnectionData sshTunnelConnectionData;

    public ComputeInstance(
            String id,
            String hostName,
            int vCPU,
            int ram,
            InstanceState state,
            String localIpAddress) {
        super(id, state);
        this.hostName = hostName;
        this.vCPU = vCPU;
        this.ram = ram;
        this.localIpAddress = localIpAddress;
    }

    public ComputeInstance(String id) {
        super(id);
    }

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

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }
    
    public String getLocalIpAddress() {
		return localIpAddress;
	}

    public SshTunnelConnectionData getSshTunnelConnectionData() {
        return sshTunnelConnectionData;
    }

    public void setSshTunnelConnectionData(
        SshTunnelConnectionData sshTunnelConnectionData) {
        this.sshTunnelConnectionData = sshTunnelConnectionData;
    }
}
