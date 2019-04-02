package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SIZE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = TEMPLATE)
public class VirtualNetworkReservedTemplate extends OpenNebulaMarshaller {

	private String name;
	private int size;
	
	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = SIZE)
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
