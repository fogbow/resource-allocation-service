package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import java.util.List;

public class CreateSecurityGroupRequest {

	private SecurityGroupTemplate securityGroup;
	
	public SecurityGroupTemplate getSecurityGroup() {
		return securityGroup;
	}
	
	public CreateSecurityGroupRequest(Builder builder) {
		String id = builder.id;
		String name = builder.name;
		List<Rule> rules = builder.rules;
		this.securityGroup = new SecurityGroupTemplate(id, name, rules);
	}
	
	public static class Builder {
		
		private String id;
		private String name;
		private List<Rule> rules;
		
		public Builder id(String id) {
			this.id = id;
			return this;
		}
		
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
