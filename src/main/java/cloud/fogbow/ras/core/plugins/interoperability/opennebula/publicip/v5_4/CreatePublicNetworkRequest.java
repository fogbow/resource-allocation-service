package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.List;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4.PublicNetworkTemplate.LeaseIp;

public class CreatePublicNetworkRequest {

	private PublicNetworkTemplate publicNetwork;

	public PublicNetworkTemplate getPublicNetwork() {
		return publicNetwork;
	}

	public CreatePublicNetworkRequest(Builder builder) {
		String name = builder.name;
		String type = builder.type;
		String bridge = builder.bridge;
		String bridgedDrive = builder.bridgedDrive;
		List<LeaseIp> leases = builder.leases;
		String securityGroups = builder.securityGroups;
		
		this.publicNetwork = new PublicNetworkTemplate();
		this.publicNetwork.setName(name);
		this.publicNetwork.setType(type);
		this.publicNetwork.setBridge(bridge);
		this.publicNetwork.setBridgedDrive(bridgedDrive);
		this.publicNetwork.setLeases(leases);
		this.publicNetwork.setSecurityGroups(securityGroups);
	}
	
	public static class Builder {
		
		private String name;
		private String type;
		private String bridge;
		private String bridgedDrive;
		private List<LeaseIp> leases;
		private String securityGroups;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder type(String type) {
			this.type = type;
			return this;
		}
		
		public Builder bridge(String bridge) {
			this.bridge = bridge;
			return this;
		}
		
		public Builder bridgedDrive(String bridgedDrive) {
			this.bridgedDrive = bridgedDrive;
			return this;
		}
		
		public Builder leases(List<LeaseIp> leases) {
			this.leases = leases;
			return this;
		}
		
		public Builder securityGroups(String securityGroups) {
			this.securityGroups = securityGroups;
			return this;
		}
		
		public CreatePublicNetworkRequest build() {
			return new CreatePublicNetworkRequest(this);
		}
	}
}
