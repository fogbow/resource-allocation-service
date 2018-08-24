package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class GetAllDiskOfferingsRequest extends CloudStackRequest {

    protected static final String LIST_DISK_OFFERINGS_COMMAND = "listDiskOfferings";
    
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
