package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DeployVirtualMachineRequest extends CloudStackRequest {
    public static final String DEPLOY_VM_COMMAND = "deployVirtualMachine";
    public static final String SERVICE_OFFERING_ID_KEY = "serviceofferingid";
    public static final String TEMPLATE_ID_KEY = "templateid";
    public static final String ZONE_ID_KEY = "zoneid";
    public static final String NAME_KEY = "name";
    public static final String DISK_OFFERING_ID = "diskofferingid";
    public static final String USER_DATA = "userdata";
    public static final String NETWORKS_ID = "networkids";

    private DeployVirtualMachineRequest(Builder builder) throws InvalidParameterException {
        addParameter(SERVICE_OFFERING_ID_KEY, builder.serviceOfferingId);
        addParameter(TEMPLATE_ID_KEY, builder.templateId);
        addParameter(ZONE_ID_KEY, builder.zoneId);
        addParameter(NAME_KEY, builder.name);
        addParameter(DISK_OFFERING_ID, builder.diskOfferingId);
        addParameter(USER_DATA, builder.userData);
        addParameter(NETWORKS_ID, builder.networksId);
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
        private String name;
        private String diskOfferingId;
        private String userData;
        private String networksId;

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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder diskOfferingId(String diskOfferingId) {
            this.diskOfferingId = diskOfferingId;
            return this;
        }

        public Builder userData(String userData) {
            this.userData = userData;
            return this;
        }

        public Builder networksId(String networksId) {
            this.networksId = networksId;
            return this;
        }

        public DeployVirtualMachineRequest build() throws InvalidParameterException {
            return new DeployVirtualMachineRequest(this);
        }
    }
}
