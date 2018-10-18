package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

public class CreateNicRequest {

	private NetworkInterfaceConnected nic;
	private SecurityGroups securityGroups;

	public NetworkInterfaceConnected getNic() {
		return this.nic;
	}

	public SecurityGroups getSecurityGroups() {
		return securityGroups;
	}
	
	public CreateNicRequest(Builder builder) {
		String nicId = builder.nicId;
		String securityGroups = builder.securityGroups;
		this.nic = new NetworkInterfaceConnected(nicId, securityGroups);
	}

	public static class Builder {

		private String nicId;
		private String securityGroups;

		public Builder nicId(String nicId) {
			this.nicId = nicId;
			return this;
		}
		
		public Builder securityGroups(String securityGroups) {
			this.securityGroups = securityGroups;
			return this;
		}

		public CreateNicRequest build() {
			return new CreateNicRequest(this);
		}
	}
}
