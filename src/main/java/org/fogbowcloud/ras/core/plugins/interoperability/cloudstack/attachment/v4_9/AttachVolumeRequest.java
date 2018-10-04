package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class AttachVolumeRequest extends CloudStackRequest {

    protected static final String ATTACH_VOLUME_COMMAND = "attachVolume";
    protected static final String ATTACH_VOLUME_ID = "id";
    protected static final String VIRTUAL_MACHINE_ID = "virtualmachineid";
    
    protected AttachVolumeRequest(Builder builder) throws InvalidParameterException {
        super();
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

        public AttachVolumeRequest build() throws InvalidParameterException {
            return new AttachVolumeRequest(this);
        }
        
    }
    
}