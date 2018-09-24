package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

public class VolumeRequest {
    public VolumeRequest(Builder builder) {

    }

    public static class Builder(){
        private String volumeName;
        private String persistent;
        private String type;
        private String fstype;
        private String diskType;
        private String devPrefix;
        private int size;

        public Builder name(String volumeName){
            this.volumeName = volumeName;
            return this;
        }

        public VolumeRequest build(){

            return new VolumeRequest(this);
        }
    }
}
