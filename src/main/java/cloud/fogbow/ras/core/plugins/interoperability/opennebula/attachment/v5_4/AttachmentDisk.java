package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

@XmlRootElement(name = TEMPLATE)
public class AttachmentDisk extends OpenNebulaMarshaller {

	private Disk disk;

	public AttachmentDisk() {}

	public AttachmentDisk(String imageId, String target) {
		this.disk = new Disk();
		this.disk.imageId = imageId;
		this.disk.target = target;
	}

	@XmlElement(name = DISK)
	public Disk getDisk() {
		return disk;
	}
	
	@XmlRootElement(name = DISK)
	public static class Disk {
		// TODO(pauloewerton): move to common
		private static final String TARGET = "TARGET";
		
		private String imageId;
		private String target;

		@XmlElement(name = IMAGE_ID)
		public String getImageId() {
			return imageId;
		}

		@XmlElement(name = TARGET)
		public String getTarget() {
			return target;
		}
	}
}
