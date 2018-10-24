package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DeleteVolumeRequest extends CloudStackRequest {
    protected static final String DELETE_VOLUME_COMMAND = "deleteVolume";
    private static final String VOLUME_ID_KEY = "id";

    protected DeleteVolumeRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(VOLUME_ID_KEY, builder.id);
    }

    @Override
    public String getCommand() {
        return DELETE_VOLUME_COMMAND;
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

        public DeleteVolumeRequest build() throws InvalidParameterException {
            return new DeleteVolumeRequest(this);
        }
    }
}
