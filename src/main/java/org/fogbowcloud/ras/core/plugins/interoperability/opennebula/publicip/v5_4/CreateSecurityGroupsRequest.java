package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.List;

public class CreateSecurityGroupsRequest {

	private SecurityGroups securityGroups;

	public SecurityGroups getSecurityGroups() {
		return this.securityGroups;
	}

	public CreateSecurityGroupsRequest(Builder builder) {
		String name = builder.name;
		List<SecurityGroups.Rule> rules = builder.rules;
		this.securityGroups = new SecurityGroups(name, rules);
	}
	
	public static class Builder {
		
		private String name;
		private List<SecurityGroups.Rule> rules;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder rules(List<SecurityGroups.Rule> rules) {
			this.rules = rules;
			return this;
		}
		
		public CreateSecurityGroupsRequest build() {
			return new CreateSecurityGroupsRequest(this);
		}
	}
}
