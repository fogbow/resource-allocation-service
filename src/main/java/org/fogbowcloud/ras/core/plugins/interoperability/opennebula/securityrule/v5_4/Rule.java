package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.IP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RANGE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE_TYPE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SIZE;

import java.io.File;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;

@XmlRootElement(name = RULE)
public class Rule {
	

	public static final Logger LOGGER = Logger.getLogger(Rule.class);

	private static final String IPSEC_XML_TEMPLATE_VALUE = "IPSEC";
	private static final String ALL_XML_TEMPLATE_VALUE = "ALL";
	private static final String ICMPV6_XML_TEMPLATE_VALUE = "ICMPV6";
	private static final String ICMP_XML_TEMPLATE_VALUE = "ICMP";
	private static final String UDP_XML_TEMPLATE_VALUE = "UDP";
	private static final String TCP_XML_TEMPLATE_VALUE = "TCP";
	private static final String INBOUND_XML_TEMPLATE_VALUE = "inbound";
	private static final String OUTBOUND_XML_TEMPLATE_VALUE = "outbound";
	
	private static final String OPENNEBULA_RANGE_SEPARETOR = ":";
	private static final int POSITION_PORT_FROM_IN_RANGE = 0;
	private static final int POSITION_PORT_TO_IN_RANGE = 1;
	private static final int INT_ERROR_CODE = -1;
	private static final int LOG_BASE = 2;
	
	private String protocol;
	private String ip;
	private int size;
	private String range;
	private String type;
	private int networkId;

	public String getProtocol() {
		return protocol;
	}

	@XmlElement(name = PROTOCOL)
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getIp() {
		return ip;
	}

	@XmlElement(name = IP)
	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getSize() {
		return size;
	}

	@XmlElement(name = SIZE)
	public void setSize(int size) {
		this.size = size;
	}

	public String getType() {
		return type;
	}

	@XmlElement(name = RULE_TYPE)
	public void setType(String type) {
		this.type = type;
	}

	public String getRange() {
		return range;
	}

	@XmlElement(name = RANGE)
	public void setRange(String range) {
		this.range = range;
	}

	public int getNetworkId() {
		return networkId;
	}

	@XmlElement(name = NETWORK_ID)
	public void setNetworkId(int networkId) {
		this.networkId = networkId;
	}
	
	/*
	* TODO refactor this methods above
	* TODO implement tests 	
	* TODO check if is necessary put this messages in another class	
	*/
	
	public int getPortFrom() {
		return getPortInRange(POSITION_PORT_FROM_IN_RANGE);
	}

	public int getPortTo() {
		return getPortInRange(POSITION_PORT_TO_IN_RANGE);
	}	
	
	protected int getPortInRange(int portType) {
		try {
			if (this.range == null || this.range.isBlank()) {
				throw new Exception("The range is null");
			}
			String[] rangeSplited = this.range.split(OPENNEBULA_RANGE_SEPARETOR);
			if (rangeSplited.length != 2) {
				throw new Exception("The range is with wrong format");
			}
			return Integer.parseInt(rangeSplited[portType]);
		} catch (Exception e) {
			LOGGER.warn("There is a problem when it is trying to get port.", e);
			return INT_ERROR_CODE;
		}
	}

	public Direction getDirection() throws FogbowRasException {
		if (this.type == null || this.type.isBlank()) {
			LOGGER.warn("The type is null");
			return null;
		}
		switch (this.type) {
		case INBOUND_XML_TEMPLATE_VALUE:
			return Direction.IN;
		case OUTBOUND_XML_TEMPLATE_VALUE:
			return Direction.OUT;
		default:
			LOGGER.warn(String.format("The type(%s) is inconsistent", this.type));
			return null;
		}
	}
	
	public Protocol getSRProtocol() throws FogbowRasException {
		if (this.protocol == null || this.protocol.isBlank()) {
			LOGGER.warn("The protocol is null");
		}
		switch (this.protocol) {
		case TCP_XML_TEMPLATE_VALUE:
			return Protocol.TCP;
		case UDP_XML_TEMPLATE_VALUE:
			return Protocol.TCP;
		case ICMP_XML_TEMPLATE_VALUE:
		case ICMPV6_XML_TEMPLATE_VALUE:
			return Protocol.ICMP;
		case ALL_XML_TEMPLATE_VALUE:
		case IPSEC_XML_TEMPLATE_VALUE: // TODO thing more about this ipsec value.
			return Protocol.ANY;
		default:
			LOGGER.warn(String.format("The protocol(%s) is inconsistent", this.protocol));
			return null;
		}		
	}
	
	public EtherType getEtherType() {
		if (this.ip == null) {
			LOGGER.warn("The etherType is null");
			return null;
		}
		if (CidrUtils.isIpv4(this.ip)) {
			return EtherType.IPv4;
		} else if (CidrUtils.isIpv6(this.ip)) {
			return EtherType.IPv6;
		} else {
			LOGGER.warn(String.format("The etherType is inconsistent"));
			return null;
		}		
	}
	
	public String getCIDR() {
		if (this.ip == null) {
			return ALL_XML_TEMPLATE_VALUE;
		}
		return this.ip + File.separator + calculateSubnetMask();
	}

	protected String calculateSubnetMask() {
		try {
			return String.valueOf(32 - (int) (Math.log(this.size) / Math.log(LOG_BASE)));			
		} catch (Exception e) {
			LOGGER.warn("There is a problem when it is trying to calculate the subnet mask", e);
			return String.valueOf(INT_ERROR_CODE);			
		}
	}
}
