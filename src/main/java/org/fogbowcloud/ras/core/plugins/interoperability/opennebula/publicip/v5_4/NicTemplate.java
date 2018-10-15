package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.NETWORK_INTERFACE_CONNECTED;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;

@XmlRootElement(name = NETWORK_INTERFACE_CONNECTED)
public class NicTemplate extends OpenNebulaRequestTemplate {

	private String networkId;

	public NicTemplate(String networkId) {
		this.networkId = networkId;
	}

	@XmlElement(name = NETWORK_ID)
	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
}
