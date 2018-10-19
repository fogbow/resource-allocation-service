package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.NETWORK_INTERFACE_CONNECTED;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.NIC_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualNetwork.SECURITY_GROUPS;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;

@XmlRootElement(name = NETWORK_INTERFACE_CONNECTED)
public class NetworkInterfaceConnected extends OpenNebulaRequestTemplate {

	private String nicId;
	private String networkId;
	private String securityGroups;

	public NetworkInterfaceConnected(String nicId, String networkId, String securityGroups) {
		this.nicId = nicId;
		this.networkId = networkId;
		this.securityGroups = securityGroups;
	}

	@XmlElement(name = NIC_ID)
	public String getNicId() {
		return nicId;
	}

	public void setNicId(String nicId) {
		this.nicId = nicId;
	}
	
	@XmlElement(name = NETWORK_ID)
	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}

	@XmlElement(name = SECURITY_GROUPS)
	public String getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(String securityGroups) {
		this.securityGroups = securityGroups;
	}

}
