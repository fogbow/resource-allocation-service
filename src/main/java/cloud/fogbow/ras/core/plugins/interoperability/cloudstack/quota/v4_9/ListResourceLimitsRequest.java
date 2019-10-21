package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;
import com.google.common.annotations.VisibleForTesting;

import static cloud.fogbow.common.constants.CloudStackConstants.Quota.*;

public class ListResourceLimitsRequest extends CloudStackRequest {

    private ListResourceLimitsRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(DOMAIN_ID_KEY_JSON, builder.domainId);
        addParameter(RESOURCE_TYPE_KEY_JSON, builder.resourceType);
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
        private String cloudStackUrl;
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

        public ListResourceLimitsRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new ListResourceLimitsRequest(this);
        }

    }

}
