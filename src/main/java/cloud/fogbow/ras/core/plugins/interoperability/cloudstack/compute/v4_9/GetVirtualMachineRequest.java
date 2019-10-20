package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.Compute.ID_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.Compute.LIST_VIRTUAL_MACHINES_COMMAND;

public class GetVirtualMachineRequest extends CloudStackRequest {

    private GetVirtualMachineRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(ID_KEY_JSON, builder.id);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return LIST_VIRTUAL_MACHINES_COMMAND;
    }

    public static class Builder {

        private String cloudStackUrl;
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public GetVirtualMachineRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new GetVirtualMachineRequest(this);
        }

    }
}
