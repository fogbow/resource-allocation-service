package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

import java.util.List;

public class DeployComputeRequest extends CloudStackRequest {

    public static final String DEPLOY_VM_COMMAND = "deployVirtualMachine";
    public static final String SERVICE_OFFERING_ID_KEY = "serviceofferingid";
    public static final String TEMPLATE_ID_KEY = "templateid";
    public static final String ZONE_ID_KEY = "zoneid";
    public static final String DISK_SIZE = "size";
    public static final String USER_DATA = "userdata";
    public static final String NETWORKS_ID = "networksid";

    private DeployComputeRequest(Builder builder) throws InvalidParameterException {
        addParameter(SERVICE_OFFERING_ID_KEY, builder.serviceOfferingId);
        addParameter(TEMPLATE_ID_KEY, builder.templateId);
        addParameter(ZONE_ID_KEY, builder.zoneId);
        addParameter(DISK_SIZE, builder.diskSize);
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
        private String diskSize;
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

        public Builder diskSize(String diskSize) {
            this.diskSize = diskSize;
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

        public DeployComputeRequest build() throws InvalidParameterException {
            return new DeployComputeRequest(this);
        }
    }
}
