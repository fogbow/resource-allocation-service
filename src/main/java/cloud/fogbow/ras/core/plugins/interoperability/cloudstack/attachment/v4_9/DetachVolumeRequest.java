package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

public class DetachVolumeRequest extends CloudStackRequest {

    protected static final String DETACH_VOLUME_COMMAND = "detachVolume";
    protected static final String ATTACH_VOLUME_ID = "id";
    
    protected DetachVolumeRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
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
        private String cloudStackUrl;
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public DetachVolumeRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new DetachVolumeRequest(this);
        }
        
    }
    
}
