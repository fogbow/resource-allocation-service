package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class ListResourceLimitsRequest extends CloudStackRequest {

    public static final String LIST_RESOURCE_LIMITS_COMMAND = "listResourceLimits";

    protected ListResourceLimitsRequest() throws InvalidParameterException {
    }

    @Override
    public String getCommand() {
        return LIST_RESOURCE_LIMITS_COMMAND;
    }

}
