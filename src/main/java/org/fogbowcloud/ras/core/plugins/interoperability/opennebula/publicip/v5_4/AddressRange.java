package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.AR;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.IP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SIZE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = TEMPLATE)
public class AddressRange extends OpenNebulaMarshallerTemplate {

	private Range range;
	
	public AddressRange() {}

	public AddressRange(String type, String ip, String size) {
		this.range = new Range();
		this.range.type = type;
		this.range.ip = ip;
		this.range.size = size;
	}

	@XmlElement(name = AR)
	public Range getRange() {
		return range;
	}

	@XmlRootElement(name = AR)
	public static class Range {
		
		private String type;
		private String ip;
		private String size;
		
		@XmlElement(name = TYPE)
		public String getType() {
			return type;
		}
		
		@XmlElement(name = IP)
		public String getIp() {
			return ip;
		}
		
		@XmlElement(name = SIZE)
		public String getSize() {
			return size;
		}
	}
}
