package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.BRIDGE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.IP;
import static cloud.fogbow.common.constants.OpenNebulaConstants.LEASES;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SECURITY_GROUPS;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TYPE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.VIRTUAL_NETWORK_BRIDGED_DRIVE;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = TEMPLATE)
public class PublicNetworkTemplate extends OpenNebulaMarshallerTemplate{

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
