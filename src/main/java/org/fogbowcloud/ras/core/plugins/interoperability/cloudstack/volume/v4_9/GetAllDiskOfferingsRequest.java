package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class GetAllDiskOfferingsRequest extends CloudStackRequest {
    public static final String LIST_DISK_OFFERINGS_COMMAND = "listDiskOfferings";

    protected GetAllDiskOfferingsRequest(Builder builder) throws InvalidParameterException {
        super();
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

        public GetAllDiskOfferingsRequest build() throws InvalidParameterException {
            return new GetAllDiskOfferingsRequest(this);
        }
    }
}
