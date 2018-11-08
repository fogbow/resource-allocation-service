package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NAME;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.RULE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.TEMPLATE;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;

@XmlRootElement(name = TEMPLATE)
public class SecurityGroups extends OpenNebulaRequestTemplate {

	private String name;
	private List<SafetyRule> rules;

	public SecurityGroups() {}

	public SecurityGroups(String name, List<SafetyRule> rules) {
		this.name = name;
		this.rules = rules;
	}

	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElementWrapper
	@XmlElement(name = RULE)
	public List<SafetyRule> getRules() {
		return rules;
	}

	public void setRules(List<SafetyRule> rules) {
		this.rules = rules;
	}
}
