package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import cloud.fogbow.common.util.cloud.opennebula.OpenNebulaMarshallerTemplate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

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
