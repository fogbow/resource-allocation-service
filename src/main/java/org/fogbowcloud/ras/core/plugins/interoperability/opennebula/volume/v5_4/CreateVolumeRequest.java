package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

public class CreateVolumeRequest {

    private VolumeImageRequestTemplate volumeImageRequestTemplate;

    public CreateVolumeRequest(Builder builder) {
        String name = builder.name;
        String persistent = builder.persistent;
        String type = builder.type;
        String fstype = builder.fstype;
        String diskType = builder.diskType;
        String devPrefix = builder.devPrefix;
        int size = builder.size;
        this.volumeImageRequestTemplate = new VolumeImageRequestTemplate(
            name, persistent, type, fstype, diskType, devPrefix, size);
    }

    public VolumeImageRequestTemplate getVolumeImageRequestTemplate() {
        return volumeImageRequestTemplate;
    }

    public static class Builder {
        private String name;
        private String persistent;
        private String type;
        private String fstype;
        private String diskType;
        private String devPrefix;
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

        public Builder fstype(String fstype) {
            this.fstype = fstype;
            return this;
        }

        public Builder diskType(String diskType) {
            this.diskType = diskType;
            return this;
        }

        public Builder devPrefix(String devPrefix) {
            this.devPrefix = devPrefix;
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
