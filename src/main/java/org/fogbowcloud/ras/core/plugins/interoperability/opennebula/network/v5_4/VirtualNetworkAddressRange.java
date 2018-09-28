package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualNetwork.*;

@XmlRootElement(name = AR)
public class VirtualNetworkAddressRange {

	private String type;
	private String ipAddress;
	private String rangeSize;
	
	@XmlElement(name = TYPE)
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement(name = IP)
	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	@XmlElement(name = SIZE)
	public String getRangeSize() {
		return rangeSize;
	}
	
	public void setRangeSize(String rangeSize) {
		this.rangeSize = rangeSize;
	}
	
}
