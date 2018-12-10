package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.*;

@XmlRootElement(name = RULE)
public class Rule {

	public static final Logger LOGGER = Logger.getLogger(Rule.class);

	protected static final String IPSEC_XML_TEMPLATE_VALUE = "IPSEC";
	protected static final String ALL_XML_TEMPLATE_VALUE = "ALL";
	protected static final String ICMPV6_XML_TEMPLATE_VALUE = "ICMPV6";
	protected static final String ICMP_XML_TEMPLATE_VALUE = "ICMP";
	protected static final String UDP_XML_TEMPLATE_VALUE = "UDP";
	protected static final String TCP_XML_TEMPLATE_VALUE = "TCP";
	protected static final String INBOUND_XML_TEMPLATE_VALUE = "inbound";
	protected static final String OUTBOUND_XML_TEMPLATE_VALUE = "outbound";

	protected static final String CIRD_SEPARATOR = "/";
	protected static final String OPENNEBULA_RANGE_SEPARATOR = ":";
	protected static final int POSITION_PORT_FROM_IN_RANGE = 0;
	protected static final int POSITION_PORT_TO_IN_RANGE = 1;
	protected static final int INT_ERROR_CODE = -1;
	private static final int LOG_BASE_2 = 2;
	public static final int IPV4_AMOUNT_BITS = 32;


	private String protocol;
	private String ip;
	private int size;
	private String range;
	private String type;
	private int networkId;

	public Rule() {};

	public Rule(String protocol, String ip, int size, String range, String type, int networkId) {
		this.protocol = protocol;
		this.ip = ip;
		this.size = size;
		this.range = range;
		this.type = type;
		this.networkId = networkId;
	}

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
			if (this.range == null || this.range.isEmpty()) {
				throw new Exception("The range is null");
			}
			String[] rangeSplited = this.range.split(OPENNEBULA_RANGE_SEPARATOR);
			if (rangeSplited.length != 2) {
				throw new Exception("The range is with wrong format");
			}
			return Integer.parseInt(rangeSplited[portType]);
		} catch (Exception e) {
			LOGGER.warn("There is a problem when it is trying to get port.", e);
			return INT_ERROR_CODE;
		}
	}

	public Direction getDirection() {
		if (this.type == null || this.type.isEmpty()) {
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
	
	public Protocol getSRProtocol() {
		if (this.protocol == null || this.protocol.isEmpty()) {
			LOGGER.warn("The protocol is null");
		}
		switch (this.protocol) {
		case TCP_XML_TEMPLATE_VALUE:
			return Protocol.TCP;
		case UDP_XML_TEMPLATE_VALUE:
			return Protocol.UDP;
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
		return this.ip + CIRD_SEPARATOR + calculateSubnetMask();
	}

	// TODO implement for ipv6
	protected String calculateSubnetMask() {
		try {
			if (CidrUtils.isIpv4(this.ip)) {
				return getSubnetIPV4(this.size);
			} else if (CidrUtils.isIpv6(this.ip)) {
				// TODO implement
				// return String.valueOf(32 - (int) (Math.log(this.size) / Math.log(LOG_BASE_2)));
				throw new Exception("Not implemented");
			} else {
				LOGGER.warn(String.format("The IP is inconsistent"));
				return null;
			}
		} catch (Exception e) {
			LOGGER.warn("There is a problem when it is trying to calculate the subnet mask", e);
			return String.valueOf(INT_ERROR_CODE);
		}
	}

	protected static String getSubnetIPV4(int size) {
		return String.valueOf(IPV4_AMOUNT_BITS - (int) (Math.log(size) / Math.log(LOG_BASE_2)));
	}
}
