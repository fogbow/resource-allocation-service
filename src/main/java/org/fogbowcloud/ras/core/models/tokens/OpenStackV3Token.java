package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class OpenStackV3Token extends FederationUserToken {
    @Column
    private String projectId;
    @Column
    private String projectName;

    public OpenStackV3Token() {
    }

    public OpenStackV3Token(String tokenProvider, String tokenValue, String userId, String name, String projectId,
                            String projectName) {
        super(tokenProvider, tokenValue, userId, name);
        this.projectId = projectId;
        this.projectName = projectName;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public String getProjectName() {
        return this.projectName;
    }

    // Used in testing
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
