package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class GetVirtualMachineRequest extends CloudStackRequest {
    public static final String LIST_VMS_COMMAND = "listVirtualMachines";
    public static final String VIRTUAL_MACHINE_ID_KEY = "id";

    private GetVirtualMachineRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(VIRTUAL_MACHINE_ID_KEY, builder.id);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return LIST_VMS_COMMAND;
    }

    public static class Builder {

        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public GetVirtualMachineRequest build() throws InvalidParameterException {
            return new GetVirtualMachineRequest(this);
        }

    }
}
