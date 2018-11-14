package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NAME;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RANGE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE_TYPE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = TEMPLATE)
public class SecurityGroups extends OpenNebulaMarshallerTemplate {

	private String name;
	private List<Rule> rules;

	public SecurityGroups() {}

	public SecurityGroups(String name, List<Rule> rules) {
		this.name = name;
		this.rules = rules;
	}
	
	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}

	@XmlElement(name = RULE)
	public List<Rule> getRules() {
		return rules;
	}

	public static Rule allocateSafetyRule(String protocol, String type, String range, int networkId) {
		Rule safetyRule = new Rule();
		safetyRule.protocol = protocol;
		safetyRule.type = type;
		safetyRule.range = range;
		safetyRule.networkId = networkId;
		return safetyRule;
	}
	
	@XmlRootElement(name = RULE)
	public static class Rule {
		
		protected String protocol;
		protected String type;
		protected String range;
		protected int networkId;
		
		@XmlElement(name = PROTOCOL)
		public String getProtocol() {
			return protocol;
		}
		
		@XmlElement(name = RULE_TYPE)
		public String getType() {
			return type;
		}
		
		@XmlElement(name = RANGE)
		public String getRange() {
			return range;
		}
		
		@XmlElement(name = NETWORK_ID)
		public int getNetworkId() {
			return networkId;
		}
	}
}
