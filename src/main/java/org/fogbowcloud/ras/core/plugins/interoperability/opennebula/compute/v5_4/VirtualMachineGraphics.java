package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.GRAPHICS;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.LISTEN;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = GRAPHICS)
public class VirtualMachineGraphics {

	private String listen;
	private String type;
	
	@XmlElement(name = LISTEN)
	public String getListen() {
		return listen;
	}
	
	public void setListen(String listen) {
		this.listen = listen;
	}
	
	@XmlElement(name = TYPE)
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
}
