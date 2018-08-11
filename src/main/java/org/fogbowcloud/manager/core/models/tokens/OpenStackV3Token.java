package org.fogbowcloud.manager.core.models.tokens;

public class OpenStackV3Token extends FederationUserToken {

    private String projectId;
    private String projectName;

    public OpenStackV3Token(String tokenValue, String userId, String name, String projectId, String projectName) {
        super(tokenValue, userId, name);
        this.projectId = projectId;
        this.projectName = projectName;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public String getProjectName() { return this.projectName; }
}
