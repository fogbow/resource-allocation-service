package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

public class CreateNetworkUpdateRequest {

	private VirtualNetworkUpdateTemplate virtualNetworkUpdate;
	
	public VirtualNetworkUpdateTemplate getVirtualNetworkUpdate() {
		return virtualNetworkUpdate;
	}
	
	public CreateNetworkUpdateRequest(Builder builder) {
		String securityGroups = builder.securityGroups;
		
		this.virtualNetworkUpdate = new VirtualNetworkUpdateTemplate();
		this.virtualNetworkUpdate.setSecurityGroups(securityGroups);
	}

	public static class Builder {

		private String securityGroups;
		
		public Builder securityGroups(String securityGroups) {
			this.securityGroups = securityGroups;
			return this;
		}
		
		public CreateNetworkUpdateRequest build() {
			return new CreateNetworkUpdateRequest(this);
		}
		
	}

}
