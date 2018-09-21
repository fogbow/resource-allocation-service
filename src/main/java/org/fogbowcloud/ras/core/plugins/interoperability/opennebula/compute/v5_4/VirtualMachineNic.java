package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

// NIC: Network Interface Connected
@XmlRootElement(name = "NIC")
public class VirtualMachineNic {

	private String networkId;

	@XmlElement(name = "NETWORK_ID")
	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
	
}
