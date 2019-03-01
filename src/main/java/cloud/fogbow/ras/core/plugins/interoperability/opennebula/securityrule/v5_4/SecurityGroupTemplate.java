package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

@XmlRootElement(name = TEMPLATE)
public class SecurityGroupTemplate extends OpenNebulaMarshaller {

	public static final String RANGE_FORMAT = "%s:s%";
	
	private String id;
	private String name;
	private List<Rule> rules;

	public SecurityGroupTemplate() {}

	public SecurityGroupTemplate(String id, String name, List<Rule> rules) {
		this.id = id;
		this.name = name;
		this.rules = rules;
	}
	
	@XmlElement(name = ID)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = RULE)
	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	public static Rule allocateSafetyRule(String protocol, String type, String ip,
			String size, int portFrom, int portTo, String networkId) {

		String range = String.format(RANGE_FORMAT, String.valueOf(portFrom), String.valueOf(portTo));

		Rule rule = new Rule();
		rule.setProtocol(protocol);
		rule.setIp(ip);
		rule.setSize(size);
		rule.setRange(range);
		rule.setType(type);
		rule.setNetworkId(networkId);

		return rule;
	}
}
