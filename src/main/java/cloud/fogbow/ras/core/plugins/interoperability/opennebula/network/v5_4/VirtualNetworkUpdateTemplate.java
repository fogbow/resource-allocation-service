package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.SECURITY_GROUPS;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = TEMPLATE)
public class VirtualNetworkUpdateTemplate extends OpenNebulaMarshaller {
	
	private String securityGroups;

	@XmlElement(name = SECURITY_GROUPS)
	public String getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(String securityGroups) {
		this.securityGroups = securityGroups;
	}
	
}
