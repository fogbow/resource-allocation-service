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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;

@XmlRootElement(name = RULE)
public class Rule {

	private static final int PROTOCOL_INDEX = 0;
	private static final int IP_INDEX = 1;
	private static final int SIZE_INDEX = 2;
	private static final int RANGE_INDEX = 3;
	private static final int TYPE_INDEX = 4;
	private static final int NETWORK_ID_INDEX = 5;
	private static final int SECURITY_GROUP_INDEX = 6;

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
	protected static final int IPV4_AMOUNT_BITS = 32;

	private static final String INSTANCE_ID_SEPARATOR = "@@";
	
	protected static int IPV6_AMOUNT_BITS = 128;


	private String protocol;
	private String ip;
	private int size;
	private String range;
	private String type;
	private int networkId;
	private String securityGroupId;

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
	
	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(String securityGroupId) {
		this.securityGroupId = securityGroupId;
	}
	
	/*
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

	// TODO the size must be a BigInteger because the ipv6 amount
	protected String calculateSubnetMask() {
		try {
			if (CidrUtils.isIpv4(this.ip)) {
				return getSubnetIPV4(this.size);
			} else if (CidrUtils.isIpv6(this.ip)) {
				return getSubnetIPV6(this.size);
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

	protected static String getSubnetIPV6(int size) {
		return String.valueOf(IPV6_AMOUNT_BITS - (int) (Math.log(size) / Math.log(LOG_BASE_2)));
	}
	
	public String serialize() {
		String[] attributes = new String[7];
		attributes[PROTOCOL_INDEX] = this.protocol;
		attributes[IP_INDEX] = this.ip;
		attributes[SIZE_INDEX] = String.valueOf(this.size);
		attributes[RANGE_INDEX] = this.range;
		attributes[TYPE_INDEX] = this.type;
		attributes[NETWORK_ID_INDEX] = String.valueOf(this.networkId);
		attributes[SECURITY_GROUP_INDEX] = this.securityGroupId;
		
		String instanceId = StringUtils.join(attributes, INSTANCE_ID_SEPARATOR);
		return instanceId;
	}
	
	public static Rule deserialize(String instanceId) {
		Rule rule = new Rule();
		String[] instanceIdSplit = instanceId.split(INSTANCE_ID_SEPARATOR);
		rule.setProtocol(instanceIdSplit[PROTOCOL_INDEX]);
		rule.setIp(instanceIdSplit[IP_INDEX]);
		rule.setSize(Integer.parseInt(instanceIdSplit[SIZE_INDEX]));
		rule.setRange(instanceIdSplit[RANGE_INDEX]);
		rule.setType(instanceIdSplit[TYPE_INDEX]);
		rule.setNetworkId(Integer.parseInt(instanceIdSplit[NETWORK_ID_INDEX]));
		rule.setSecurityGroupId(instanceIdSplit[SECURITY_GROUP_INDEX]);
		return rule;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rule other = (Rule) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (networkId != other.networkId)
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (size != other.size)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
