package org.fogbowcloud.manager.core.models.tokens;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;

public class OpenStackV3Token extends FederationUserToken {

    private String projectId;
    private String projectName;

    public OpenStackV3Token(String tokenValue, String userId, String name, String projectId, String projectName)
            throws InvalidParameterException {
        super(tokenValue, userId, name);

        if (projectId != null) {
            this.projectId = projectId;
        } else {
            throw new InvalidParameterException("projectId not defined");
        }
        this.projectName = projectName;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public String getName() {
        return this.projectName;
    }
}
