package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;


import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import java.security.InvalidParameterException;

public class GetZonesRequest extends CloudStackRequest {
    public static final String LIST_ZONES_COMMAND = "listZones";

    protected GetZonesRequest(Builder builder) throws InvalidParameterException {
    }

    @Override
    public String getCommand() {
        return LIST_ZONES_COMMAND;
    }

    public static class Builder {

        public GetZonesRequest build() throws InvalidParameterException {
            return new GetZonesRequest(this);
        }

    }
}
