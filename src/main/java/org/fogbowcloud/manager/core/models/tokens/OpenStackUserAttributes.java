package org.fogbowcloud.manager.core.models.tokens;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;

public class OpenStackUserAttributes extends LocalUserAttributes {

    private String tenantId;

    public OpenStackUserAttributes(String tokenValue, String tenantId) throws InvalidParameterException {
        super(tokenValue);

        if (tenantId != null) {
            this.tenantId = tenantId;
        } else {
            throw new InvalidParameterException("tenantId not defined");
        }

    }

    public String getTenantId() {
        return tenantId;
    }

}
