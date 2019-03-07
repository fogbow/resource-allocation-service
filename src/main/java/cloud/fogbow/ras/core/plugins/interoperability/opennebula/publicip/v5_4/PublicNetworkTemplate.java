package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;
//import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaMarshallerTemplate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

@XmlRootElement(name = TEMPLATE)
public class PublicNetworkTemplate extends OpenNebulaMarshaller{

	private String name;
	private String type;
	private String bridge;
	private String bridgedDrive;
	private List<LeaseIp> leases;
	private String securityGroups;
	
	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = TYPE)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlElement(name = BRIDGE)
	public String getBridge() {
		return bridge;
	}

	public void setBridge(String bridge) {
		this.bridge = bridge;
	}

	@XmlElement(name = VIRTUAL_NETWORK_BRIDGED_DRIVE)
	public String getBridgedDrive() {
		return bridgedDrive;
	}

	public void setBridgedDrive(String bridgedDrive) {
		this.bridgedDrive = bridgedDrive;
	}

	@XmlElement(name = LEASES)
	public List<LeaseIp> getLeases() {
		return leases;
	}

	public void setLeases(List<LeaseIp> leases) {
		this.leases = leases;
	}
	
	@XmlElement(name = SECURITY_GROUPS)
	public String getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(String securityGroups) {
		this.securityGroups = securityGroups;
	}
	
	public static LeaseIp allocateIpAddress(String ip) {
		LeaseIp leaseIp = new LeaseIp();
		leaseIp.ip = ip;
		return leaseIp;
	}
	
	@XmlRootElement(name = LEASES)
	public static class LeaseIp {
		
		private String ip;

		@XmlElement(name = IP)
		public String getIp() {
			return ip;
		}
	}
}
