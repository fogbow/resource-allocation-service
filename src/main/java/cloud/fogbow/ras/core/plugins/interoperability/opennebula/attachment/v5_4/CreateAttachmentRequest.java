package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

public class CreateAttachmentRequest {

	private AttachmentDisk attachDisk;

	public CreateAttachmentRequest(Builder builder) {
		String imageId = builder.imageId;
		String target = builder.target;

		this.attachDisk = new AttachmentDisk(imageId, target);
	}

	public AttachmentDisk getAttachDisk() {
		return attachDisk;
	}

	public static class Builder {
		
		private String imageId;
		private String target;

		public Builder imageId(String imageId) {
			this.imageId = imageId;
			return this;
		}

		public Builder target(String target) {
			this.target = target;
			return this;
		}

		public CreateAttachmentRequest build(){
            return new CreateAttachmentRequest(this);
        }
	}
}
