package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SIZE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = TEMPLATE)
public class VirtualNetworkReserveTemplate extends OpenNebulaMarshaller {

	private String name;
	private String ip;
	private String addressRangeId;
	private int size;

	//TODO(pauloewerton): move to common
	private static final String IP = "IP";
	private static final String AR_ID = "AR_ID";

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

	@XmlElement(name = IP)
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@XmlElement(name = AR_ID)
	public String getAddressRangeId() {
		return addressRangeId;
	}

	public void setAddressRangeId(String addressRangeId) {
		this.addressRangeId = addressRangeId;
	}
}
