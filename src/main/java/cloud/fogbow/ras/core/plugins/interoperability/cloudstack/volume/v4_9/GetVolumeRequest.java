package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

public class GetVolumeRequest extends CloudStackRequest {
    public static final String LIST_VOLUMES_COMMAND = "listVolumes";
    public static final String VOLUME_ID_KEY = "id";
    public static final String VIRTUAL_MACHINE_ID_KEY = "virtualmachineid";
    public static final String TYPE_KEY = "type";

    protected GetVolumeRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
        addParameter(VOLUME_ID_KEY, builder.id);
        addParameter(VIRTUAL_MACHINE_ID_KEY, builder.virtualMachineId);
        addParameter(TYPE_KEY, builder.type);
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
        private String cloudStackUrl;
        private String id;
        private String virtualMachineId;
        private String type;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder virtualMachineId(String virtualMachineId) {
            this.virtualMachineId = virtualMachineId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public GetVolumeRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new GetVolumeRequest(this);
        }
    }
}
