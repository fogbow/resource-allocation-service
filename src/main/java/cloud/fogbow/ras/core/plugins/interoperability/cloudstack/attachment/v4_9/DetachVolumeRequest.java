package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.Attachment.DETACH_VOLUME_COMMAND;
import static cloud.fogbow.common.constants.OpenStackConstants.Attachment.ID_KEY_JSON;

public class DetachVolumeRequest extends CloudStackRequest {

    protected DetachVolumeRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
        addParameter(ID_KEY_JSON, builder.id);
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

        public DetachVolumeRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new DetachVolumeRequest(this);
        }
        
    }
    
}
