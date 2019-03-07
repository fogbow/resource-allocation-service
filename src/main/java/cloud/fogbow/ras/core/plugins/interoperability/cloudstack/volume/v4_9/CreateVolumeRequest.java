package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

public class CreateVolumeRequest extends CloudStackRequest {
    protected static final String CREATE_VOLUME_COMMAND = "createVolume";
    protected static final String ZONE_ID = "zoneid";
    protected static final String VOLUME_NAME = "name";
    protected static final String DISK_OFFERING_ID = "diskofferingid";
    protected static final String VOLUME_SIZE = "size";

    protected CreateVolumeRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(ZONE_ID, builder.zoneId);
        addParameter(VOLUME_NAME, builder.name);
        addParameter(DISK_OFFERING_ID, builder.diskOfferingId);
        addParameter(VOLUME_SIZE, builder.size);
    }

    @Override
    public String getCommand() {
        return CREATE_VOLUME_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static class Builder {
        private String cloudStackUrl;
        private String zoneId;
        private String name;
        private String diskOfferingId;
        private String size;

        public Builder zoneId(String zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder diskOfferingId(String diskOfferingId) {
            this.diskOfferingId = diskOfferingId;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public CreateVolumeRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new CreateVolumeRequest(this);
        }
    }
}
