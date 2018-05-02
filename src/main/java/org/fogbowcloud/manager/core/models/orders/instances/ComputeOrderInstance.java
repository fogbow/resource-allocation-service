package org.fogbowcloud.manager.core.models.orders.instances;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.utils.SshCommonUserUtil;
import org.json.JSONObject;

public class ComputeOrderInstance extends OrderInstance {

    private static final String SSH_SERVICE_NAME = null;
	private static final String SSH_PUBLIC_ADDRESS_ATT = null;
	private static final String SSH_USERNAME_ATT = null;
	private static final String EXTRA_PORTS_ATT = null;
	
	private String hostName;
    private int vCPU;
    /**
     *  Memory attribute, must be set in MB.
     */
    private int memory;    
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

	public void addTunnelingPorts(String key, String value) {
		if (this.tunnelingPorts == null) {
			this.tunnelingPorts = new HashMap<String, String>();
		}
		this.tunnelingPorts.put(key, value);
	}
	
	public void setExternalServiceAddresses(Map<String, String> serviceAddresses) {
		if (serviceAddresses != null) {
			this.addTunnelingPorts(SSH_PUBLIC_ADDRESS_ATT, serviceAddresses.get(SSH_SERVICE_NAME));
			this.addTunnelingPorts(SSH_USERNAME_ATT, SshCommonUserUtil.getSshCommonUser());
			serviceAddresses.remove(SSH_SERVICE_NAME);
			this.addTunnelingPorts(EXTRA_PORTS_ATT, new JSONObject(serviceAddresses).toString());
		}
	}

}
