package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

public class CreateVolumeRequest {

    private VolumeImage volumeImage;

    public CreateVolumeRequest(Builder builder) {
    	this.volumeImage = new VolumeImage();
    	this.volumeImage.setName(builder.name);
    	this.volumeImage.setPersistent(builder.persistent);
    	this.volumeImage.setType(builder.type);
    	this.volumeImage.setFileSystemType(builder.fileSystemType);
    	this.volumeImage.setDiskType(builder.diskType);
    	this.volumeImage.setDevicePrefix(builder.devicePrefix);
    	this.volumeImage.setSize(builder.size);
    }

    public VolumeImage getVolumeImage() {
        return volumeImage;
    }

    public static class Builder {
        
    	private String name;
        private String persistent;
        private String type;
        private String fileSystemType;
        private String diskType;
        private String devicePrefix;
        private int size;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder persistent(String persistent) {
            this.persistent = persistent;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder fileSystemType(String fileSystemType) {
            this.fileSystemType = fileSystemType;
            return this;
        }

        public Builder diskType(String diskType) {
            this.diskType = diskType;
            return this;
        }

        public Builder devicePrefix(String devicePrefix) {
            this.devicePrefix = devicePrefix;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public CreateVolumeRequest build(){
            return new CreateVolumeRequest(this);
        }
    }
}
