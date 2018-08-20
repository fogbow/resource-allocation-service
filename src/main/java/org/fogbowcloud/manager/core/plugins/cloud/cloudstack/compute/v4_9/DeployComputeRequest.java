package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class DeployComputeRequest extends CloudStackRequest {

    public static final String DEPLOY_VM_COMMAND = "deployVirtualMachine";
    public static final String SERVICE_OFFERING_ID_KEY = "serviceofferingid";
    public static final String TEMPLATE_ID_KEY = "templateid";
    public static final String ZONE_ID_KEY = "zoneid";

    private DeployComputeRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(SERVICE_OFFERING_ID_KEY, builder.serviceOfferingId);
        addParameter(TEMPLATE_ID_KEY, builder.templateId);
        addParameter(ZONE_ID_KEY, builder.zoneId);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return DEPLOY_VM_COMMAND;
    }

    public static class Builder {

        private String serviceOfferingId;
        private String templateId;
        private String zoneId;

        public Builder serviceOfferingId(String serviceOfferingId) {
            this.serviceOfferingId = serviceOfferingId;
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder zoneId(String zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public DeployComputeRequest build() throws InvalidParameterException {
            return new DeployComputeRequest(this);
        }
    }
}
