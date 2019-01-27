package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class AttachVolumeRequest extends CloudStackRequest {

    protected static final String ATTACH_VOLUME_COMMAND = "attachVolume";
    protected static final String ATTACH_VOLUME_ID = "id";
    protected static final String VIRTUAL_MACHINE_ID = "virtualmachineid";
    
    protected AttachVolumeRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(ATTACH_VOLUME_ID, builder.id);
        addParameter(VIRTUAL_MACHINE_ID, builder.virtualMachineId);
    }
    
    @Override
    public String getCommand() {
        return ATTACH_VOLUME_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }
    
    public static class Builder {
        private String cloudStackUrl;
        private String id;
        private String virtualMachineId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder virtualMachineId(String virtualMachineId) {
            this.virtualMachineId = virtualMachineId;
            return this;
        }

        public AttachVolumeRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new AttachVolumeRequest(this);
        }
        
    }
    
}