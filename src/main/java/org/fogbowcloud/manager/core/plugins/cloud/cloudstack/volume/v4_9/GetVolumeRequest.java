package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class GetVolumeRequest extends CloudStackRequest {

    private static final String VOLUMES_COMMAND = "listVolumes";

    protected GetVolumeRequest(Builder builder) throws InvalidParameterException {
        super();
    }

    @Override
    public String getCommand() {
        return VOLUMES_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static class Builder {

        public GetVolumeRequest build() throws InvalidParameterException {
            return new GetVolumeRequest(this);
        }

    }

}
