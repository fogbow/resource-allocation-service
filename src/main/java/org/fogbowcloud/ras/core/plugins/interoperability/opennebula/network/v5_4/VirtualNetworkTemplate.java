package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.AR;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.BRIDGE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.DESCRIPTION;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NAME;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NETWORK_ADDRESS;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.NETWORK_GATEWAY;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.TEMPLATE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;




@XmlRootElement(name = TEMPLATE)
public class VirtualNetworkTemplate extends OpenNebulaRequestTemplate {

	private String name;
	private String description;
	private String type;
	private String bridge;
	private String networkAddress;
	private String networkGateway;
	private VirtualNetworkAddressRange addressRange;
	
	public VirtualNetworkTemplate() {}

	public VirtualNetworkTemplate(String name, String description, String type, String bridge,
			String networkAddress, String networkGateway, VirtualNetworkAddressRange addressRange) {
		
		super();
		this.name = name;
		this.description = description;
		this.type = type;
		this.bridge = bridge;
		this.networkAddress = networkAddress;
		this.networkGateway = networkGateway;
		this.addressRange = addressRange;
	}

	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlElement(name = DESCRIPTION)
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	@XmlElement(name = TYPE)
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement(name = BRIDGE)
	public String getBridge() {
		return bridge;
	}
	
	public void setBridge(String bridge) {
		this.bridge = bridge;
	}
	
	@XmlElement(name = NETWORK_ADDRESS)
	public String getNetworkAddress() {
		return networkAddress;
	}
	
	public void setNetworkAddress(String networkAddress) {
		this.networkAddress = networkAddress;
	}
	
	@XmlElement(name = NETWORK_GATEWAY)
	public String getNetworkGateway() {
		return networkGateway;
	}
	
	public void setNetworkGateway(String networkGateway) {
		this.networkGateway = networkGateway;
	}
	
	@XmlElement(name = AR)
	public VirtualNetworkAddressRange getAddressRange() {
		return addressRange;
	}
	
	public void setAddressRange(VirtualNetworkAddressRange addressRange) {
		this.addressRange = addressRange;
	}
	
}
