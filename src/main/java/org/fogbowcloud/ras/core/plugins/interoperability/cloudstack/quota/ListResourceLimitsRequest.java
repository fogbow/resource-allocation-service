package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class ListResourceLimitsRequest extends CloudStackRequest {

    public static final String LIST_RESOURCE_LIMITS_COMMAND = "listResourceLimits";
    public static final String DOMAIN_ID_KEY = "domainid";
    public static final String RESOURCE_TYPE_KEY = "resourcetype";

    private ListResourceLimitsRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(DOMAIN_ID_KEY, builder.domainId);
        addParameter(RESOURCE_TYPE_KEY, builder.resourceType);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return LIST_RESOURCE_LIMITS_COMMAND;
    }

    public static class Builder {
        private String domainId;
        private String resourceType;

        public Builder domainId(String domainId) {
            this.domainId = domainId;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public ListResourceLimitsRequest build() throws InvalidParameterException {
            return new ListResourceLimitsRequest(this);
        }

    }

}
