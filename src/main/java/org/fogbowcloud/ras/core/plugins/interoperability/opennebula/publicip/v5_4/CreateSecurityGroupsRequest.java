package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.List;

public class CreateSecurityGroupsRequest {

	private SgTemplate securityGroups;

	public SgTemplate getSecurityGroups() {
		return this.securityGroups;
	}

	public CreateSecurityGroupsRequest(Builder builder) {
		super();
		String name = builder.name;
		List<SgTemplate.Rule> rules = builder.rules;
//		this.securityGroups = new SgTemplate(name, rules);
		// TODO Auto-generated constructor stub
	}
	
	public static class Builder {
		
		private String name;
		private List<SgTemplate.Rule> rules; // FIXME ...
		
		
		
	}
	
}
