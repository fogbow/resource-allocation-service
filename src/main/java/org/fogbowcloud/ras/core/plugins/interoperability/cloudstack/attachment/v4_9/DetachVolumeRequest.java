package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DetachVolumeRequest extends CloudStackRequest {

    protected static final String DETACH_VOLUME_COMMAND = "detachVolume";
    protected static final String ATTACH_VOLUME_ID = "id";
    
    protected DetachVolumeRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(ATTACH_VOLUME_ID, builder.id);
    }

    @Override
    public String getCommand() {
        return DETACH_VOLUME_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }
    
    public static class Builder {
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public DetachVolumeRequest build() throws InvalidParameterException {
            return new DetachVolumeRequest(this);
        }
        
    }
    
}
