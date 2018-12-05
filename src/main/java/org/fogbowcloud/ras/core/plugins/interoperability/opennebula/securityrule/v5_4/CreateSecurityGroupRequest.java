package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import java.util.List;

public class CreateSecurityGroupRequest {

	private SecurityGroupTemplate securityGroup;
	
	public SecurityGroupTemplate getSecurityGroup() {
		return securityGroup;
	}
	
	public CreateSecurityGroupRequest(Builder builder) {
		String name = builder.name;
		List<Rule> rules = builder.rules;
		this.securityGroup = new SecurityGroupTemplate(name, rules);
	}
	
	public static class Builder {
		
		private String name;
		private List<Rule> rules;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder rules(List<Rule> rules) {
			this.rules = rules;
			return this;
		}
		
		public CreateSecurityGroupRequest build() {
			return new CreateSecurityGroupRequest(this);
		}
	}
}
