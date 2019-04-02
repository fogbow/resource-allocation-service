package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

public class CreateSecurityGroupsRequest {

	private SecurityGroupsTemplate securityGroups;
	
	public SecurityGroupsTemplate getSecurityGroups() {
		return securityGroups;
	}
	
	public CreateSecurityGroupsRequest(Builder builder) {
		String securityGroups = builder.securityGroups;
		
		this.securityGroups = new SecurityGroupsTemplate();
		this.securityGroups.setSecurityGroups(securityGroups);
	}

	public static class Builder {

		private String securityGroups;
		
		public Builder securityGroups(String securityGroups) {
			this.securityGroups = securityGroups;
			return this;
		}
		
		public CreateSecurityGroupsRequest build() {
			return new CreateSecurityGroupsRequest(this);
		}
		
	}

}
