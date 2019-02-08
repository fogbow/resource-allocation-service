package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DeleteVolumeRequest extends CloudStackRequest {
    protected static final String DELETE_VOLUME_COMMAND = "deleteVolume";
    private static final String VOLUME_ID_KEY = "id";

    protected DeleteVolumeRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
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
        private String cloudStackUrl;
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public DeleteVolumeRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new DeleteVolumeRequest(this);
        }
    }
}
