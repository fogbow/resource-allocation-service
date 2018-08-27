package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class GetVolumeRequest extends CloudStackRequest {
    protected static final String LIST_VOLUMES_COMMAND = "listVolumes";
    private static final String VOLUME_ID_KEY = "id";
    private static final String VIRTUAL_MACHINE_ID_KEY = "virtualmachineid";

    protected GetVolumeRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(VOLUME_ID_KEY, builder.id);
        addParameter(VIRTUAL_MACHINE_ID_KEY, builder.virtualMachineId);
    }

    @Override
    public String getCommand() {
        return LIST_VOLUMES_COMMAND;
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

        public GetVolumeRequest build() throws InvalidParameterException {
            return new GetVolumeRequest(this);
        }
    }
}
