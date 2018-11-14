package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.CONTEXT;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.USERDATA;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.USERDATA_ENCODING;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = CONTEXT)
public class VirtualMachineContext {

	private String encoding;
	private String userdata;
	private String network;
	
	@XmlElement(name = USERDATA_ENCODING)
	public String getEncoding() {
		return encoding;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	@XmlElement(name = USERDATA)
	public String getUserdata() {
		return userdata;
	}
	
	public void setUserdata(String userdata) {
		this.userdata = userdata;
	}
	
	@XmlElement(name = NETWORK)
	public String getNetwork() {
		return network;
	}
	
	public void setNetwork(String network) {
		this.network = network;
	}
	
}
