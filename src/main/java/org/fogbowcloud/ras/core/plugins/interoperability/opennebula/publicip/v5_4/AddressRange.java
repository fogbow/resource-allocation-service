package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.AR;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.AR_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.IP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.SIZE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;

@XmlRootElement(name = AR)
public class AddressRange extends OpenNebulaRequestTemplate {

	private String arId;
	private String type;
	private String ip;
	private String size;
	
	public AddressRange(String arId, String type, String ip, String size) {
		this.arId = arId;
		this.type = type;
		this.ip = ip;
		this.size = size;
	}

	@XmlElement(name = AR_ID)
	public String getArId() {
		return arId;
	}

	public void setArId(String arId) {
		this.arId = arId;
	}
	
	@XmlElement(name = TYPE)
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement(name = IP)
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	@XmlElement(name = SIZE)
	public String getSize() {
		return size;
	}
	
	public void setRangeSize(String size) {
		this.size = size;
	}

}
