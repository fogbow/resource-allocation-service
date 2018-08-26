package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DestroyVirtualMachineRequest extends CloudStackRequest {
    protected static final String DESTROY_VIRTUAL_MACHINE_COMMAND = "destroyVirtualMachine";
    private static final String VIRTUAL_MACHINE_ID_KEY = "id";
    private static final String EXPUNGE_KEY = "expunge";

    protected DestroyVirtualMachineRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(VIRTUAL_MACHINE_ID_KEY, builder.id);
        addParameter(EXPUNGE_KEY, builder.expunge);
    }

    @Override
    public String getCommand() {
        return DESTROY_VIRTUAL_MACHINE_COMMAND;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static class Builder {

        private String id;
        private String expunge;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder expunge(String expunge) {
            this.expunge = expunge;
            return this;
        }

        public DestroyVirtualMachineRequest build() throws InvalidParameterException {
            return new DestroyVirtualMachineRequest(this);
        }
    }
}
