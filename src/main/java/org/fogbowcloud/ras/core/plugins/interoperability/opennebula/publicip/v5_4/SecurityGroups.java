package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TEMPLATE")
public class SgTemplate {

	private String name;
	private List<Rule> rules;

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
	
	@XmlRootElement(name = "RULE")
	public class Rule {
		
		private String protocol;
		private String ruleType;
		private String range;
		
		@XmlElement(name = "PROTOCOL")
		public String getProtocol() {
			return protocol;
		}
		
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}
		
		@XmlElement(name = "RULE_TYPE")
		public String getRuleType() {
			return ruleType;
		}
		
		public void setRuleType(String ruleType) {
			this.ruleType = ruleType;
		}
		
		@XmlElement(name = "RANGE")
		public String getRange() {
			return range;
		}
		
		public void setRange(String range) {
			this.range = range;
		}
		
	}
}
