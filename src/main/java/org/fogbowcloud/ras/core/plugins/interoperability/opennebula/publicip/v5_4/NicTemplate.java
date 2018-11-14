package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_INTERFACE_CONNECTED;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SECURITY_GROUPS;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = TEMPLATE)
public class NicTemplate extends OpenNebulaMarshallerTemplate {

	private NetworkInterfaceConnected nic;
	
	public NicTemplate() {}
	
	public NicTemplate(String networkId, String securityGroups) {
		this.nic = new NetworkInterfaceConnected();
		this.nic.networkId = networkId;
		this.nic.securityGroups = securityGroups;
	}

	@XmlElement(name = NETWORK_INTERFACE_CONNECTED)
	public NetworkInterfaceConnected getNic() {
		return nic;
	}

	public static class NetworkInterfaceConnected {
		
		private String networkId;
		private String securityGroups;
		
		@XmlElement(name = NETWORK_ID)
		public String getNetworkId() {
			return networkId;
		}
		
		@XmlElement(name = SECURITY_GROUPS)
		public String getSecurityGroups() {
			return securityGroups;
		}
	}
}
