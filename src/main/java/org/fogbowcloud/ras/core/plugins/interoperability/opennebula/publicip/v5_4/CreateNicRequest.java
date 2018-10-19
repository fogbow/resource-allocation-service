package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

public class CreateNicRequest {

	private NetworkInterfaceConnected nic;

	public NetworkInterfaceConnected getNic() {
		return this.nic;
	}

	public CreateNicRequest(Builder builder) {
		String nicId = builder.nicId;
		String networkId = builder.networkId;
		String securityGroups = builder.securityGroups;
		this.nic = new NetworkInterfaceConnected(nicId, networkId, securityGroups);
	}

	public static class Builder {

		private String nicId;
		private String networkId;
		private String securityGroups;

		public Builder nicId(String id) {
			this.nicId = id;
			return this;
		}
		
		public Builder networkId(String networkId) {
			this.networkId = networkId;
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
