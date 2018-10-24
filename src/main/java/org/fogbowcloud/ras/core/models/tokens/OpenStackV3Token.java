package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class OpenStackV3Token extends SignedFederationUserToken {
	
    @Column
    private String projectId;

    public OpenStackV3Token(String tokenProvider, String tokenValue, String userId, String name, 
    						String projectId, String signature) {
        super(tokenProvider, tokenValue, userId, name, signature);
        this.projectId = projectId;
    }

    public String getProjectId() {
        return this.projectId;
    }

    // Used in testing
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
