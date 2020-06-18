package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

public class GetAllDiskOfferingsRequest extends CloudStackRequest {
    public static final String LIST_DISK_OFFERINGS_COMMAND = "listDiskOfferings";

    protected GetAllDiskOfferingsRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
    }

    @Override
    public String getCommand() {
        return LIST_DISK_OFFERINGS_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static class Builder {
        private String cloudStackUrl;

        public GetAllDiskOfferingsRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new GetAllDiskOfferingsRequest(this);
        }
    }
}
