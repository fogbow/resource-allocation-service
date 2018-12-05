package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.IP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RANGE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE_TYPE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SIZE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = RULE)
public class Rule {

	private String protocol;
	private String ip;
	private int size;
	private String range;
	private String type;
	private int networkId;

	@XmlElement(name = PROTOCOL)
	public String getProtocol() {
		return protocol;
	}

	@XmlElement(name = PROTOCOL)
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@XmlElement(name = IP)
	public String getIp() {
		return ip;
	}

	@XmlElement(name = IP)
	public void setIp(String ip) {
		this.ip = ip;
	}

	@XmlElement(name = SIZE)
	public int getSize() {
		return size;
	}

	@XmlElement(name = SIZE)
	public void setSize(int size) {
		this.size = size;
	}

	@XmlElement(name = RULE_TYPE)
	public String getType() {
		return type;
	}

	@XmlElement(name = RULE_TYPE)
	public void setType(String type) {
		this.type = type;
	}

	@XmlElement(name = RANGE)
	public String getRange() {
		return range;
	}

	@XmlElement(name = RANGE)
	public void setRange(String range) {
		this.range = range;
	}

	@XmlElement(name = NETWORK_ID)
	public int getNetworkId() {
		return networkId;
	}

	@XmlElement(name = NETWORK_ID)
	public void setNetworkId(int networkId) {
		this.networkId = networkId;
	}
}
