package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

public class CreateAttachmentRequest {

	private AttachmentDisk attachDisk;

	public CreateAttachmentRequest(Builder builder) {
		String imageId = builder.imageId;
		this.attachDisk = new AttachmentDisk(imageId);
	}

	public AttachmentDisk getAttachDisk() {
		return attachDisk;
	}

	public static class Builder {
		
		private String imageId;
		
		public Builder imageId(String imageId) {
			this.imageId = imageId;
			return this;
		}
		
		public CreateAttachmentRequest build(){
            return new CreateAttachmentRequest(this);
        }
	}
}
