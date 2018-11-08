package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NETWORK_INTERFACE_CONNECTED;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = NETWORK_INTERFACE_CONNECTED)
public class VirtualMachineNic {

	private String networkId;

	@XmlElement(name = NETWORK_ID)
	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
	
}
