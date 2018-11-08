package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.RANGE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.RULE_TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RULES")
public class SafetyRule {

	private String protocol;
	private String type;
	private String range;
	private int networkId;
	
	public SafetyRule() {}

	public SafetyRule(String protocol, String type, String range, int networkId) {
		this.protocol = protocol;
		this.type = type;
		this.range = range;
		this.networkId = networkId;
	}

	@XmlElement(name = PROTOCOL)
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	@XmlElement(name = RULE_TYPE)
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
		@XmlElement(name = RANGE)
	public String getRange() {
		return range;
	}
	
	public void setRange(String range) {
		this.range = range;
	}
	
	@XmlElement(name = NETWORK_ID)
	public int getNetworkId() {
		return networkId;
	}

	public void setNetworkId(int networkId) {
		this.networkId = networkId;
	}
}
