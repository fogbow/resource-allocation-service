package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class GetAllServiceOfferingsRequest extends CloudStackRequest {

    protected static final String LIST_SERVICE_OFFERINGS_COMMAND = "listServiceOfferings";
    
    protected GetAllServiceOfferingsRequest(Builder builder) throws InvalidParameterException {
        super();
    }

    @Override
    public String getCommand() {
        return LIST_SERVICE_OFFERINGS_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }
    
    public static class Builder {

        public GetAllServiceOfferingsRequest build() throws InvalidParameterException {
            return new GetAllServiceOfferingsRequest(this);
        }
        
    }
    
}
