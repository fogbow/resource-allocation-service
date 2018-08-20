package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class GetAllDiskOfferingsRequest extends CloudStackRequest {

    private static final String DISK_OFFERINGS_COMMAND = "listDiskOfferings";
    
    protected GetAllDiskOfferingsRequest(Builder builder) throws InvalidParameterException {
        super();
    }

    @Override
    public String getCommand() {
        return DISK_OFFERINGS_COMMAND;
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
