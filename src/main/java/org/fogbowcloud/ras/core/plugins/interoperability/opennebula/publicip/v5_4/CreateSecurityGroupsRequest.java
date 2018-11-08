package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.List;

public class CreateSecurityGroupsRequest {

	private SecurityGroups securityGroups;

	public SecurityGroups getSecurityGroups() {
		return this.securityGroups;
	}

	public CreateSecurityGroupsRequest(Builder builder) {
		String name = builder.name;
		List<SafetyRule> rules = builder.rules;
		this.securityGroups = new SecurityGroups();
		this.securityGroups.setName(name);
		this.securityGroups.setRules(rules);
	}
	
	public static class Builder {
		
		private String name;
		private List<SafetyRule> rules;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder rules(List<SafetyRule> rules) {
			this.rules = rules;
			return this;
		}
		
		public CreateSecurityGroupsRequest build() {
			return new CreateSecurityGroupsRequest(this);
		}
	}
}
