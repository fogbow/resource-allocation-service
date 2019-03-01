package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import cloud.fogbow.common.util.cloud.opennebula.OpenNebulaMarshallerTemplate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

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
