package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.DISK;
import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.IMAGE_ID;
import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = TEMPLATE)
public class AttachmentDisk extends OpenNebulaMarshallerTemplate {

	private Disk disk;

	public AttachmentDisk() {}

	public AttachmentDisk(String imageId) {
		this.disk = new Disk();
		this.disk.imageId = imageId;
	}

	@XmlElement(name = DISK)
	public Disk getDisk() {
		return disk;
	}
	
	@XmlRootElement(name = DISK)
	public static class Disk {
		
		private String imageId;

		@XmlElement(name = IMAGE_ID)
		public String getImageId() {
			return imageId;
		}
	}
}
