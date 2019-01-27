package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

public class CreateNicRequest {

	private NicTemplate nic;

	public NicTemplate getNic() {
		return this.nic;
	}

	public CreateNicRequest(Builder builder) {
		String networkId = builder.networkId;
		String securityGroups = builder.securityGroups;
		this.nic = new NicTemplate(networkId, securityGroups);
	}

	public static class Builder {

		private String networkId;
		private String securityGroups;

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
