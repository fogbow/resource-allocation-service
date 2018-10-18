package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TEMPLATE")
public class SecurityGroups extends OpenNebulaRequestTemplate {

	private String name;
	private List<Rule> rules;

	public SecurityGroups(String name, List<Rule> rules) {
		this.name = name;
		this.rules = rules;
	}

	@XmlElement(name = "NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElementWrapper
	@XmlElement(name = "RULE")
	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}
	
	public static abstract class Rule {
		
		protected String protocol;
		protected String type;
		
		@XmlElement(name = "PROTOCOL")
		public String getProtocol() {
			return protocol;
		}
		
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}
		
		@XmlElement(name = "RULE_TYPE")
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
	}
	
	@XmlRootElement(name = "RULE")
	public static class SafetyRule {
		
		private String range;
		 
 		@XmlElement(name = "RANGE")
		public String getRange() {
			return range;
		}
		
		public void setRange(String range) {
			this.range = range;
		}
	}
	
	@XmlRootElement(name = "RULE")
	public static class DefaultRule extends Rule {

		private int networkId;

		public DefaultRule(String protocol, String type, int networkId) {
			this.protocol = protocol;
			this.type = type;
			this.networkId = networkId;
		}

		@XmlElement(name = "NETWORK_ID")
		public int getNetworkId() {
			return networkId;
		}

		public void setNetworkId(int networkId) {
			this.networkId = networkId;
		}
	}
}
