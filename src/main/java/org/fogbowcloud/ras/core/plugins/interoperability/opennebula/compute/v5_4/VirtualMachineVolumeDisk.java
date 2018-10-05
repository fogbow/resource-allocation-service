package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = DISK)
public class VirtualMachineVolumeDisk {

	private String size;
	private String type;
	
	@XmlElement(name = SIZE)
	public String getSize() {
		return size;
	}
	
	public void setSize(String size) {
		this.size = size;
	}
	
	@XmlElement(name = TYPE)
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
}
