package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.Volume.*;

public class CreateVolumeRequest extends CloudStackRequest {

    protected CreateVolumeRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
        addParameter(ZONE_ID_KEY_JSON, builder.zoneId);
        addParameter(NAME_KEY_JSON, builder.name);
        addParameter(DISK_OFFERING_ID_KEY_JSON, builder.diskOfferingId);
        addParameter(SIZE_KEY_JSON, builder.size);
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

        public CreateVolumeRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new CreateVolumeRequest(this);
        }
    }
}
